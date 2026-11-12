package mx.gotadevida.cita.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mx.gotadevida.cita.entity.AgendarCitaPeticion
import mx.gotadevida.cita.entity.CheckinPeticion
import mx.gotadevida.cita.service.CitaService
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol

fun Route.citaRoutes(servicio: CitaService) {
    authenticate(AUTH_JWT) {
        get("/api/v1/hospitales/{id}/horarios") {
            call.exigirRol(Rol.DONADOR, Rol.HOSPITAL, Rol.ADMIN)
            val id = call.parameters["id"]?.takeIf { it.isNotBlank() } ?: throw DatosInvalidos("Falta el id del hospital.")
            call.respond(servicio.horarios(id, call.request.queryParameters["fecha"]))
        }
        get("/api/v1/hospitales/me/citas") {
            val usuario = call.exigirRol(Rol.HOSPITAL)
            call.respond(servicio.delHospital(usuario.id, call.request.queryParameters["fecha"]))
        }
        route("/api/v1/citas") {
            post {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(HttpStatusCode.Created, servicio.agendar(usuario.id, call.receive<AgendarCitaPeticion>()))
            }
            get("/me") {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(servicio.misCitas(usuario.id))
            }
            post("/{id}/cancelar") {
                val usuario = call.exigirRol(Rol.DONADOR)
                call.respond(servicio.cancelar(usuario.id, call.idCita()))
            }
            post("/{id}/checkin") {
                val usuario = call.exigirRol(Rol.HOSPITAL)
                call.respond(servicio.checkin(usuario.id, call.idCita(), call.receive<CheckinPeticion>().codigoQr))
            }
            post("/checkin-qr") {
                val usuario = call.exigirRol(Rol.HOSPITAL)
                call.respond(servicio.checkinConQr(usuario.id, call.receive<CheckinPeticion>().codigoQr))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.idCita(): String =
    parameters["id"]?.takeIf { it.isNotBlank() } ?: throw DatosInvalidos("Falta el id de la cita.")
