package mx.gotadevida.hospital.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.gotadevida.core.dominio.PuntoGeo

/** Hospital o banco de sangre que publica urgencias. */
@Serializable
data class Hospital(
    @SerialName("_id") val id: String,
    val nombre: String,
    val direccion: String,
    val municipio: String,
    val ubicacion: PuntoGeo,
    val creadoEn: String,
    val telefono: String? = null,
    val esDemostracion: Boolean = false
)

@Serializable
data class CrearHospitalSolicitud(
    val nombre: String,
    val direccion: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double,
    val telefono: String? = null
)

@Serializable
data class HospitalRespuesta(
    val id: String,
    val nombre: String,
    val direccion: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double,
    val telefono: String? = null,
    val esDemostracion: Boolean
)

fun Hospital.aRespuesta() = HospitalRespuesta(
    id = id,
    nombre = nombre,
    direccion = direccion,
    municipio = municipio,
    latitud = ubicacion.latitud,
    longitud = ubicacion.longitud,
    telefono = telefono,
    esDemostracion = esDemostracion
)
