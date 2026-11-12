package mx.gotadevida.donacion.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Donación confirmada en el hospital. Su `_id` es el de la cita: así no se duplica. */
@Serializable
data class Donacion(
    @SerialName("_id") val id: String,
    val donadorId: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val fecha: String,
    val creadaEn: String,
    val solicitudId: String? = null,
)

@Serializable
enum class Insignia { PRIMERA_DONACION, TRES_DONACIONES, CINCO_DONACIONES, HEROE_DE_URGENCIA }

@Serializable
data class InsigniaRespuesta(val codigo: Insignia, val obtenida: Boolean)

@Serializable
data class DonacionRespuesta(
    val id: String,
    val hospitalNombre: String,
    val fecha: String,
    val porUrgencia: Boolean,
)

@Serializable
data class HistorialRespuesta(
    val donaciones: List<DonacionRespuesta>,
    val totalDonaciones: Int,
    /** Cada donación de sangre total puede ayudar hasta a 3 personas. */
    val vidasQuePuedesAyudar: Int,
    val elegible: Boolean,
    val proximaFechaElegible: String? = null,
    val insignias: List<InsigniaRespuesta>,
)
