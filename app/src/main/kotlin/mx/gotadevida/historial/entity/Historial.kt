package mx.gotadevida.historial.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class Insignia { PRIMERA_DONACION, TRES_DONACIONES, CINCO_DONACIONES, HEROE_DE_URGENCIA }

@Serializable
data class InsigniaGanada(val codigo: Insignia, val obtenida: Boolean)

@Serializable
data class DonacionHecha(val id: String, val hospitalNombre: String, val fecha: String, val porUrgencia: Boolean)

/** `GET /api/v1/donaciones/me`. */
@Serializable
data class Historial(
    val donaciones: List<DonacionHecha>,
    val totalDonaciones: Int,
    val vidasQuePuedesAyudar: Int,
    val elegible: Boolean,
    val proximaFechaElegible: String? = null,
    val insignias: List<InsigniaGanada>
)

/** Copia local del historial (un solo renglón con el JSON) para verlo sin conexión. */
@Entity(tableName = "historial")
data class HistorialEntity(@PrimaryKey val usuarioId: String, val json: String)
