package mx.gotadevida.core.mensajeria

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.gotadevida.core.db.Colecciones

/**
 * Idempotencia: RabbitMQ puede entregar un mensaje más de una vez.
 * Cada consumidor marca el evento antes de procesarlo; si ya estaba marcado, lo ignora.
 */
interface EventosProcesados {
    /** Devuelve `true` si es la primera vez que este consumidor ve el evento. */
    suspend fun marcar(consumidor: String, eventoId: String): Boolean

    /** Si el procesamiento falla, se quita la marca para poder reintentar. */
    suspend fun desmarcar(consumidor: String, eventoId: String)
}

@Serializable
private data class EventoProcesado(@SerialName("_id") val id: String, val procesadoEn: String)

class MongoEventosProcesados(db: MongoDatabase, private val reloj: Clock = Clock.systemUTC()) : EventosProcesados {
    private val coleccion = db.getCollection<EventoProcesado>(Colecciones.EVENTOS_PROCESADOS)

    override suspend fun marcar(consumidor: String, eventoId: String): Boolean = try {
        // El _id único hace que el segundo intento falle con "llave duplicada".
        coleccion.insertOne(EventoProcesado("$consumidor:$eventoId", reloj.instant().toString()))
        true
    } catch (e: MongoWriteException) {
        if (e.error.category == ErrorCategory.DUPLICATE_KEY) false else throw e
    }

    override suspend fun desmarcar(consumidor: String, eventoId: String) {
        coleccion.deleteOne(com.mongodb.client.model.Filters.eq("_id", "$consumidor:$eventoId"))
    }
}

class EventosProcesadosEnMemoria : EventosProcesados {
    private val vistos = ConcurrentHashMap.newKeySet<String>()

    override suspend fun marcar(consumidor: String, eventoId: String) = vistos.add("$consumidor:$eventoId")

    override suspend fun desmarcar(consumidor: String, eventoId: String) {
        vistos.remove("$consumidor:$eventoId")
    }
}
