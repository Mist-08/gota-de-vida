package mx.gotadevida.solicitud

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.gotadevida.autenticacion.entity.DispositivoPeticion
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.autenticacion.entity.UsuarioHospitalSolicitud
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.hospital.entity.CrearHospitalSolicitud
import mx.gotadevida.hospital.entity.HospitalRespuesta
import mx.gotadevida.solicitud.entity.CambiarEstadoPeticion
import mx.gotadevida.solicitud.entity.CrearSolicitudPeticion
import mx.gotadevida.solicitud.entity.EstadoSolicitud
import mx.gotadevida.solicitud.entity.ResponderPeticion
import mx.gotadevida.solicitud.entity.SolicitudRespuesta
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.iniciarComoAdmin
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Hospital Universitario de Torreón (coordenadas aproximadas). */
internal const val LAT_HOSPITAL = 25.5428
internal const val LNG_HOSPITAL = -103.4068

class SolicitudRoutesTest {

    internal suspend fun HttpClient.crearHospitalYCuenta(): String {
        val admin = iniciarComoAdmin()
        val hospital = post("/api/v1/hospitales") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(CrearHospitalSolicitud("Hospital Universitario", "Av. Morelos 900", "Torreón", LAT_HOSPITAL, LNG_HOSPITAL))
        }.body<HospitalRespuesta>()
        post("/api/v1/hospitales/${hospital.id}/usuarios") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(UsuarioHospitalSolicitud("Banco HU", "banco@hu.mx", "banco-seguro-123"))
        }
        val token = post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginSolicitud("banco@hu.mx", "banco-seguro-123"))
        }.body<SesionRespuesta>().tokens.tokenAcceso
        put("/api/v1/usuarios/me/dispositivo") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(DispositivoPeticion("token-hospital-0000000000"))
        }
        return token
    }

    /** Registra un donador elegible con su tipo de sangre a `kmAlNorte` del hospital. */
    internal suspend fun HttpClient.donador(email: String, tipo: TipoSangre, kmAlNorte: Double, token: String? = null): Pair<String, String> {
        val sesion = registrar(email = email, nombre = "Donador ${tipo.name}")
        val acceso = sesion.tokens.tokenAcceso
        put("/api/v1/donadores/me") {
            conToken(acceso)
            contentType(ContentType.Application.Json)
            setBody(
                ActualizarPerfilSolicitud(
                    tipoSangre = tipo,
                    fechaNacimiento = "1995-04-10",
                    pesoKg = 70.0,
                    sexoParaCriteriosMedicos = SexoParaCriteriosMedicos.MASCULINO,
                    latitud = LAT_HOSPITAL + kmAlNorte / 111.0,
                    longitud = LNG_HOSPITAL,
                    radioMaximoKm = 60,
                ),
            )
        }
        if (token != null) {
            put("/api/v1/usuarios/me/dispositivo") {
                conToken(acceso)
                contentType(ContentType.Application.Json)
                setBody(DispositivoPeticion(token))
            }
        }
        return sesion.usuario.id to acceso
    }

    internal suspend fun HttpClient.publicar(hospital: String, tipo: TipoSangre, unidades: Int) = post("/api/v1/solicitudes") {
        conToken(hospital)
        contentType(ContentType.Application.Json)
        setBody(CrearSolicitudPeticion(tipo, unidades, "Paciente en quirófano"))
    }

    @Test
    fun `al publicar una urgencia solo se avisa a donadores compatibles y cercanos`() = probarApi { cliente, entorno ->
        val hospital = cliente.crearHospitalYCuenta()
        val (oNeg, _) = cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0, "token-oneg-0000000000000")
        cliente.donador("apos@correo.mx", TipoSangre.A_POSITIVO, 2.0, "token-apos-0000000000000")
        cliente.donador("lejos@correo.mx", TipoSangre.O_NEGATIVO, 40.0, "token-lejos-000000000000")

        val respuesta = cliente.publicar(hospital, TipoSangre.O_NEGATIVO, 2)
        assertEquals(HttpStatusCode.Created, respuesta.status)
        val urgencia = respuesta.body<SolicitudRespuesta>()
        assertEquals("O−", urgencia.etiquetaTipoSangre)

        // Primera oleada (10 km): solo el O− cercano. El A+ no es compatible con un paciente O−.
        val avisados = entorno.push.enviados.filter { it.second["tipo"] == "urgencia" }.flatMap { it.first }
        assertEquals(listOf("token-oneg-0000000000000"), avisados)
        assertEquals(listOf(oNeg), entorno.solicitudes.datos.getValue(urgencia.id).notificados)

        // Si no se cubre, la siguiente oleada amplía el radio y avisa al que está a 40 km... hasta la tercera (50 km).
        entorno.bus.dispararRetrasados()
        entorno.bus.dispararRetrasados()
        val todos = entorno.push.enviados.filter { it.second["tipo"] == "urgencia" }.flatMap { it.first }
        assertEquals(listOf("token-oneg-0000000000000", "token-lejos-000000000000"), todos)
        assertEquals(3, entorno.solicitudes.datos.getValue(urgencia.id).oleada)
    }

    @Test
    fun `el donador ve solo urgencias compatibles y el contador sube hasta cubrirse`() = probarApi { cliente, entorno ->
        val hospital = cliente.crearHospitalYCuenta()
        val (_, oNeg) = cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0)
        val (_, oPos) = cliente.donador("opos@correo.mx", TipoSangre.O_POSITIVO, 4.0)
        val (_, aPos) = cliente.donador("apos@correo.mx", TipoSangre.A_POSITIVO, 2.0)
        val urgencia = cliente.publicar(hospital, TipoSangre.O_POSITIVO, 2).body<SolicitudRespuesta>()

        // Una urgencia O+ no le aparece a un donador A+.
        assertTrue(cliente.get("/api/v1/solicitudes") { conToken(aPos) }.body<List<SolicitudRespuesta>>().isEmpty())
        val vista = cliente.get("/api/v1/solicitudes") { conToken(oNeg) }.body<List<SolicitudRespuesta>>().single()
        assertEquals(3.0, vista.distanciaKm!!, 0.2)
        assertNull(vista.miRespuesta)

        val compatible = cliente.post("/api/v1/solicitudes/${urgencia.id}/respuestas") {
            conToken(aPos)
            contentType(ContentType.Application.Json)
            setBody(ResponderPeticion(true))
        }
        assertEquals(HttpStatusCode.Conflict, compatible.status)

        for ((i, token) in listOf(oNeg, oPos).withIndex()) {
            val r = cliente.post("/api/v1/solicitudes/${urgencia.id}/respuestas") {
                conToken(token)
                contentType(ContentType.Application.Json)
                setBody(ResponderPeticion(true))
            }.body<SolicitudRespuesta>()
            assertEquals(i + 1, r.unidadesComprometidas)
            assertEquals(true, r.miRespuesta)
        }

        val detalle = cliente.get("/api/v1/solicitudes/${urgencia.id}") { conToken(hospital) }.body<SolicitudRespuesta>()
        assertEquals(EstadoSolicitud.CUBIERTA, detalle.estado)
        assertEquals(2, detalle.unidadesComprometidas)
        assertEquals(2, detalle.aceptaciones!!.size)
        assertTrue(detalle.aceptaciones!!.all { it.telefono != null })
        // El hospital recibió un aviso por cada aceptación; el último dice que ya se cubrió.
        assertEquals("urgencia_cubierta", entorno.push.enviados.last().second["tipo"])

        // Ya cubierta: deja de aparecer y no admite más aceptaciones.
        assertTrue(cliente.get("/api/v1/solicitudes") { conToken(oNeg) }.body<List<SolicitudRespuesta>>().isEmpty())
    }

    @Test
    fun `una urgencia cancelada no lanza mas oleadas y otro hospital no puede tocarla`() = probarApi { cliente, entorno ->
        val hospital = cliente.crearHospitalYCuenta()
        cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 30.0, "token-oneg-0000000000000")
        val urgencia = cliente.publicar(hospital, TipoSangre.AB_POSITIVO, 1).body<SolicitudRespuesta>()

        val (_, donador) = cliente.donador("otro@correo.mx", TipoSangre.B_POSITIVO, 1.0)
        val ajeno = cliente.patch("/api/v1/solicitudes/${urgencia.id}/estado") {
            conToken(donador)
            contentType(ContentType.Application.Json)
            setBody(CambiarEstadoPeticion(EstadoSolicitud.CANCELADA))
        }
        assertEquals(HttpStatusCode.Forbidden, ajeno.status)

        val cancelada = cliente.patch("/api/v1/solicitudes/${urgencia.id}/estado") {
            conToken(hospital)
            contentType(ContentType.Application.Json)
            setBody(CambiarEstadoPeticion(EstadoSolicitud.CANCELADA))
        }.body<SolicitudRespuesta>()
        assertEquals(EstadoSolicitud.CANCELADA, cancelada.estado)

        entorno.bus.dispararRetrasados()
        assertEquals(1, entorno.solicitudes.datos.getValue(urgencia.id).oleada)
        assertTrue(entorno.push.enviados.none { it.second["tipo"] == "urgencia" })

        val lista = cliente.get("/api/v1/hospitales/me/solicitudes") { conToken(hospital) }.body<List<SolicitudRespuesta>>()
        assertEquals(listOf(urgencia.id), lista.map { it.id })
    }

    @Test
    fun `un donador no puede publicar urgencias`() = probarApi { cliente, _ ->
        val (_, donador) = cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 1.0)
        assertEquals(HttpStatusCode.Forbidden, cliente.publicar(donador, TipoSangre.O_NEGATIVO, 1).status)
    }
}
