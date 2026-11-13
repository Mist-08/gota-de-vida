package mx.gotadevida.autenticacion.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import mx.gotadevida.autenticacion.entity.DispositivoPeticion
import mx.gotadevida.autenticacion.entity.GoogleSolicitud
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RecuperarSolicitud
import mx.gotadevida.autenticacion.entity.RefrescoSolicitud
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.RestablecerSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioHospitalSolicitud
import mx.gotadevida.autenticacion.service.AutenticacionService
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.seguridad.AUTH_JWT
import mx.gotadevida.core.seguridad.LIMITE_AUTENTICACION
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.exigirRol
import mx.gotadevida.core.seguridad.usuarioActual

/**
 * Controller de autenticación: solo traduce HTTP ↔ service.
 * [existeHospital] lo provee la funcionalidad de hospitales, sin que esta dependa de ella.
 */
fun Route.autenticacionRoutes(
    servicio: AutenticacionService,
    existeHospital: suspend (hospitalId: String) -> Boolean,
    borrarDatosDelDonador: suspend (String) -> Unit = {}
) {
    rateLimit(LIMITE_AUTENTICACION) {
        route("/api/v1/auth") {
            post("/registro") {
                val solicitud = call.receive<RegistroSolicitud>()
                call.respond(HttpStatusCode.Created, servicio.registrarDonador(solicitud))
            }
            post("/login") {
                call.respond(servicio.iniciarSesion(call.receive<LoginSolicitud>()))
            }
            post("/google") {
                call.respond(servicio.iniciarSesionConGoogle(call.receive<GoogleSolicitud>()))
            }
            post("/refrescar") {
                call.respond(servicio.refrescar(call.receive<RefrescoSolicitud>()))
            }
            post("/recuperar") {
                servicio.solicitarRecuperacion(call.receive<RecuperarSolicitud>())
                call.respond(HttpStatusCode.NoContent)
            }
            post("/restablecer") {
                call.respond(servicio.restablecerContrasena(call.receive<RestablecerSolicitud>()))
            }
        }
    }

    // Cuentas del personal de un banco de sangre: solo el ADMIN las crea.
    authenticate(AUTH_JWT) {
        delete("/api/v1/usuarios/me") {
            val usuario = call.usuarioActual()
            servicio.eliminarCuenta(usuario.id, borrarDatosDelDonador)
            call.respond(HttpStatusCode.NoContent)
        }
        put("/api/v1/usuarios/me/dispositivo") {
            val usuario = call.usuarioActual()
            servicio.registrarDispositivo(usuario.id, call.receive<DispositivoPeticion>().tokenFcm)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/api/v1/hospitales/{id}/usuarios") {
            call.exigirRol(Rol.ADMIN)
            val hospitalId = call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: throw DatosInvalidos("Falta el id del hospital.")
            if (!existeHospital(hospitalId)) throw NoEncontrado("No existe ese hospital.")
            val cuenta = servicio.crearUsuarioHospital(hospitalId, call.receive<UsuarioHospitalSolicitud>())
            call.respond(HttpStatusCode.Created, cuenta)
        }
    }
}
