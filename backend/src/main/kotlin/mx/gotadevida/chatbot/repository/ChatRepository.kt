package mx.gotadevida.chatbot.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import mx.gotadevida.chatbot.entity.Conversacion
import mx.gotadevida.chatbot.entity.PreguntaFrecuente
import mx.gotadevida.core.db.Colecciones

interface ConversacionRepository {
    suspend fun buscar(donadorId: String): Conversacion?

    suspend fun guardar(conversacion: Conversacion)
}

interface PreguntasFrecuentesRepository {
    suspend fun todas(): List<PreguntaFrecuente>

    suspend fun guardar(pregunta: PreguntaFrecuente)
}

class MongoConversacionRepository(db: MongoDatabase) : ConversacionRepository {
    private val coleccion = db.getCollection<Conversacion>(Colecciones.CONVERSACIONES)

    override suspend fun buscar(donadorId: String) = coleccion.find(Filters.eq("_id", donadorId)).firstOrNull()

    override suspend fun guardar(conversacion: Conversacion) {
        coleccion.replaceOne(Filters.eq("_id", conversacion.id), conversacion, ReplaceOptions().upsert(true))
    }
}

class MongoPreguntasFrecuentesRepository(db: MongoDatabase) : PreguntasFrecuentesRepository {
    private val coleccion = db.getCollection<PreguntaFrecuente>(Colecciones.PREGUNTAS_FRECUENTES)

    override suspend fun todas() = coleccion.find().toList()

    override suspend fun guardar(pregunta: PreguntaFrecuente) {
        coleccion.replaceOne(Filters.eq("_id", pregunta.id), pregunta, ReplaceOptions().upsert(true))
    }
}
