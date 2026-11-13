package mx.gotadevida.core.salud

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/** Respuesta de `/health`: sirve para saber si el servidor está arriba y para despertarlo en Render. */
@Serializable
data class EstadoSalud(val estado: String, val servicio: String, val version: String)

fun Route.saludRoutes() {
    get("/health") {
        call.respond(
            EstadoSalud(
                estado = "ok",
                servicio = "gota-de-vida-backend",
                version = "0.1.0"
            )
        )
    }
}
