package mx.gotadevida.hospital.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.hospital.entity.Hospital

interface HospitalRepository {
    suspend fun crear(hospital: Hospital)

    suspend fun buscarPorId(id: String): Hospital?

    suspend fun listar(): List<Hospital>

    suspend fun contar(): Long
}

class MongoHospitalRepository(db: MongoDatabase) : HospitalRepository {
    private val coleccion = db.getCollection<Hospital>(Colecciones.HOSPITALES)

    override suspend fun crear(hospital: Hospital) {
        coleccion.insertOne(hospital)
    }

    override suspend fun buscarPorId(id: String): Hospital? =
        coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun listar(): List<Hospital> =
        coleccion.find().sort(Sorts.ascending("municipio", "nombre")).toList()

    override suspend fun contar(): Long = coleccion.countDocuments()
}
