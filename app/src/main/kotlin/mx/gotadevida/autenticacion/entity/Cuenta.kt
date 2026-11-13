package mx.gotadevida.autenticacion.entity

import kotlinx.serialization.Serializable

// Mismos nombres y formas que la API (backend/.../autenticacion/entity).

@Serializable
data class RegistroSolicitud(
    val nombre: String,
    val email: String,
    val password: String,
    val telefono: String,
    val aceptaAvisoPrivacidad: Boolean
)

@Serializable
data class LoginSolicitud(val email: String, val password: String)

@Serializable
data class GoogleSolicitud(val idToken: String)

@Serializable
data class RecuperarSolicitud(val email: String)

@Serializable
data class RestablecerSolicitud(val email: String, val codigo: String, val password: String)

@Serializable
data class Tokens(val tokenAcceso: String, val tokenRefresco: String, val expiraEnSegundos: Long)

@Serializable
data class UsuarioPublico(val id: String, val nombre: String, val email: String, val rol: String, val hospitalId: String? = null)

@Serializable
data class SesionRespuesta(val usuario: UsuarioPublico, val tokens: Tokens)

/** Campos del formulario de cuenta, para asociar cada error con su campo. */
enum class CampoCuenta { NOMBRE, EMAIL, PASSWORD, TELEFONO, CODIGO }

/** Errores de validación. La UI los convierte en texto en el idioma del usuario. */
enum class ErrorCampo { VACIO, NOMBRE_CORTO, EMAIL_INVALIDO, PASSWORD_CORTA, TELEFONO_INVALIDO, CODIGO_INVALIDO }
