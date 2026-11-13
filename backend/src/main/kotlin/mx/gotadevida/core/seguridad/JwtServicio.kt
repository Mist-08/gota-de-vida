package mx.gotadevida.core.seguridad

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import java.time.Clock
import java.time.Duration
import java.util.Date
import kotlinx.serialization.Serializable
import mx.gotadevida.core.errores.NoAutorizado

/** Par de tokens que recibe la app al iniciar sesión. */
@Serializable
data class Tokens(val tokenAcceso: String, val tokenRefresco: String, val expiraEnSegundos: Long)

/**
 * Crea y verifica los JWT. El token de acceso dura 1 hora; el de refresco, 30 días.
 * El claim `tipo` evita que un token de refresco se use como token de acceso.
 */
class JwtServicio(secreto: String, private val emisor: String, private val reloj: Clock = Clock.systemUTC()) {
    private val algoritmo = Algorithm.HMAC256(secreto)

    val verificador: JWTVerifier = JWT.require(algoritmo)
        .withIssuer(emisor)
        .withAudience(AUDIENCIA)
        .build()

    fun crearTokens(usuarioId: String, rol: Rol): Tokens = Tokens(
        tokenAcceso = crear(usuarioId, rol, TIPO_ACCESO, DURACION_ACCESO),
        tokenRefresco = crear(usuarioId, rol, TIPO_REFRESCO, DURACION_REFRESCO),
        expiraEnSegundos = DURACION_ACCESO.seconds
    )

    /** Devuelve el id del usuario si el token de refresco es válido. */
    fun verificarRefresco(token: String): String {
        val decodificado = try {
            verificador.verify(token)
        } catch (e: JWTVerificationException) {
            throw NoAutorizado("Tu sesión expiró. Inicia sesión de nuevo.")
        }
        if (decodificado.getClaim(CLAIM_TIPO).asString() != TIPO_REFRESCO) {
            throw NoAutorizado("Tu sesión expiró. Inicia sesión de nuevo.")
        }
        return decodificado.subject
    }

    private fun crear(usuarioId: String, rol: Rol, tipo: String, duracion: Duration): String {
        val ahora = reloj.instant()
        return JWT.create()
            .withIssuer(emisor)
            .withAudience(AUDIENCIA)
            .withSubject(usuarioId)
            .withClaim(CLAIM_ROL, rol.name)
            .withClaim(CLAIM_TIPO, tipo)
            .withIssuedAt(Date.from(ahora))
            .withExpiresAt(Date.from(ahora.plus(duracion)))
            .sign(algoritmo)
    }

    companion object {
        const val AUDIENCIA = "gota-de-vida-app"
        const val CLAIM_ROL = "rol"
        const val CLAIM_TIPO = "tipo"
        const val TIPO_ACCESO = "acceso"
        const val TIPO_REFRESCO = "refresco"
        val DURACION_ACCESO: Duration = Duration.ofHours(1)
        val DURACION_REFRESCO: Duration = Duration.ofDays(30)
    }
}
