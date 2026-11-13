package mx.gotadevida.core.errores

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/** Forma de todas las respuestas de error de la API. */
@Serializable
data class ErrorRespuesta(val error: String, val mensaje: String)

/** Error esperado de la lógica de negocio: se convierte en una respuesta HTTP clara. */
open class ErrorDeNegocio(val estado: HttpStatusCode, val codigo: String, mensaje: String) : RuntimeException(mensaje)

class NoEncontrado(mensaje: String) : ErrorDeNegocio(HttpStatusCode.NotFound, "no_encontrado", mensaje)

class DatosInvalidos(mensaje: String) : ErrorDeNegocio(HttpStatusCode.BadRequest, "datos_invalidos", mensaje)

class NoAutorizado(mensaje: String = "Correo o contraseña incorrectos.") :
    ErrorDeNegocio(HttpStatusCode.Unauthorized, "no_autorizado", mensaje)

class Prohibido(mensaje: String = "No tienes permiso para esta acción.") :
    ErrorDeNegocio(HttpStatusCode.Forbidden, "prohibido", mensaje)

class Conflicto(mensaje: String) : ErrorDeNegocio(HttpStatusCode.Conflict, "conflicto", mensaje)

fun Application.configurarErrores() {
    install(StatusPages) {
        exception<ErrorDeNegocio> { call, error ->
            call.respond(error.estado, ErrorRespuesta(error.codigo, error.message.orEmpty()))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorRespuesta("datos_invalidos", "El cuerpo de la petición no tiene el formato esperado.")
            )
        }
        exception<Throwable> { call, error ->
            call.application.log.error("Error no controlado", error)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorRespuesta("error_interno", "Ocurrió un error inesperado. Intenta de nuevo.")
            )
        }
    }
}

/** Atajo para validar datos de entrada dentro de los services. */
fun validar(condicion: Boolean, mensaje: () -> String) {
    if (!condicion) throw DatosInvalidos(mensaje())
}
