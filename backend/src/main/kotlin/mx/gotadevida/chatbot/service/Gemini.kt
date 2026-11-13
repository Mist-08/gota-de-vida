package mx.gotadevida.chatbot.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Un turno de la conversación tal como lo espera Gemini (`role` = user o model). */
data class TurnoGemini(val rol: String, val partes: JsonArray)

/** Respuesta de Gemini: texto final o llamadas a funciones. */
sealed interface RespuestaGemini {
    data class Texto(val texto: String) : RespuestaGemini

    data class Funciones(val parteDelModelo: JsonArray, val llamadas: List<Pair<String, JsonObject>>) : RespuestaGemini
}

class CuotaAgotada : RuntimeException("Gemini respondió 429: se acabó la cuota gratuita.")

/** Cliente de Gemini. Es una interfaz para poder usar uno falso en las pruebas. */
interface ClienteGemini {
    suspend fun generar(sistema: String, turnos: List<TurnoGemini>, herramientas: JsonArray): RespuestaGemini
}

/**
 * Gemini API (Google AI Studio, nivel gratuito) con function calling.
 * La llave vive solo en el backend (variable GEMINI_API_KEY).
 */
class ClienteGeminiHttp(private val llave: String, private val modelo: String) : ClienteGemini {
    private val json = Json { ignoreUnknownKeys = true }
    private val cliente = HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 30_000 } }

    override suspend fun generar(sistema: String, turnos: List<TurnoGemini>, herramientas: JsonArray): RespuestaGemini {
        val cuerpo = buildJsonObject {
            putJsonObject("systemInstruction") { putJsonArray("parts") { addJsonObject { put("text", sistema) } } }
            putJsonArray("contents") {
                turnos.forEach { t ->
                    addJsonObject {
                        put("role", t.rol)
                        put("parts", t.partes)
                    }
                }
            }
            putJsonArray("tools") { addJsonObject { put("functionDeclarations", herramientas) } }
            putJsonObject("generationConfig") {
                put("temperature", 0.3)
                put("maxOutputTokens", 400)
            }
        }
        val r = cliente.post("https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent") {
            header("x-goog-api-key", llave)
            contentType(ContentType.Application.Json)
            setBody(cuerpo.toString())
        }
        if (r.status == HttpStatusCode.TooManyRequests) throw CuotaAgotada()
        val texto = r.bodyAsText()
        check(r.status.value < 400) { "Gemini respondió ${r.status}: ${texto.take(300)}" }
        val partes = json.parseToJsonElement(texto).jsonObject["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray ?: JsonArray(emptyList())
        val llamadas = partes.mapNotNull { p ->
            p.jsonObject["functionCall"]?.jsonObject?.let { fc ->
                fc.getValue("name").jsonPrimitive.content to
                    (fc["args"]?.jsonObject ?: JsonObject(emptyMap()))
            }
        }
        return if (llamadas.isNotEmpty()) {
            RespuestaGemini.Funciones(partes, llamadas)
        } else {
            RespuestaGemini.Texto(
                partes.mapNotNull {
                    it.jsonObject["text"]?.jsonPrimitive?.content
                }.joinToString("").trim()
            )
        }
    }
}
