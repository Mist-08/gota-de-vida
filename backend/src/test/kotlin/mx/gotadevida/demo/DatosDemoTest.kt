package mx.gotadevida.demo

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.chatbot.entity.MensajePeticion
import mx.gotadevida.core.demo.DatosDemo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.hospital.service.HospitalesDemo
import mx.gotadevida.solicitud.SolicitudRoutesTest
import mx.gotadevida.solicitud.entity.SolicitudRespuesta
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.probarApi

class DatosDemoTest {
    @Test
    fun `el seed crea hospitales, cuenta de banco y donadores, y se puede repetir`() = probarApi { cliente, entorno ->
        val deps = entorno.dependencias
        HospitalesDemo.cargarSiFaltan(deps.hospitales)
        repeat(2) { DatosDemo.cargar("demo-segura-123", deps.autenticacion, deps.donadores, deps.hospitales, LocalDate.of(2026, 11, 12)) }
        assertEquals(12, entorno.donadores.datos.size)
        assertEquals(3, entorno.usuarios.datos.values.count { it.hospitalId != null })

        // El banco de sangre de demo publica una urgencia O− y se avisa a donadores de demo compatibles.
        val banco = cliente.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginSolicitud("banco1@demo.gotadevida.mx", "demo-segura-123"))
        }.body<SesionRespuesta>().tokens.tokenAcceso
        val urgencia = with(SolicitudRoutesTest()) { cliente.publicar(banco, TipoSangre.O_NEGATIVO, 2) }.body<SolicitudRespuesta>()
        assertTrue(entorno.solicitudes.datos.getValue(urgencia.id).notificados.isNotEmpty())
    }

    @Test
    fun `el donador puede borrar su cuenta y sus datos`() = probarApi { cliente, entorno ->
        val (id, token) = with(SolicitudRoutesTest()) { cliente.donador("borrar@correo.mx", TipoSangre.A_POSITIVO, 1.0) }
        cliente.post("/api/v1/chat/mensajes") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(MensajePeticion("2"))
        }
        assertEquals(HttpStatusCode.NoContent, cliente.delete("/api/v1/usuarios/me") { conToken(token) }.status)
        assertTrue(id !in entorno.usuarios.datos)
        assertTrue(id !in entorno.donadores.datos)
        assertTrue(id !in entorno.conversaciones.datos)
        assertTrue(cliente.get("/api/v1/donadores/me") { conToken(token) }.status != HttpStatusCode.OK)
    }
}
