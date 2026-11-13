package mx.gotadevida.donador

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.PerfilDonadorRespuesta
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.iniciarComoAdmin
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar

class DonadorRoutesTest {

    @Test
    fun `sin token no se puede ver el perfil`() = probarApi { cliente, _ ->
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/api/v1/donadores/me").status)
    }

    @Test
    fun `un perfil recien creado esta incompleto y no es elegible`() = probarApi { cliente, _ ->
        val token = cliente.registrar().tokens.tokenAcceso

        val perfil = cliente.get("/api/v1/donadores/me") { conToken(token) }.body<PerfilDonadorRespuesta>()
        val elegibilidad = cliente.get("/api/v1/donadores/me/elegibilidad") { conToken(token) }.body<Elegibilidad>()

        assertFalse(perfil.perfilCompleto)
        assertFalse(elegibilidad.elegible)
        assertTrue(elegibilidad.faltanDatos)
    }

    @Test
    fun `al completar el perfil queda elegible y la ubicacion se guarda aproximada`() = probarApi { cliente, entorno ->
        val sesion = cliente.registrar()
        val token = sesion.tokens.tokenAcceso

        val respuesta = cliente.put("/api/v1/donadores/me") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(
                ActualizarPerfilSolicitud(
                    tipoSangre = TipoSangre.O_NEGATIVO,
                    fechaNacimiento = "2001-03-14",
                    pesoKg = 68.0,
                    sexoParaCriteriosMedicos = SexoParaCriteriosMedicos.FEMENINO,
                    latitud = 25.541234,
                    longitud = -103.406789,
                    zona = "Torreón Jardín, Torreón",
                    fechaUltimaDonacion = "2026-01-12"
                )
            )
        }
        val perfil = respuesta.body<PerfilDonadorRespuesta>()
        val elegibilidad = cliente.get("/api/v1/donadores/me/elegibilidad") { conToken(token) }.body<Elegibilidad>()

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertTrue(perfil.perfilCompleto)
        assertEquals("O−", perfil.etiquetaTipoSangre)
        assertEquals(25.541, perfil.latitud)
        assertEquals(-103.407, perfil.longitud)
        assertEquals(listOf("2026-01-12"), entorno.donadores.datos.getValue(sesion.usuario.id).fechasDonacion)
        assertTrue(elegibilidad.elegible, elegibilidad.motivos.joinToString())
    }

    @Test
    fun `una fecha de nacimiento en el futuro se rechaza`() = probarApi { cliente, _ ->
        val token = cliente.registrar().tokens.tokenAcceso

        val respuesta = cliente.put("/api/v1/donadores/me") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(ActualizarPerfilSolicitud(fechaNacimiento = "2099-01-01"))
        }

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
    }

    @Test
    fun `el administrador no tiene perfil de donador`() = probarApi { cliente, _ ->
        val token = cliente.iniciarComoAdmin()

        assertEquals(HttpStatusCode.Forbidden, cliente.get("/api/v1/donadores/me") { conToken(token) }.status)
    }
}
