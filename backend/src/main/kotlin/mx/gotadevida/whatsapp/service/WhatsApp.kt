package mx.gotadevida.whatsapp.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Mensaje de texto (o botón) que llegó por WhatsApp. */
data class MensajeEntrante(val mensajeId: String, val telefono: String, val texto: String)

/** Envía mensajes por WhatsApp. Interfaz para poder probar sin Meta. */
interface ClienteWhatsApp {
    /** Texto libre (solo dentro de la ventana de 24 h). Con `opciones` se mandan hasta 3 botones. */
    suspend fun enviarTexto(telefono: String, texto: String, opciones: List<String> = emptyList())

    /** Plantilla aprobada `urgencia_donacion`: tipo de sangre, hospital y distancia, con botones "Sí puedo" / "No puedo". */
    suspend fun enviarUrgencia(telefono: String, tipoSangre: String, hospital: String, distanciaKm: String)
}

/** Sin WhatsApp configurado: solo registra lo que se habría enviado. */
class WhatsAppRegistro : ClienteWhatsApp {
    private val log = LoggerFactory.getLogger(WhatsAppRegistro::class.java)
    val enviados = mutableListOf<Pair<String, String>>()

    override suspend fun enviarTexto(telefono: String, texto: String, opciones: List<String>) {
        enviados += telefono to texto
        log.info("WhatsApp simulado a ***${telefono.takeLast(4)}: $texto $opciones")
    }

    override suspend fun enviarUrgencia(telefono: String, tipoSangre: String, hospital: String, distanciaKm: String) {
        enviados += telefono to "urgencia_donacion($tipoSangre, $hospital, $distanciaKm)"
    }
}

/**
 * WhatsApp Cloud API de Meta con el número de prueba gratuito (sin método de pago).
 * Solo puede escribir a los números verificados en Meta (los del equipo y el profesor).
 */
class ClienteWhatsAppMeta(
    private val token: String,
    private val idNumero: String,
    private val versionApi: String = "v23.0",
) : ClienteWhatsApp {
    private val log = LoggerFactory.getLogger(ClienteWhatsAppMeta::class.java)
    private val cliente = HttpClient(CIO)

    private suspend fun enviar(cuerpo: JsonObject) {
        val r = cliente.post("https://graph.facebook.com/$versionApi/$idNumero/messages") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(cuerpo.toString())
        }
        if (r.status.value >= 400) log.warn("WhatsApp respondió ${r.status}: ${r.bodyAsText().take(300)}")
    }

    override suspend fun enviarTexto(telefono: String, texto: String, opciones: List<String>) = enviar(
        buildJsonObject {
            put("messaging_product", "whatsapp")
            put("to", internacional(telefono))
            if (opciones.isEmpty() || opciones.size > 3) {
                put("type", "text")
                putJsonObject("text") { put("body", texto.take(4096)) }
            } else {
                put("type", "interactive")
                putJsonObject("interactive") {
                    put("type", "button")
                    putJsonObject("body") { put("text", texto.take(1024)) }
                    putJsonObject("action") {
                        putJsonArray("buttons") {
                            opciones.forEach { o ->
                                addJsonObject {
                                    put("type", "reply")
                                    putJsonObject("reply") { put("id", o.take(256)); put("title", o.take(20)) }
                                }
                            }
                        }
                    }
                }
            }
        },
    )

    override suspend fun enviarUrgencia(telefono: String, tipoSangre: String, hospital: String, distanciaKm: String) = enviar(
        buildJsonObject {
            put("messaging_product", "whatsapp")
            put("to", internacional(telefono))
            put("type", "template")
            putJsonObject("template") {
                put("name", "urgencia_donacion")
                putJsonObject("language") { put("code", "es_MX") }
                putJsonArray("components") {
                    addJsonObject {
                        put("type", "body")
                        putJsonArray("parameters") {
                            listOf(tipoSangre, hospital, distanciaKm).forEach { v -> addJsonObject { put("type", "text"); put("text", v) } }
                        }
                    }
                }
            }
        },
    )

    /** Los celulares se guardan con 10 dígitos; WhatsApp los quiere con lada de país. */
    private fun internacional(telefono: String) = "52" + telefono.filter { it.isDigit() }.takeLast(10)
}

object FirmaMeta {
    /** Verifica `X-Hub-Signature-256` (HMAC-SHA256 del cuerpo con el App Secret de Meta). */
    fun valida(cuerpo: String, cabecera: String?, secreto: String): Boolean {
        if (cabecera == null || !cabecera.startsWith("sha256=")) return false
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secreto.toByteArray(), "HmacSHA256")) }
        val esperada = mac.doFinal(cuerpo.toByteArray()).joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(esperada.toByteArray(), cabecera.removePrefix("sha256=").toByteArray())
    }

    fun firmar(cuerpo: String, secreto: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secreto.toByteArray(), "HmacSHA256")) }
        return "sha256=" + mac.doFinal(cuerpo.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/** Saca los mensajes de texto y respuestas de botón del JSON que manda Meta. */
fun extraerMensajes(cuerpo: JsonObject): List<MensajeEntrante> =
    cuerpo["entry"]?.jsonArray.orEmpty().flatMap { e ->
        e.jsonObject["changes"]?.jsonArray.orEmpty().flatMap { c ->
            c.jsonObject["value"]?.jsonObject?.get("messages")?.jsonArray.orEmpty().mapNotNull { m ->
                val o = m.jsonObject
                val texto = o["text"]?.jsonObject?.get("body")?.jsonPrimitive?.content
                    ?: o["button"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                    ?: o["interactive"]?.jsonObject?.get("button_reply")?.jsonObject?.get("title")?.jsonPrimitive?.content
                    ?: return@mapNotNull null
                MensajeEntrante(
                    mensajeId = o["id"]?.jsonPrimitive?.content.orEmpty(),
                    telefono = o["from"]?.jsonPrimitive?.content.orEmpty().filter { it.isDigit() }.takeLast(10),
                    texto = texto,
                )
            }
        }
    }
