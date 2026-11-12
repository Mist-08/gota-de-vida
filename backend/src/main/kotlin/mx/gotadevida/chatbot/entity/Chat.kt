package mx.gotadevida.chatbot.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Canal { APP, WHATSAPP }

@Serializable
enum class Autor { DONADOR, GOTA }

@Serializable
data class MensajeChat(val autor: Autor, val texto: String, val en: String)

/**
 * Lo que la conversación "recuerda" para el flujo sin IA: la urgencia de la que se habla
 * y el día elegido para la cita.
 */
@Serializable
data class ContextoChat(
    val solicitudId: String? = null,
    val hospitalId: String? = null,
    val fecha: String? = null,
)

/** Conversación de un donador. `_id` = donadorId (la misma en la app y en WhatsApp). */
@Serializable
data class Conversacion(
    @SerialName("_id") val id: String,
    val mensajes: List<MensajeChat>,
    val contexto: ContextoChat,
    val actualizadaEn: String,
)

/** Pregunta frecuente: si el mensaje trae alguna palabra clave, se responde sin gastar cuota de Gemini. */
@Serializable
data class PreguntaFrecuente(
    @SerialName("_id") val id: String,
    val palabrasClave: List<String>,
    /** Puede usar {edadMinima}, {edadMaxima}, {pesoMinimoKg}, {diasEntreDonaciones}, {horasAyunoMinimo}, {horasAyunoMaximo}, {mesesEsperaTatuaje}. */
    val respuesta: String,
)

@Serializable
data class MensajePeticion(val texto: String)

@Serializable
data class RespuestaChat(
    val respuesta: String,
    /** Respuestas rápidas sugeridas (botones en la app y en WhatsApp). */
    val opciones: List<String> = emptyList(),
    /** De dónde salió: PREGUNTA_FRECUENTE, GEMINI o MENU (sin IA). Ayuda a explicar el ahorro de cuota. */
    val origen: String,
)

@Serializable
data class HistorialChat(val mensajes: List<MensajeChat>)
