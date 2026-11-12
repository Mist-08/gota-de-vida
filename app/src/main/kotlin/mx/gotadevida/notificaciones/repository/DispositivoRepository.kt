package mx.gotadevida.notificaciones.repository

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.notificaciones.service.AvisosPush
import retrofit2.http.Body
import retrofit2.http.PUT
import javax.inject.Inject
import kotlin.coroutines.resume

@Serializable
data class DispositivoPeticion(val tokenFcm: String)

interface DispositivoApi {
    @PUT("api/v1/usuarios/me/dispositivo")
    suspend fun registrar(@Body peticion: DispositivoPeticion)
}

/** Avisa al backend a qué teléfono mandar las notificaciones de esta cuenta. */
class DispositivoRepository @Inject constructor(
    private val api: DispositivoApi,
    private val almacen: AlmacenSesion,
) {
    suspend fun registrar(token: String): Resultado<Unit> {
        if (almacen.actual() == null) return Resultado.Exito(Unit)
        return llamarApi { api.registrar(DispositivoPeticion(token)) }
    }

    /** Pide a Firebase el token de este teléfono y lo registra. Sin Firebase configurado no hace nada. */
    suspend fun registrarTokenActual() {
        if (!AvisosPush.configurado) return
        val token = suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                cont.resume(if (tarea.isSuccessful) tarea.result else null)
            }
        } ?: return
        registrar(token)
    }
}
