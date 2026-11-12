package mx.gotadevida.donador.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.donador.entity.Donador

interface DonadorRepository {
    suspend fun buscarPorId(id: String): Donador?

    /** Crea o reemplaza el perfil completo. */
    suspend fun guardar(donador: Donador)
}

class MongoDonadorRepository(db: MongoDatabase) : DonadorRepository {
    private val coleccion = db.getCollection<Donador>(Colecciones.DONADORES)

    override suspend fun buscarPorId(id: String): Donador? =
        coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun guardar(donador: Donador) {
        coleccion.replaceOne(Filters.eq("_id", donador.id), donador, ReplaceOptions().upsert(true))
    }
}
