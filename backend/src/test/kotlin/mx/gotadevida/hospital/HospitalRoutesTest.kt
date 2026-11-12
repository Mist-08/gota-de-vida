package mx.gotadevida.hospital

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.autenticacion.entity.UsuarioHospitalSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioPublico
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.hospital.entity.CrearHospitalSolicitud
import mx.gotadevida.hospital.entity.HospitalRespuesta
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.iniciarComoAdmin
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar
import kotlin.test.Test
import kotlin.test.assertEquals

class HospitalRoutesTest {

    private val solicitud = CrearHospitalSolicitud(
        nombre = "Hospital de prueba",
        direccion = "Centro, Torreón",
        municipio = "Torreón",
        latitud = 25.5428,
        longitud = -103.4068,
    )

    @Test
    fun `un donador no puede dar de alta hospitales`() = probarApi { cliente, _ ->
        val token = cliente.registrar().tokens.tokenAcceso

        val respuesta = cliente.post("/api/v1/hospitales") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(solicitud)
        }

        assertEquals(HttpStatusCode.Forbidden, respuesta.status)
    }

    @Test
    fun `el administrador crea un hospital, su cuenta, y esa cuenta inicia sesion como HOSPITAL`() =
        probarApi { cliente, _ ->
            val admin = cliente.iniciarComoAdmin()

            val hospital = cliente.post("/api/v1/hospitales") {
                conToken(admin)
                contentType(ContentType.Application.Json)
                setBody(solicitud)
            }.body<HospitalRespuesta>()

            val cuenta = cliente.post("/api/v1/hospitales/${hospital.id}/usuarios") {
                conToken(admin)
                contentType(ContentType.Application.Json)
                setBody(UsuarioHospitalSolicitud("Banco de sangre HU", "banco@hu.mx", "banco-seguro-123"))
            }
            val sesion = cliente.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginSolicitud("banco@hu.mx", "banco-seguro-123"))
            }.body<SesionRespuesta>()

            assertEquals(HttpStatusCode.Created, cuenta.status)
            assertEquals(hospital.id, cuenta.body<UsuarioPublico>().hospitalId)
            assertEquals(Rol.HOSPITAL, sesion.usuario.rol)
        }

    @Test
    fun `no se crean cuentas para un hospital que no existe`() = probarApi { cliente, _ ->
        val admin = cliente.iniciarComoAdmin()

        val respuesta = cliente.post("/api/v1/hospitales/no-existe/usuarios") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(UsuarioHospitalSolicitud("Banco", "banco@hu.mx", "banco-seguro-123"))
        }

        assertEquals(HttpStatusCode.NotFound, respuesta.status)
    }

    @Test
    fun `cualquier usuario con sesion ve la lista de hospitales`() = probarApi { cliente, _ ->
        val admin = cliente.iniciarComoAdmin()
        cliente.post("/api/v1/hospitales") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(solicitud)
        }
        val token = cliente.registrar().tokens.tokenAcceso

        val lista = cliente.get("/api/v1/hospitales") { conToken(token) }.body<List<HospitalRespuesta>>()

        assertEquals(listOf("Hospital de prueba"), lista.map { it.nombre })
    }
}
