package mx.gotadevida.core.correo

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

/** Correo listo para enviar: versión HTML y versión de texto plano. */
data class Correo(val para: String, val nombre: String, val asunto: String, val html: String, val texto: String)

/** Envía correos. Interfaz para poder probar sin un proveedor real. */
interface EnviadorCorreo {
    suspend fun enviar(correo: Correo)
}

/** Sin proveedor configurado: solo registra lo que se habría enviado (y lo guarda para las pruebas). */
class CorreoRegistro : EnviadorCorreo {
    private val log = LoggerFactory.getLogger(CorreoRegistro::class.java)
    val enviados = mutableListOf<Correo>()

    override suspend fun enviar(correo: Correo) {
        enviados += correo
        log.info("Correo simulado a ${correo.para.take(3)}***: ${correo.asunto}")
    }
}

/**
 * API HTTP de Brevo. Se usa HTTPS (puerto 443) porque el plan gratis de Render
 * bloquea los puertos SMTP (25, 465 y 587).
 */
class CorreoBrevo(
    private val llave: String,
    private val remitente: String,
    private val nombreRemitente: String = "Gota de Vida"
) : EnviadorCorreo {
    private val log = LoggerFactory.getLogger(CorreoBrevo::class.java)
    private val cliente = HttpClient(CIO)

    override suspend fun enviar(correo: Correo) {
        val cuerpo = buildJsonObject {
            putJsonObject("sender") {
                put("email", remitente)
                put("name", nombreRemitente)
            }
            putJsonArray("to") {
                addJsonObject {
                    put("email", correo.para)
                    put("name", correo.nombre)
                }
            }
            put("subject", correo.asunto)
            put("htmlContent", correo.html)
            put("textContent", correo.texto)
        }
        val r = cliente.post("https://api.brevo.com/v3/smtp/email") {
            header("api-key", llave)
            contentType(ContentType.Application.Json)
            setBody(cuerpo.toString())
        }
        if (r.status.value >= 400) log.warn("Brevo respondió ${r.status}: ${r.bodyAsText().take(300)}")
    }
}
