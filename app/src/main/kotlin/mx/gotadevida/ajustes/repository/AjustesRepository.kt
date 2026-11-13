package mx.gotadevida.ajustes.repository

import javax.inject.Inject
import kotlinx.serialization.Serializable
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT

/** Solo el dato que necesita Ajustes del perfil (el resto lo maneja `donador`). */
@Serializable
data class PreferenciasDonador(val notificacionesActivas: Boolean = true)

interface AjustesApi {
    @GET("api/v1/donadores/me")
    suspend fun preferencias(): PreferenciasDonador

    @PUT("api/v1/donadores/me")
    suspend fun guardarPreferencias(@Body preferencias: PreferenciasDonador)

    @DELETE("api/v1/usuarios/me")
    suspend fun eliminarCuenta()
}

interface AjustesRepository {
    suspend fun notificacionesActivas(): Resultado<Boolean>

    suspend fun cambiarNotificaciones(activas: Boolean): Resultado<Unit>

    suspend fun eliminarCuenta(): Resultado<Unit>
}

class AjustesRepositoryRed @Inject constructor(private val api: AjustesApi) : AjustesRepository {
    override suspend fun notificacionesActivas(): Resultado<Boolean> = when (val r = llamarApi { api.preferencias() }) {
        is Resultado.Exito -> Resultado.Exito(r.valor.notificacionesActivas)
        is Resultado.Fallo -> r
    }

    override suspend fun cambiarNotificaciones(activas: Boolean) = llamarApi { api.guardarPreferencias(PreferenciasDonador(activas)) }

    override suspend fun eliminarCuenta() = llamarApi { api.eliminarCuenta() }
}
