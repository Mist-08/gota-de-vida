package mx.gotadevida.solicitud.repository

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sync.ColaDeSalida
import mx.gotadevida.core.sync.EjecutorDeAccion
import mx.gotadevida.core.sync.ResultadoEnvio
import mx.gotadevida.solicitud.entity.ResponderPeticion
import mx.gotadevida.solicitud.entity.RespuestaPendiente
import mx.gotadevida.solicitud.entity.Urgencia
import mx.gotadevida.solicitud.entity.UrgenciaEntity
import mx.gotadevida.solicitud.entity.aEntidad
import mx.gotadevida.solicitud.entity.aModelo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject

interface SolicitudApi {
    @GET("api/v1/solicitudes")
    suspend fun cercanas(): List<Urgencia>

    @GET("api/v1/solicitudes/{id}")
    suspend fun detalle(@Path("id") id: String): Urgencia

    @POST("api/v1/solicitudes/{id}/respuestas")
    suspend fun responder(@Path("id") id: String, @Body peticion: ResponderPeticion): Urgencia
}

@Dao
interface SolicitudDao {
    @Query("SELECT * FROM urgencias WHERE cercana = 1 AND estado = 'ACTIVA' ORDER BY distanciaKm")
    fun observarCercanas(): Flow<List<UrgenciaEntity>>

    @Query("SELECT * FROM urgencias WHERE id = :id")
    fun observar(id: String): Flow<UrgenciaEntity?>

    @Query("SELECT * FROM urgencias WHERE id = :id")
    suspend fun buscar(id: String): UrgenciaEntity?

    @Upsert
    suspend fun guardar(urgencias: List<UrgenciaEntity>)

    @Query("UPDATE urgencias SET cercana = 0")
    suspend fun desmarcarCercanas()

    /** La lista nueva reemplaza a la anterior, pero se conservan las abiertas en detalle. */
    @Transaction
    suspend fun reemplazarCercanas(urgencias: List<UrgenciaEntity>) {
        desmarcarCercanas()
        guardar(urgencias)
    }
}

/** Qué pasó al responder una urgencia. */
enum class EnvioRespuesta { ENVIADA, PENDIENTE }

/**
 * Urgencias del donador: primero lo guardado en Room, luego lo nuevo de la API.
 * Sin conexión, la respuesta a una urgencia se guarda en la cola de salida y se manda después.
 */
interface SolicitudRepository {
    fun observarCercanas(): Flow<List<Urgencia>>

    fun observar(id: String): Flow<Urgencia?>

    /** Ids de urgencias con una respuesta "pendiente de enviar". */
    fun observarPendientes(): Flow<Set<String>>

    suspend fun refrescarCercanas(): Resultado<Unit>

    suspend fun refrescar(id: String): Resultado<Unit>

    suspend fun responder(id: String, acepta: Boolean): Resultado<EnvioRespuesta>
}

class SolicitudRepositoryLocalYRed @Inject constructor(
    private val api: SolicitudApi,
    private val dao: SolicitudDao,
    private val cola: ColaDeSalida,
    private val json: Json,
) : SolicitudRepository {

    override fun observarCercanas() = dao.observarCercanas().map { lista -> lista.map { it.aModelo() } }

    override fun observar(id: String) = dao.observar(id).map { it?.aModelo() }

    override fun observarPendientes(): Flow<Set<String>> = cola.claves.map { claves ->
        claves.filter { it.startsWith(PREFIJO) }.map { it.removePrefix(PREFIJO) }.toSet()
    }

    override suspend fun refrescarCercanas(): Resultado<Unit> = when (val r = llamarApi { api.cercanas() }) {
        is Resultado.Exito -> Resultado.Exito(dao.reemplazarCercanas(r.valor.map { it.aEntidad(cercana = true) }))
        is Resultado.Fallo -> r
    }

    override suspend fun refrescar(id: String): Resultado<Unit> = when (val r = llamarApi { api.detalle(id) }) {
        is Resultado.Exito -> Resultado.Exito(guardar(r.valor))
        is Resultado.Fallo -> r
    }

    override suspend fun responder(id: String, acepta: Boolean): Resultado<EnvioRespuesta> =
        when (val r = llamarApi { api.responder(id, ResponderPeticion(acepta)) }) {
            is Resultado.Exito -> {
                guardar(r.valor)
                Resultado.Exito(EnvioRespuesta.ENVIADA)
            }
            is Resultado.Fallo -> if (r.fallo == FalloApp.SinConexion) {
                cola.encolar(PREFIJO + id, TIPO_ACCION, json.encodeToString(RespuestaPendiente.serializer(), RespuestaPendiente(id, acepta)))
                Resultado.Exito(EnvioRespuesta.PENDIENTE)
            } else {
                r
            }
        }

    private suspend fun guardar(urgencia: Urgencia) {
        val cercana = dao.buscar(urgencia.id)?.cercana ?: false
        dao.guardar(listOf(urgencia.aEntidad(cercana)))
    }

    companion object {
        const val TIPO_ACCION = "responder_urgencia"
        const val PREFIJO = "urgencia:"
    }
}

/** Manda desde la cola de salida una respuesta guardada sin conexión. */
class EjecutorResponderUrgencia @Inject constructor(
    private val api: SolicitudApi,
    private val dao: SolicitudDao,
    private val json: Json,
) : EjecutorDeAccion {
    override val tipo = SolicitudRepositoryLocalYRed.TIPO_ACCION

    override suspend fun ejecutar(datos: String): ResultadoEnvio {
        val pendiente = json.decodeFromString(RespuestaPendiente.serializer(), datos)
        return when (val r = llamarApi { api.responder(pendiente.solicitudId, ResponderPeticion(pendiente.acepta)) }) {
            is Resultado.Exito -> {
                val cercana = dao.buscar(r.valor.id)?.cercana ?: false
                dao.guardar(listOf(r.valor.aEntidad(cercana)))
                ResultadoEnvio.ENVIADA
            }
            // Sin red o servidor caído: se reintenta. Un 409 (ya se cubrió) ya no tiene caso reintentarlo.
            is Resultado.Fallo -> when (val f = r.fallo) {
                is FalloApp.Servidor -> if (f.estado >= 500 || f.estado == 401) ResultadoEnvio.REINTENTAR else ResultadoEnvio.DESCARTAR
                else -> ResultadoEnvio.REINTENTAR
            }
        }
    }
}
