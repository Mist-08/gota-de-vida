package mx.gotadevida.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json
import mx.gotadevida.donador.entity.ElegibilidadEntity
import mx.gotadevida.donador.entity.PerfilEntity
import mx.gotadevida.donador.repository.DonadorDao

/**
 * Base local (Room). Guarda copia de lo que viene de la API para que la app funcione sin señal.
 * Cada funcionalidad aporta sus entidades y su DAO; aquí solo se juntan.
 */
@Database(
    entities = [PerfilEntity::class, ElegibilidadEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class GotaDatabase : RoomDatabase() {
    abstract fun donadorDao(): DonadorDao

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
