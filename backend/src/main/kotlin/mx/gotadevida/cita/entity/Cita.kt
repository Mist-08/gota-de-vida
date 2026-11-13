package mx.gotadevida.cita.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EstadoCita { AGENDADA, CANCELADA, COMPLETADA }

/**
 * Cita para donar. `fechaHora` es hora local de La Laguna (`2026-11-13T09:30`).
 * `codigoQr` es un código aleatorio que el hospital escanea para confirmar la donación.
 */
@Serializable
data class Cita(
    @SerialName("_id") val id: String,
    val donadorId: String,
    val donadorNombre: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String,
    val estado: EstadoCita,
    val codigoQr: String,
    val creadaEn: String,
    val solicitudId: String? = null,
    val completadaEn: String? = null
)

@Serializable
data class Horario(val hora: String, val lugares: Int, val disponible: Boolean)

@Serializable
data class AgendarCitaPeticion(
    val hospitalId: String,
    /** Hora local: `2026-11-13T09:30`. */
    val fechaHora: String,
    val solicitudId: String? = null
)

@Serializable
data class CheckinPeticion(val codigoQr: String)

@Serializable
data class CitaRespuesta(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String,
    val estado: EstadoCita,
    val solicitudId: String? = null,
    /** Solo para el donador dueño de la cita. */
    val codigoQr: String? = null,
    /** Solo para el hospital. */
    val donadorNombre: String? = null
)

fun Cita.paraDonador() = CitaRespuesta(
    id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, fechaHora, estado, solicitudId, codigoQr = codigoQr
)

fun Cita.paraHospital() = CitaRespuesta(
    id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, fechaHora, estado, solicitudId, donadorNombre = donadorNombre
)
