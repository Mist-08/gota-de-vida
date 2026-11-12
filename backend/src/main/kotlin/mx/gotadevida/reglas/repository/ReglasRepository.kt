package mx.gotadevida.reglas.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.reglas.entity.ReglasElegibilidad

interface ReglasRepository {
    suspend fun obtenerVigentes(): ReglasElegibilidad?

    suspend fun guardar(reglas: ReglasElegibilidad)
}

class MongoReglasRepository(db: MongoDatabase) : ReglasRepository {
    private val coleccion = db.getCollection<ReglasElegibilidad>(Colecciones.REGLAS_ELEGIBILIDAD)

    override suspend fun obtenerVigentes(): ReglasElegibilidad? =
        coleccion.find(Filters.eq("_id", ReglasElegibilidad.ID_VIGENTES)).firstOrNull()

    override suspend fun guardar(reglas: ReglasElegibilidad) {
        coleccion.replaceOne(Filters.eq("_id", reglas.id), reglas, ReplaceOptions().upsert(true))
    }
}
