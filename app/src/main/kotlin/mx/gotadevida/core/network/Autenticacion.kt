package mx.gotadevida.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mx.gotadevida.core.sesion.AlmacenSesion
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

private const val RUTA_AUTENTICACION = "/api/v1/auth/"

/** Agrega el token de acceso a cada petición que lo necesita. */
class InterceptorToken(private val almacen: AlmacenSesion) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        if (peticion.url.encodedPath.contains(RUTA_AUTENTICACION)) return chain.proceed(peticion)
        val token = runBlocking { almacen.actual()?.tokenAcceso } ?: return chain.proceed(peticion)
        return chain.proceed(peticion.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

@Serializable
private data class RefrescoCuerpo(val tokenRefresco: String)

@Serializable
private data class TokensRespuesta(val tokenAcceso: String, val tokenRefresco: String)

@Serializable
private data class SesionRefrescada(val tokens: TokensRespuesta)

/**
 * Cuando el servidor responde 401 (token vencido), pide tokens nuevos con el de refresco
 * y repite la petición una vez. Si el refresco también falla, cierra la sesión: la app
 * regresa sola a la bienvenida.
 */
class RenovadorSesion(
    private val almacen: AlmacenSesion,
    private val urlBase: String,
    private val json: Json,
    private val cliente: OkHttpClient = OkHttpClient()
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains(RUTA_AUTENTICACION)) return null
        if (response.priorResponse != null) return null // ya se intentó una vez

        return runBlocking {
            val sesion = almacen.actual() ?: return@runBlocking null
            val enviado = response.request.header("Authorization")?.removePrefix("Bearer ")
            // Otra petición ya renovó el token mientras esta esperaba.
            if (enviado != null && enviado != sesion.tokenAcceso) {
                return@runBlocking response.request.conToken(sesion.tokenAcceso)
            }
            val nuevos = refrescar(sesion.tokenRefresco)
            if (nuevos == null) {
                almacen.borrar()
                null
            } else {
                almacen.actualizarTokens(nuevos.tokenAcceso, nuevos.tokenRefresco)
                response.request.conToken(nuevos.tokenAcceso)
            }
        }
    }

    private fun refrescar(tokenRefresco: String): TokensRespuesta? = runCatching {
        val cuerpo = json.encodeToString(RefrescoCuerpo.serializer(), RefrescoCuerpo(tokenRefresco))
            .toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url(urlBase.trimEnd('/') + RUTA_AUTENTICACION + "refrescar")
            .post(cuerpo)
            .build()
        cliente.newCall(peticion).execute().use { r ->
            if (!r.isSuccessful) return@use null
            json.decodeFromString(SesionRefrescada.serializer(), r.body.string()).tokens
        }
    }.getOrNull()

    private fun Request.conToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()
}
