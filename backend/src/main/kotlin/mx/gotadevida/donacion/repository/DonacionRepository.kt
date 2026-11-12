package mx.gotadevida.donacion.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.donacion.entity.Donacion

interface DonacionRepository {
    suspend fun guardar(donacion: Donacion)

    suspend fun deDonador(donadorId: String): List<Donacion>

    suspend fun deHospital(hospitalId: String): List<Donacion>
}

class MongoDonacionRepository(db: MongoDatabase) : DonacionRepository {
    private val coleccion = db.getCollection<Donacion>(Colecciones.DONACIONES)

    override suspend fun guardar(donacion: Donacion) {
        coleccion.replaceOne(Filters.eq("_id", donacion.id), donacion, ReplaceOptions().upsert(true))
    }

    override suspend fun deDonador(donadorId: String): List<Donacion> =
        coleccion.find(Filters.eq("donadorId", donadorId)).sort(Sorts.descending("fecha")).toList()

    override suspend fun deHospital(hospitalId: String): List<Donacion> =
        coleccion.find(Filters.eq("hospitalId", hospitalId)).toList()
}
