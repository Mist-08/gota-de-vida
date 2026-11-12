package mx.gotadevida.cita.repository

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import mx.gotadevida.cita.entity.AgendarPeticion
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.entity.CitaEntity
import mx.gotadevida.cita.entity.Horario
import mx.gotadevida.cita.entity.Requisitos
import mx.gotadevida.cita.entity.aEntidad
import mx.gotadevida.cita.entity.aModelo
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sync.ColaDeSalida
import mx.gotadevida.core.sync.EjecutorDeAccion
import mx.gotadevida.core.sync.ResultadoEnvio
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query as Parametro
import javax.inject.Inject

interface CitaApi {
    @GET("api/v1/hospitales/{id}/horarios")
    suspend fun horarios(@Path("id") hospitalId: String, @Parametro("fecha") fecha: String): List<Horario>

    @POST("api/v1/citas")
    suspend fun agendar(@Body peticion: AgendarPeticion): Cita

    @GET("api/v1/citas/me")
    suspend fun misCitas(): List<Cita>

    @POST("api/v1/citas/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: String): Cita

    @GET("api/v1/reglas-elegibilidad")
    suspend fun requisitos(): Requisitos
}

@Dao
interface CitaDao {
    @Query("SELECT * FROM citas ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<CitaEntity>>

    @Upsert
    suspend fun guardar(citas: List<CitaEntity>)

    @Query("DELETE FROM citas")
    suspend fun borrarTodas()

    @Transaction
    suspend fun reemplazar(citas: List<CitaEntity>) {
        borrarTodas()
        guardar(citas)
    }
}

enum class EnvioCita { AGENDADA, PENDIENTE }

/** Citas del donador: Room primero (el QR se ve sin señal) y luego la API. */
interface CitaRepository {
    fun observarCitas(): Flow<List<Cita>>

    /** true si hay una cita guardada sin conexión esperando a enviarse. */
    fun observarAgendaPendiente(): Flow<Boolean>

    suspend fun refrescar(): Resultado<Unit>

    suspend fun horarios(hospitalId: String, fecha: String): Resultado<List<Horario>>

    suspend fun requisitos(): Resultado<Requisitos>

    suspend fun agendar(peticion: AgendarPeticion): Resultado<EnvioCita>

    suspend fun cancelar(id: String): Resultado<Unit>
}

class CitaRepositoryLocalYRed @Inject constructor(
    private val api: CitaApi,
    private val dao: CitaDao,
    private val cola: ColaDeSalida,
    private val json: Json,
) : CitaRepository {
    override fun observarCitas() = dao.observarTodas().map { lista -> lista.map { it.aModelo() } }

    override fun observarAgendaPendiente() = cola.claves.map { claves -> CLAVE in claves }

    override suspend fun refrescar(): Resultado<Unit> = when (val r = llamarApi { api.misCitas() }) {
        is Resultado.Exito -> Resultado.Exito(dao.reemplazar(r.valor.map { it.aEntidad() }))
        is Resultado.Fallo -> r
    }

    override suspend fun horarios(hospitalId: String, fecha: String) = llamarApi { api.horarios(hospitalId, fecha) }

    override suspend fun requisitos() = llamarApi { api.requisitos() }

    override suspend fun agendar(peticion: AgendarPeticion): Resultado<EnvioCita> = when (val r = llamarApi { api.agendar(peticion) }) {
        is Resultado.Exito -> {
            dao.guardar(listOf(r.valor.aEntidad()))
            Resultado.Exito(EnvioCita.AGENDADA)
        }
        is Resultado.Fallo -> if (r.fallo == FalloApp.SinConexion) {
            cola.encolar(CLAVE, TIPO_ACCION, json.encodeToString(AgendarPeticion.serializer(), peticion))
            Resultado.Exito(EnvioCita.PENDIENTE)
        } else {
            r
        }
    }

    override suspend fun cancelar(id: String): Resultado<Unit> = when (val r = llamarApi { api.cancelar(id) }) {
        is Resultado.Exito -> Resultado.Exito(dao.guardar(listOf(r.valor.aEntidad())))
        is Resultado.Fallo -> r
    }

    companion object {
        const val TIPO_ACCION = "agendar_cita"
        const val CLAVE = "cita:agendar"
    }
}

/** Manda desde la cola de salida una cita agendada sin conexión. */
class EjecutorAgendarCita @Inject constructor(
    private val api: CitaApi,
    private val dao: CitaDao,
    private val json: Json,
) : EjecutorDeAccion {
    override val tipo = CitaRepositoryLocalYRed.TIPO_ACCION

    override suspend fun ejecutar(datos: String): ResultadoEnvio =
        when (val r = llamarApi { api.agendar(json.decodeFromString(AgendarPeticion.serializer(), datos)) }) {
            is Resultado.Exito -> {
                dao.guardar(listOf(r.valor.aEntidad()))
                ResultadoEnvio.ENVIADA
            }
            is Resultado.Fallo -> when (val f = r.fallo) {
                // Si el horario se llenó mientras no había señal, ya no tiene caso reintentar.
                is FalloApp.Servidor -> if (f.estado >= 500 || f.estado == 401) ResultadoEnvio.REINTENTAR else ResultadoEnvio.DESCARTAR
                else -> ResultadoEnvio.REINTENTAR
            }
        }
}
