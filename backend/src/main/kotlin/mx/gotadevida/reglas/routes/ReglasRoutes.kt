package mx.gotadevida.reglas.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol
import mx.gotadevida.reglas.entity.ActualizarReglasSolicitud
import mx.gotadevida.reglas.service.ReglasService

fun Route.reglasRoutes(servicio: ReglasService) {
    route("/api/v1/reglas-elegibilidad") {
        // Públicas: la app y el asistente las muestran antes de iniciar sesión.
        get {
            call.respond(servicio.vigentes())
        }
        authenticate(AUTH_JWT) {
            put {
                call.exigirRol(Rol.ADMIN)
                call.respond(servicio.actualizar(call.receive<ActualizarReglasSolicitud>()))
            }
        }
    }
}
