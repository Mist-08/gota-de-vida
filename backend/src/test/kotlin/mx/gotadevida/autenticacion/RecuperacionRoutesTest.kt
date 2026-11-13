package mx.gotadevida.autenticacion

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mx.gotadevida.autenticacion.entity.GoogleSolicitud
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RecuperarSolicitud
import mx.gotadevida.autenticacion.entity.RestablecerSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.soporte.EntornoDePrueba
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar

class RecuperacionRoutesTest {

    private suspend fun HttpClient.recuperar(email: String) = post("/api/v1/auth/recuperar") {
        contentType(ContentType.Application.Json)
        setBody(RecuperarSolicitud(email))
    }

    private suspend fun HttpClient.restablecer(email: String, codigo: String, password: String = "nueva-contrasena"): HttpResponse =
        post("/api/v1/auth/restablecer") {
            contentType(ContentType.Application.Json)
            setBody(RestablecerSolicitud(email, codigo, password))
        }

    private suspend fun HttpClient.login(email: String, password: String) = post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginSolicitud(email, password))
    }

    /** El código viene en el asunto del último correo enviado. */
    private fun EntornoDePrueba.ultimoCodigo(): String = correo.enviados.last().asunto.takeLast(6)

    @Test
    fun `con el codigo del correo se cambia la contrasena y queda la sesion iniciada`() = probarApi { cliente, entorno ->
        cliente.registrar()

        assertEquals(HttpStatusCode.NoContent, cliente.recuperar("Mariana@Correo.mx ").status)
        val correo = entorno.correo.enviados.single()
        assertEquals("mariana@correo.mx", correo.para)
        val codigo = entorno.ultimoCodigo()
        assertTrue(codigo.all { it.isDigit() } && codigo.length == 6)
        assertTrue(codigo in correo.texto)

        val respuesta = cliente.restablecer("mariana@correo.mx", codigo)
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertTrue(respuesta.body<SesionRespuesta>().tokens.tokenAcceso.isNotBlank())

        assertEquals(HttpStatusCode.OK, cliente.login("mariana@correo.mx", "nueva-contrasena").status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.login("mariana@correo.mx", "contrasena-segura").status)
        // El código ya no sirve una segunda vez.
        assertEquals(HttpStatusCode.BadRequest, cliente.restablecer("mariana@correo.mx", codigo, "otra-mas-segura").status)
    }

    @Test
    fun `un correo sin cuenta responde igual y no manda nada`() = probarApi { cliente, entorno ->
        assertEquals(HttpStatusCode.NoContent, cliente.recuperar("nadie@correo.mx").status)
        assertTrue(entorno.correo.enviados.isEmpty())
    }

    @Test
    fun `un codigo equivocado no cambia la contrasena y a los 5 intentos se invalida`() = probarApi { cliente, entorno ->
        val sesion = cliente.registrar()
        cliente.recuperar("mariana@correo.mx")
        val codigo = entorno.ultimoCodigo()
        val incorrecto = if (codigo == "000000") "111111" else "000000"

        repeat(5) { assertEquals(HttpStatusCode.BadRequest, cliente.restablecer("mariana@correo.mx", incorrecto).status) }

        assertNull(entorno.usuarios.datos.getValue(sesion.usuario.id).recuperacion)
        assertEquals(HttpStatusCode.BadRequest, cliente.restablecer("mariana@correo.mx", codigo).status)
        assertEquals(HttpStatusCode.OK, cliente.login("mariana@correo.mx", "contrasena-segura").status)
    }

    @Test
    fun `un codigo vencido no sirve`() = probarApi { cliente, entorno ->
        val sesion = cliente.registrar()
        cliente.recuperar("mariana@correo.mx")
        val codigo = entorno.ultimoCodigo()
        val usuario = entorno.usuarios.datos.getValue(sesion.usuario.id)
        entorno.usuarios.datos[usuario.id] =
            usuario.copy(recuperacion = usuario.recuperacion!!.copy(vence = Instant.now().minusSeconds(60).toString()))

        assertEquals(HttpStatusCode.BadRequest, cliente.restablecer("mariana@correo.mx", codigo).status)
        assertNull(entorno.usuarios.datos.getValue(sesion.usuario.id).recuperacion)
    }

    @Test
    fun `la nueva contrasena tambien debe tener 8 caracteres`() = probarApi { cliente, entorno ->
        cliente.registrar()
        cliente.recuperar("mariana@correo.mx")

        assertEquals(HttpStatusCode.BadRequest, cliente.restablecer("mariana@correo.mx", entorno.ultimoCodigo(), "corta").status)
    }

    @Test
    fun `una cuenta de Google recibe un aviso en vez de un codigo`() = probarApi { cliente, entorno ->
        cliente.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleSolicitud("token-valido"))
        }

        cliente.recuperar("laura@gmail.com")

        val correo = assertNotNull(entorno.correo.enviados.singleOrNull())
        assertTrue("Google" in correo.asunto)
        assertNull(entorno.usuarios.datos.values.single { it.email == "laura@gmail.com" }.recuperacion)
    }
}
