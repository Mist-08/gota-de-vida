package mx.gotadevida.chatbot.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mx.gotadevida.chatbot.entity.HistorialChat
import mx.gotadevida.chatbot.entity.MensajePeticion
import mx.gotadevida.chatbot.service.AsistenteService
import mx.gotadevida.core.errores.validar
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol

fun Route.chatRoutes(asistente: AsistenteService) {
    authenticate(AUTH_JWT) {
        post("/api/v1/chat/mensajes") {
            val usuario = call.exigirRol(Rol.DONADOR)
            val texto = call.receive<MensajePeticion>().texto
            validar(texto.isNotBlank()) { "Escribe un mensaje." }
            call.respond(asistente.responder(usuario.id, texto))
        }
        get("/api/v1/chat/mensajes") {
            val usuario = call.exigirRol(Rol.DONADOR)
            call.respond(HistorialChat(asistente.historial(usuario.id)))
        }
    }
}
