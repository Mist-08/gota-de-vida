package mx.gotadevida.autenticacion.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Datos que confirmamos de una cuenta de Google. */
data class CuentaGoogle(
    val googleId: String,
    val email: String,
    val nombre: String,
)

/** Verifica el `idToken` que manda la app después de "Continuar con Google". */
interface VerificadorGoogle {
    /** Devuelve la cuenta si el token es válido y fue emitido para nuestra app; si no, null. */
    suspend fun verificar(idToken: String): CuentaGoogle?
}

/**
 * Verificación con el endpoint público `tokeninfo` de Google (gratis, sin llave).
 * Revisa que el token sea para nuestro `GOOGLE_CLIENT_ID` y que el correo esté verificado.
 */
class VerificadorGoogleTokenInfo(
    private val clientId: String,
    private val http: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : VerificadorGoogle {

    override suspend fun verificar(idToken: String): CuentaGoogle? {
        val respuesta = http.get("https://oauth2.googleapis.com/tokeninfo") {
            parameter("id_token", idToken)
        }
        if (!respuesta.status.isSuccess()) return null
        val info = respuesta.body<TokenInfo>()
        val emisorValido = info.iss == "accounts.google.com" || info.iss == "https://accounts.google.com"
        if (info.aud != clientId || !emisorValido || info.emailVerified != "true") return null
        val email = info.email ?: return null
        return CuentaGoogle(googleId = info.sub, email = email, nombre = info.name ?: email.substringBefore('@'))
    }

    @Serializable
    private data class TokenInfo(
        val aud: String,
        val sub: String,
        val iss: String,
        val email: String? = null,
        @SerialName("email_verified") val emailVerified: String? = null,
        val name: String? = null,
    )
}
