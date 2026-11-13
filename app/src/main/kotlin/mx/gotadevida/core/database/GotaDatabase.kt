package mx.gotadevida.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json
import mx.gotadevida.chatbot.entity.MensajeEntity
import mx.gotadevida.chatbot.repository.ChatDao
import mx.gotadevida.cita.entity.CitaEntity
import mx.gotadevida.cita.repository.CitaDao
import mx.gotadevida.core.sync.AccionPendiente
import mx.gotadevida.core.sync.AccionPendienteDao
import mx.gotadevida.donador.entity.ElegibilidadEntity
import mx.gotadevida.donador.entity.PerfilEntity
import mx.gotadevida.donador.repository.DonadorDao
import mx.gotadevida.historial.entity.HistorialEntity
import mx.gotadevida.historial.repository.HistorialDao
import mx.gotadevida.solicitud.entity.UrgenciaEntity
import mx.gotadevida.solicitud.repository.SolicitudDao

/**
 * Base local (Room). Guarda copia de lo que viene de la API para que la app funcione sin señal.
 * Cada funcionalidad aporta sus entidades y su DAO; aquí solo se juntan.
 */
@Database(
    entities = [
        PerfilEntity::class,
        ElegibilidadEntity::class,
        UrgenciaEntity::class,
        AccionPendiente::class,
        CitaEntity::class,
        HistorialEntity::class,
        MensajeEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Convertidores::class)
abstract class GotaDatabase : RoomDatabase() {
    abstract fun donadorDao(): DonadorDao

    abstract fun solicitudDao(): SolicitudDao

    abstract fun accionPendienteDao(): AccionPendienteDao

    abstract fun citaDao(): CitaDao

    abstract fun historialDao(): HistorialDao

    abstract fun chatDao(): ChatDao

    companion object {
        const val NOMBRE = "gota_de_vida.db"
    }
}

class Convertidores {
    @TypeConverter
    fun deLista(valor: List<String>): String = Json.encodeToString(valor)

    @TypeConverter
    fun aLista(valor: String): List<String> = Json.decodeFromString(valor)
}
