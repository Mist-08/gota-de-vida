package mx.gotadevida.chatbot.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.gotadevida.chatbot.entity.Autor
import mx.gotadevida.chatbot.entity.HistorialChat
import mx.gotadevida.chatbot.entity.MensajeChat
import mx.gotadevida.chatbot.entity.MensajeEntity
import mx.gotadevida.chatbot.entity.MensajePeticion
import mx.gotadevida.chatbot.entity.RespuestaChat
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.Instant
import javax.inject.Inject

interface ChatApi {
    @GET("api/v1/chat/mensajes")
    suspend fun historial(): HistorialChat

    @POST("api/v1/chat/mensajes")
    suspend fun enviar(@Body peticion: MensajePeticion): RespuestaChat
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM mensajes_chat ORDER BY id")
    fun observar(): Flow<List<MensajeEntity>>

    @Insert
    suspend fun agregar(mensajes: List<MensajeEntity>)

    @Query("DELETE FROM mensajes_chat")
    suspend fun borrar()

    @Transaction
    suspend fun reemplazar(mensajes: List<MensajeEntity>) {
        borrar()
        agregar(mensajes)
    }
}

/** Chat con Gota. El backend decide si contesta con preguntas frecuentes, Gemini o el menú. */
interface ChatRepository {
    fun observarMensajes(): Flow<List<MensajeChat>>

    suspend fun refrescar(): Resultado<Unit>

    suspend fun enviar(texto: String): Resultado<RespuestaChat>
}

class ChatRepositoryLocalYRed @Inject constructor(
    private val api: ChatApi,
    private val dao: ChatDao,
) : ChatRepository {
    override fun observarMensajes() = dao.observar().map { lista -> lista.map { MensajeChat(Autor.valueOf(it.autor), it.texto, it.en) } }

    override suspend fun refrescar(): Resultado<Unit> = when (val r = llamarApi { api.historial() }) {
        is Resultado.Exito -> Resultado.Exito(dao.reemplazar(r.valor.mensajes.map { MensajeEntity(autor = it.autor.name, texto = it.texto, en = it.en) }))
        is Resultado.Fallo -> r
    }

    override suspend fun enviar(texto: String): Resultado<RespuestaChat> {
        val r = llamarApi { api.enviar(MensajePeticion(texto)) }
        if (r is Resultado.Exito) {
            val ahora = Instant.now().toString()
            dao.agregar(
                listOf(
                    MensajeEntity(autor = Autor.DONADOR.name, texto = texto, en = ahora),
                    MensajeEntity(autor = Autor.GOTA.name, texto = r.valor.respuesta, en = ahora),
                ),
            )
        }
        return r
    }
}
