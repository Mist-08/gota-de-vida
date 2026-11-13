package mx.gotadevida.reglas

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import mx.gotadevida.reglas.entity.ActualizarReglasSolicitud
import mx.gotadevida.reglas.entity.ReglasElegibilidad
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.iniciarComoAdmin
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar

class ReglasRoutesTest {

    @Test
    fun `las reglas vigentes son publicas`() = probarApi { cliente, _ ->
        val reglas = cliente.get("/api/v1/reglas-elegibilidad").body<ReglasElegibilidad>()

        assertEquals(60, reglas.diasEntreDonaciones)
        assertEquals(18, reglas.edadMinima)
    }

    @Test
    fun `el administrador cambia una regla sin tocar el codigo y las demas se quedan igual`() = probarApi { cliente, _ ->
        val admin = cliente.iniciarComoAdmin()

        val respuesta = cliente.put("/api/v1/reglas-elegibilidad") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(ActualizarReglasSolicitud(diasEntreDonaciones = 90))
        }
        val reglas = cliente.get("/api/v1/reglas-elegibilidad").body<ReglasElegibilidad>()

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(90, reglas.diasEntreDonaciones)
        assertEquals(18, reglas.edadMinima)
    }

    @Test
    fun `un donador no puede cambiar las reglas`() = probarApi { cliente, _ ->
        val token = cliente.registrar().tokens.tokenAcceso

        val respuesta = cliente.put("/api/v1/reglas-elegibilidad") {
            conToken(token)
            contentType(ContentType.Application.Json)
            setBody(ActualizarReglasSolicitud(diasEntreDonaciones = 10))
        }

        assertEquals(HttpStatusCode.Forbidden, respuesta.status)
    }

    @Test
    fun `una regla fuera de rango se rechaza`() = probarApi { cliente, _ ->
        val admin = cliente.iniciarComoAdmin()

        val respuesta = cliente.put("/api/v1/reglas-elegibilidad") {
            conToken(admin)
            contentType(ContentType.Application.Json)
            setBody(ActualizarReglasSolicitud(diasEntreDonaciones = 5))
        }

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
    }
}
