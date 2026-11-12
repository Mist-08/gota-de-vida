package mx.gotadevida.donador.routes

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
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.service.DonadorService

fun Route.donadorRoutes(servicio: DonadorService) {
    authenticate(AUTH_JWT) {
        route("/api/v1/donadores/me") {
            get {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(servicio.obtenerPerfil(usuario.id))
            }
            put {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(servicio.actualizarPerfil(usuario.id, call.receive<ActualizarPerfilSolicitud>()))
            }
            get("/elegibilidad") {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(servicio.evaluarElegibilidad(usuario.id))
            }
        }
    }
}
