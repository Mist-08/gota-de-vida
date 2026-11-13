package mx.gotadevida.chatbot.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class Autor { DONADOR, GOTA }

@Serializable
data class MensajeChat(val autor: Autor, val texto: String, val en: String)

@Serializable
data class HistorialChat(val mensajes: List<MensajeChat>)

@Serializable
data class MensajePeticion(val texto: String)

@Serializable
data class RespuestaChat(val respuesta: String, val opciones: List<String> = emptyList(), val origen: String)

/** Copia local de la conversación para verla sin conexión. */
@Entity(tableName = "mensajes_chat")
data class MensajeEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val autor: String, val texto: String, val en: String)
