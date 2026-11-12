package mx.gotadevida.whatsapp.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.whatsapp.service.FirmaMeta
import mx.gotadevida.whatsapp.service.extraerMensajes

val LIMITE_WEBHOOK = RateLimitName("webhook")

/**
 * Webhook de WhatsApp. GET: verificación de Meta. POST: mensajes entrantes.
 * Responde 200 de inmediato y deja el trabajo a la cola (`whatsapp.mensaje.recibido`),
 * así Meta no reintenta y no se pierden mensajes si Gemini tarda.
 */
fun Route.webhookRoutes(tokenVerificacion: String?, secretoApp: String?, bus: BusDeEventos) {
    rateLimit(LIMITE_WEBHOOK) {
        route("/api/v1/webhooks/whatsapp") {
            get {
                val p = call.request.queryParameters
                if (tokenVerificacion != null && p["hub.mode"] == "subscribe" && p["hub.verify_token"] == tokenVerificacion) {
                    call.respondText(p["hub.challenge"].orEmpty())
                } else {
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
            post {
                val cuerpo = call.receiveText()
                if (secretoApp == null || !FirmaMeta.valida(cuerpo, call.request.header("X-Hub-Signature-256"), secretoApp)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val json = runCatching { Json.parseToJsonElement(cuerpo).jsonObject }.getOrNull()
                json?.let { extraerMensajes(it) }.orEmpty().forEach { m ->
                    bus.publicar(
                        TiposEvento.WHATSAPP_MENSAJE_RECIBIDO,
                        buildJsonObject {
                            put("mensajeId", m.mensajeId)
                            put("telefono", m.telefono)
                            put("texto", m.texto)
                        },
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
