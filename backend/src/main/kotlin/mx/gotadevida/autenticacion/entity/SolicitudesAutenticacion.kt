package mx.gotadevida.autenticacion.entity

import kotlinx.serialization.Serializable
import mx.gotadevida.core.seguridad.Tokens

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
data class RefrescoSolicitud(val tokenRefresco: String)

/** Alta de una cuenta del banco de sangre (solo ADMIN). */
@Serializable
data class UsuarioHospitalSolicitud(val nombre: String, val email: String, val password: String)

@Serializable
data class SesionRespuesta(val usuario: UsuarioPublico, val tokens: Tokens)
