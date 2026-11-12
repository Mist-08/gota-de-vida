package mx.gotadevida.hospital.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol
import mx.gotadevida.core.seguridad.usuarioActual
import mx.gotadevida.hospital.entity.CrearHospitalSolicitud
import mx.gotadevida.hospital.service.EstadisticasService
import mx.gotadevida.hospital.service.HospitalService

fun Route.hospitalRoutes(servicio: HospitalService, estadisticas: EstadisticasService) {
    authenticate(AUTH_JWT) {
        get("/api/v1/hospitales/me/estadisticas") {
            val usuario = call.exigirRol(Rol.HOSPITAL)
            call.respond(estadisticas.deMiHospital(usuario.id))
        }
        route("/api/v1/hospitales") {
            get {
                call.usuarioActual()
                call.respond(servicio.listar())
            }
            post {
                call.exigirRol(Rol.ADMIN)
                call.respond(HttpStatusCode.Created, servicio.crear(call.receive<CrearHospitalSolicitud>()))
            }
            get("/{id}") {
                call.usuarioActual()
                val id = call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw DatosInvalidos("Falta el id del hospital.")
                call.respond(servicio.obtener(id))
            }
        }
    }
}
