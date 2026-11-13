package mx.gotadevida.core.seguridad

import kotlinx.serialization.Serializable

/** Roles de la API. Cada ruta exige uno o varios. */
@Serializable
enum class Rol {
    DONADOR,
    HOSPITAL,
    ADMIN
}
