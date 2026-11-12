package mx.gotadevida.cita.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class EstadoCita { AGENDADA, CANCELADA, COMPLETADA }

@Serializable
data class Horario(val hora: String, val lugares: Int, val disponible: Boolean)

@Serializable
data class AgendarPeticion(val hospitalId: String, val fechaHora: String, val solicitudId: String? = null)

/** Cita como la manda la API. `fechaHora` es hora local: `2026-11-13T09:30`. */
@Serializable
data class Cita(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String,
    val estado: EstadoCita,
    val solicitudId: String? = null,
    val codigoQr: String? = null,
) {
    /** Lo que contiene el QR que escanea el hospital. */
    val contenidoQr: String? get() = codigoQr?.let { "gota:$id:$it" }
}

/** Requisitos vigentes (de `GET /api/v1/reglas-elegibilidad`) para mostrarlos antes de la cita. */
@Serializable
data class Requisitos(
    val edadMinima: Int,
    val edadMaxima: Int,
    val pesoMinimoKg: Double,
    val horasAyunoMinimo: Int,
    val horasAyunoMaximo: Int,
)

/** Copia local de la cita: el QR se puede mostrar sin señal en el hospital. */
@Entity(tableName = "citas")
data class CitaEntity(
    @PrimaryKey val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String,
    val estado: String,
    val solicitudId: String?,
    val codigoQr: String?,
)

fun Cita.aEntidad() = CitaEntity(id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, fechaHora, estado.name, solicitudId, codigoQr)

fun CitaEntity.aModelo() = Cita(id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, fechaHora, EstadoCita.valueOf(estado), solicitudId, codigoQr)
