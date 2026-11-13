package mx.gotadevida.solicitud.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol
import mx.gotadevida.solicitud.entity.CambiarEstadoPeticion
import mx.gotadevida.solicitud.entity.CrearSolicitudPeticion
import mx.gotadevida.solicitud.entity.ResponderPeticion
import mx.gotadevida.solicitud.service.SolicitudService

fun Route.solicitudRoutes(servicio: SolicitudService) {
    authenticate(AUTH_JWT) {
        route("/api/v1/solicitudes") {
            get {
                val usuario = call.exigirRol(Rol.DONADOR)
                fun numero(nombre: String) = call.request.queryParameters[nombre]?.let {
                    it.toDoubleOrNull() ?: throw DatosInvalidos("El parámetro $nombre debe ser un número.")
                }
                call.respond(servicio.cercanas(usuario.id, numero("lat"), numero("lng"), numero("radioKm")))
            }
            post {
                val usuario = call.exigirRol(Rol.HOSPITAL)
                call.respond(HttpStatusCode.Created, servicio.crear(usuario, call.receive<CrearSolicitudPeticion>()))
            }
            get("/{id}") {
                val usuario = call.exigirRol(Rol.DONADOR, Rol.HOSPITAL, Rol.ADMIN)
                call.respond(servicio.detalle(usuario, call.idSolicitud()))
            }
            patch("/{id}/estado") {
                val usuario = call.exigirRol(Rol.HOSPITAL, Rol.ADMIN)
                call.respond(
                    servicio.cambiarEstado(usuario, call.idSolicitud(), call.receive<CambiarEstadoPeticion>().estado)
                )
            }
            post("/{id}/respuestas") {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(
                    servicio.responder(usuario.id, call.idSolicitud(), call.receive<ResponderPeticion>().acepta)
                )
            }
        }
        get("/api/v1/hospitales/me/solicitudes") {
            val usuario = call.exigirRol(Rol.HOSPITAL)
            call.respond(servicio.delHospital(usuario))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.idSolicitud(): String =
    parameters["id"]?.takeIf { it.isNotBlank() } ?: throw DatosInvalidos("Falta el id de la urgencia.")
