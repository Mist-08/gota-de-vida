package mx.gotadevida.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Resultado de una operación que puede fallar, sin lanzar excepciones hacia la UI. */
sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>

    data class Fallo(val fallo: FalloApp) : Resultado<Nothing>
}

/** Por qué falló algo. La UI decide qué texto mostrar en el idioma del usuario. */
sealed interface FalloApp {
    /** El servidor respondió con un error de negocio (su mensaje ya viene listo para mostrarse). */
    data class Servidor(val estado: Int, val codigo: String?, val mensaje: String?) : FalloApp

    /** No hubo conexión o el servidor no respondió. */
    data object SinConexion : FalloApp

    /** Cualquier otra cosa. */
    data object Desconocido : FalloApp
}

@Serializable
private data class ErrorRespuesta(val error: String? = null, val mensaje: String? = null)

private val jsonErrores = Json { ignoreUnknownKeys = true }

/** Ejecuta una llamada a la API y convierte las excepciones en [FalloApp]. */
suspend fun <T> llamarApi(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Resultado.Fallo(falloDeHttp(e.code(), e.response()?.errorBody()?.string()))
} catch (e: IOException) {
    Resultado.Fallo(FalloApp.SinConexion)
} catch (e: Exception) {
    Resultado.Fallo(FalloApp.Desconocido)
}

/** Lee el cuerpo `{"error": "...", "mensaje": "..."}` que manda el backend. */
internal fun falloDeHttp(estado: Int, cuerpo: String?): FalloApp.Servidor {
    val error = cuerpo?.let { runCatching { jsonErrores.decodeFromString<ErrorRespuesta>(it) }.getOrNull() }
    return FalloApp.Servidor(estado = estado, codigo = error?.error, mensaje = error?.mensaje)
}
