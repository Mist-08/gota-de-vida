package mx.gotadevida.autenticacion.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.gotadevida.core.seguridad.Rol

/**
 * Cuenta de acceso. Un DONADOR tiene además un documento en `donadores` con el mismo id;
 * un HOSPITAL tiene `hospitalId`.
 */
@Serializable
data class Usuario(
    @SerialName("_id") val id: String,
    val email: String,
    val nombre: String,
    val rol: Rol,
    val creadoEn: String,
    val passwordHash: String? = null,
    val googleId: String? = null,
    val hospitalId: String? = null,
    val tokensFcm: List<String> = emptyList(),
)

/** Token de Firebase Cloud Messaging del teléfono, para recibir avisos. */
@Serializable
data class DispositivoPeticion(val tokenFcm: String)

/** Lo que la app puede ver de un usuario (sin hash ni ids externos). */
@Serializable
data class UsuarioPublico(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val hospitalId: String? = null,
)

fun Usuario.aPublico() = UsuarioPublico(id = id, nombre = nombre, email = email, rol = rol, hospitalId = hospitalId)
