package mx.gotadevida.donacion.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol
import mx.gotadevida.donacion.service.HistorialService

fun Route.donacionRoutes(servicio: HistorialService) {
    authenticate(AUTH_JWT) {
        get("/api/v1/donaciones/me") {
            val usuario = call.exigirRol(Rol.DONADOR)
            call.respond(servicio.historial(usuario.id))
        }
    }
}
