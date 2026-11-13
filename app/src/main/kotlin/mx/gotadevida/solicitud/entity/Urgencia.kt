package mx.gotadevida.solicitud.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class EstadoUrgencia { ACTIVA, CUBIERTA, CERRADA, CANCELADA }

/** Urgencia como la manda `GET /api/v1/solicitudes` (vista del donador). */
@Serializable
data class Urgencia(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val etiquetaTipoSangre: String,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val estado: EstadoUrgencia,
    val creadaEn: String,
    val nota: String? = null,
    val distanciaKm: Double? = null,
    val miRespuesta: Boolean? = null
)

@Serializable
data class ResponderPeticion(val acepta: Boolean)

/** Lo que se guarda en la cola de salida cuando se responde sin conexión. */
@Serializable
data class RespuestaPendiente(val solicitudId: String, val acepta: Boolean)

/** Copia local (Room). `cercana` = vino en la última lista de urgencias cercanas. */
@Entity(tableName = "urgencias")
data class UrgenciaEntity(
    @PrimaryKey val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val etiquetaTipoSangre: String,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val estado: String,
    val creadaEn: String,
    val nota: String?,
    val distanciaKm: Double?,
    val miRespuesta: Boolean?,
    val cercana: Boolean
)

fun Urgencia.aEntidad(cercana: Boolean) = UrgenciaEntity(
    id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, etiquetaTipoSangre, unidades,
    unidadesComprometidas, estado.name, creadaEn, nota, distanciaKm, miRespuesta, cercana
)

fun UrgenciaEntity.aModelo() = Urgencia(
    id, hospitalId, hospitalNombre, hospitalDireccion, latitud, longitud, etiquetaTipoSangre, unidades,
    unidadesComprometidas, EstadoUrgencia.valueOf(estado), creadaEn, nota, distanciaKm, miRespuesta
)
