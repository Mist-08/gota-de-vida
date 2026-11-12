package mx.gotadevida.chatbot

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.put
import mx.gotadevida.chatbot.entity.HistorialChat
import mx.gotadevida.chatbot.entity.MensajePeticion
import mx.gotadevida.chatbot.entity.RespuestaChat
import mx.gotadevida.chatbot.service.RespuestaGemini
import mx.gotadevida.cita.entity.CitaRespuesta
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.solicitud.SolicitudRoutesTest
import mx.gotadevida.solicitud.entity.SolicitudRespuesta
import mx.gotadevida.soporte.GeminiDePrueba
import mx.gotadevida.soporte.SECRETO_META
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.whatsapp.service.FirmaMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatbotTest {
    private val escenario = SolicitudRoutesTest()

    private suspend fun io.ktor.client.HttpClient.chat(token: String, texto: String) = post("/api/v1/chat/mensajes") {
        conToken(token); contentType(ContentType.Application.Json); setBody(MensajePeticion(texto))
    }.body<RespuestaChat>()

    @Test
    fun `las preguntas frecuentes se contestan sin gastar cuota y con las reglas vigentes`() = probarApi { cliente, entorno ->
        val (_, donador) = with(escenario) { cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0) }
        val r = cliente.chat(donador, "¿Tengo que ir en ayunas?")
        assertEquals("PREGUNTA_FRECUENTE", r.origen)
        assertTrue(r.respuesta.contains("4 a 12 horas"))
        assertEquals(null, entorno.gemini)
    }

    @Test
    fun `sin cuota de Gemini contesta con el menu y el flujo sin IA agenda la cita`() = probarApi { cliente, entorno ->
        val hospital = with(escenario) { cliente.crearHospitalYCuenta() }
        val (_, donador) = with(escenario) { cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0) }
        with(escenario) { cliente.publicar(hospital, TipoSangre.O_NEGATIVO, 2) }.body<SolicitudRespuesta>()

        // Gemini "sin cuota" (el entorno lanza CuotaAgotada cuando no hay Gemini de prueba).
        val menu = cliente.chat(donador, "quiero ayudar con algo")
        assertEquals("MENU", menu.origen)
        assertEquals(listOf("1", "2", "3", "4"), menu.opciones)

        val acepta = cliente.chat(donador, "1")
        assertTrue(acepta.respuesta.startsWith("¡Gracias!"), acepta.respuesta)
        assertEquals("08:00", acepta.opciones.first())

        val cita = cliente.chat(donador, "8:00")
        assertTrue(cita.respuesta.startsWith("✅ Listo"), cita.respuesta)
        val mias = cliente.get("/api/v1/citas/me") { conToken(donador) }.body<List<CitaRespuesta>>()
        assertTrue(mias.single().fechaHora.endsWith("T08:00"))
        assertTrue(entorno.solicitudes.datos.values.single().let { it.unidadesComprometidas == 1 })

        val historial = cliente.get("/api/v1/chat/mensajes") { conToken(donador) }.body<HistorialChat>()
        assertEquals(6, historial.mensajes.size)
    }

    @Test
    fun `con Gemini usa function calling y no le manda datos personales`() = probarApi { cliente, entorno ->
        val hospital = with(escenario) { cliente.crearHospitalYCuenta() }
        val (_, donador) = with(escenario) { cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0) }
        with(escenario) { cliente.publicar(hospital, TipoSangre.O_NEGATIVO, 2) }
        val gemini = GeminiDePrueba(
            RespuestaGemini.Funciones(
                buildJsonArray { addJsonObject { putJsonObject("functionCall") { put("name", "buscar_urgencias_activas") } } },
                listOf("buscar_urgencias_activas" to JsonObject(emptyMap())),
            ),
            RespuestaGemini.Texto("Hay una urgencia O− en el Hospital Universitario, a 3 km. ¿Puedes ir?"),
        )
        entorno.gemini = gemini

        val r = cliente.chat(donador, "hola, ¿hay alguna urgencia donde pueda ayudar?")
        assertEquals("GEMINI", r.origen)
        assertTrue(r.respuesta.contains("Hospital Universitario"))

        // Lo que vio Gemini: el texto y el resultado de la herramienta (con distancia, sin nombre ni teléfono).
        val vistoPorGemini = gemini.recibidos.last().joinToString { it.partes.toString() }
        assertTrue(vistoPorGemini.contains("distanciaKm"))
        assertFalse(vistoPorGemini.contains("Donador O_NEGATIVO"))
        assertFalse(vistoPorGemini.contains("8711234567"))
        assertFalse(vistoPorGemini.contains("latitud"))
    }

    @Test
    fun `webhook de WhatsApp - verificacion, firma y respuesta al boton Si puedo`() = probarApi { cliente, entorno ->
        val hospital = with(escenario) { cliente.crearHospitalYCuenta() }
        with(escenario) { cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0, "token-oneg-0000000000000") }
        with(escenario) { cliente.publicar(hospital, TipoSangre.O_NEGATIVO, 1) }
        // La urgencia también salió por WhatsApp con la plantilla.
        assertTrue(entorno.whatsapp.enviados.any { it.second.startsWith("urgencia_donacion(O−") })

        val reto = cliente.get("/api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=verificacion-de-prueba&hub.challenge=12345")
        assertEquals("12345", reto.bodyAsText())
        val malo = cliente.get("/api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=otro&hub.challenge=1")
        assertEquals(HttpStatusCode.Forbidden, malo.status)

        fun cuerpo(de: String, texto: String) = """
            {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"messages":[
              {"from":"$de","id":"wamid.${texto.hashCode()}","type":"button","button":{"text":"$texto"}}
            ]}}]}]}
        """.trimIndent()

        val sinFirma = cliente.post("/api/v1/webhooks/whatsapp") { setBody(cuerpo("5218711234567", "Sí puedo")) }
        assertEquals(HttpStatusCode.Unauthorized, sinFirma.status)

        val texto = cuerpo("5218711234567", "Sí puedo")
        val ok = cliente.post("/api/v1/webhooks/whatsapp") {
            header("X-Hub-Signature-256", FirmaMeta.firmar(texto, SECRETO_META))
            setBody(texto)
        }
        assertEquals(HttpStatusCode.OK, ok.status)
        val respuesta = entorno.whatsapp.enviados.last()
        assertEquals("8711234567", respuesta.first)
        assertTrue(respuesta.second.startsWith("¡Gracias!"), respuesta.second)
        assertEquals(1, entorno.solicitudes.datos.values.single().unidadesComprometidas)

        // Un número que no está registrado recibe la invitación a la app.
        val desconocido = cuerpo("5218719999999", "hola")
        cliente.post("/api/v1/webhooks/whatsapp") {
            header("X-Hub-Signature-256", FirmaMeta.firmar(desconocido, SECRETO_META))
            setBody(desconocido)
        }
        assertTrue(entorno.whatsapp.enviados.last().second.contains("Regístrate"))
    }

}
