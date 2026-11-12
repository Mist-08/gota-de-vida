package mx.gotadevida.autenticacion

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mx.gotadevida.autenticacion.entity.GoogleSolicitud
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RefrescoSolicitud
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.core.errores.ErrorRespuesta
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.soporte.probarApi
import mx.gotadevida.soporte.registrar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutenticacionRoutesTest {

    @Test
    fun `registrar un donador crea su cuenta, su perfil y devuelve tokens`() = probarApi { cliente, entorno ->
        val sesion = cliente.registrar()

        assertEquals(Rol.DONADOR, sesion.usuario.rol)
        assertEquals("mariana@correo.mx", sesion.usuario.email)
        assertTrue(sesion.tokens.tokenAcceso.isNotBlank())
        val perfil = assertNotNull(entorno.donadores.datos[sesion.usuario.id])
        assertEquals("8711234567", perfil.telefono)
        assertTrue(entorno.usuarios.datos.getValue(sesion.usuario.id).passwordHash!!.startsWith("hash:"))
    }

    @Test
    fun `no se puede registrar dos veces el mismo correo`() = probarApi { cliente, _ ->
        cliente.registrar()
        val respuesta = cliente.post("/api/v1/auth/registro") {
            contentType(ContentType.Application.Json)
            setBody(RegistroSolicitud("Otra", "MARIANA@correo.mx", "contrasena-segura", "8711234567", true))
        }

        assertEquals(HttpStatusCode.Conflict, respuesta.status)
    }

    @Test
    fun `sin aceptar el aviso de privacidad no hay registro`() = probarApi { cliente, _ ->
        val respuesta = cliente.post("/api/v1/auth/registro") {
            contentType(ContentType.Application.Json)
            setBody(RegistroSolicitud("Mariana", "mariana@correo.mx", "contrasena-segura", "8711234567", false))
        }

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
        assertEquals("datos_invalidos", respuesta.body<ErrorRespuesta>().error)
    }

    @Test
    fun `un celular sin 10 digitos se rechaza`() = probarApi { cliente, _ ->
        val respuesta = cliente.post("/api/v1/auth/registro") {
            contentType(ContentType.Application.Json)
            setBody(RegistroSolicitud("Mariana", "mariana@correo.mx", "contrasena-segura", "87112", true))
        }

        assertEquals(HttpStatusCode.BadRequest, respuesta.status)
    }

    @Test
    fun `login con la contrasena correcta funciona y con otra no`() = probarApi { cliente, _ ->
        cliente.registrar()

        val bien = cliente.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginSolicitud("mariana@correo.mx", "contrasena-segura"))
        }
        val mal = cliente.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginSolicitud("mariana@correo.mx", "otra-contrasena"))
        }

        assertEquals(HttpStatusCode.OK, bien.status)
        assertEquals(HttpStatusCode.Unauthorized, mal.status)
    }

    @Test
    fun `el token de refresco da una sesion nueva`() = probarApi { cliente, _ ->
        val sesion = cliente.registrar()

        val respuesta = cliente.post("/api/v1/auth/refrescar") {
            contentType(ContentType.Application.Json)
            setBody(RefrescoSolicitud(sesion.tokens.tokenRefresco))
        }

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals(sesion.usuario.id, respuesta.body<SesionRespuesta>().usuario.id)
    }

    @Test
    fun `el token de acceso no sirve como token de refresco`() = probarApi { cliente, _ ->
        val sesion = cliente.registrar()

        val respuesta = cliente.post("/api/v1/auth/refrescar") {
            contentType(ContentType.Application.Json)
            setBody(RefrescoSolicitud(sesion.tokens.tokenAcceso))
        }

        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
    }

    @Test
    fun `con Google se crea la cuenta la primera vez y se reutiliza despues`() = probarApi { cliente, entorno ->
        suspend fun entrar() = cliente.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleSolicitud("token-valido"))
        }.body<SesionRespuesta>()

        val primera = entrar()
        val segunda = entrar()

        assertEquals(primera.usuario.id, segunda.usuario.id)
        assertEquals("laura@gmail.com", primera.usuario.email)
        assertNotNull(entorno.donadores.datos[primera.usuario.id])
    }

    @Test
    fun `un token de Google invalido se rechaza`() = probarApi { cliente, _ ->
        val respuesta = cliente.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleSolicitud("token-falso"))
        }

        assertEquals(HttpStatusCode.Unauthorized, respuesta.status)
    }
}
