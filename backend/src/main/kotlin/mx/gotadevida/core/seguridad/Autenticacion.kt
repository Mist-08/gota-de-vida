package mx.gotadevida.core.seguridad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.response.respond
import mx.gotadevida.core.errores.ErrorRespuesta
import mx.gotadevida.core.errores.NoAutorizado
import mx.gotadevida.core.errores.Prohibido
import kotlin.time.Duration.Companion.minutes

const val AUTH_JWT = "jwt"
val LIMITE_AUTENTICACION = RateLimitName("autenticacion")

/** Quién hace la petición, sacado del token de acceso. */
data class UsuarioActual(
    val id: String,
    val rol: Rol,
)

fun Application.configurarSeguridad(jwtServicio: JwtServicio) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "gota-de-vida"
            verifier(jwtServicio.verificador)
            validate { credencial ->
                val esAcceso = credencial.payload.getClaim(JwtServicio.CLAIM_TIPO).asString() == JwtServicio.TIPO_ACCESO
                val rolValido = Rol.entries.any { it.name == credencial.payload.getClaim(JwtServicio.CLAIM_ROL).asString() }
                if (esAcceso && rolValido && credencial.payload.subject != null) JWTPrincipal(credencial.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorRespuesta("no_autorizado", "Inicia sesión para continuar."),
                )
            }
        }
    }
    install(RateLimit) {
        register(LIMITE_AUTENTICACION) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
        }
        register(io.ktor.server.plugins.ratelimit.RateLimitName("webhook")) {
            rateLimiter(limit = 300, refillPeriod = 1.minutes)
        }
    }
}

fun ApplicationCall.usuarioActual(): UsuarioActual {
    val principal = principal<JWTPrincipal>() ?: throw NoAutorizado("Inicia sesión para continuar.")
    return UsuarioActual(
        id = principal.payload.subject,
        rol = Rol.valueOf(principal.payload.getClaim(JwtServicio.CLAIM_ROL).asString()),
    )
}

/** Verifica que quien llama tenga uno de los roles indicados. */
fun ApplicationCall.exigirRol(vararg roles: Rol): UsuarioActual {
    val usuario = usuarioActual()
    if (usuario.rol !in roles) throw Prohibido()
    return usuario
}
