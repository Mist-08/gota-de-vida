package mx.gotadevida.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Cola de salida (outbox, sección 9): lo que el donador hizo sin conexión espera aquí
 * y WorkManager lo manda cuando vuelve la señal.
 * `clave` evita duplicados (por ejemplo, responder dos veces la misma urgencia).
 */
@Entity(tableName = "acciones_pendientes")
data class AccionPendiente(@PrimaryKey val clave: String, val tipo: String, val datos: String, val creadaEn: Long, val intentos: Int = 0)

@Dao
interface AccionPendienteDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun guardar(accion: AccionPendiente)

    @Query("SELECT * FROM acciones_pendientes ORDER BY creadaEn")
    suspend fun todas(): List<AccionPendiente>

    @Query("SELECT clave FROM acciones_pendientes")
    fun observarClaves(): Flow<List<String>>

    @Query("DELETE FROM acciones_pendientes WHERE clave = :clave")
    suspend fun borrar(clave: String)

    @Query("UPDATE acciones_pendientes SET intentos = intentos + 1 WHERE clave = :clave")
    suspend fun sumarIntento(clave: String)
}

/** Resultado de intentar mandar una acción guardada. */
enum class ResultadoEnvio { ENVIADA, REINTENTAR, DESCARTAR }

/**
 * Cada funcionalidad registra cómo mandar sus acciones (Hilt las junta en un Set).
 * Así `core` no conoce las clases internas de `solicitud` ni de `cita`.
 */
interface EjecutorDeAccion {
    val tipo: String

    suspend fun ejecutar(datos: String): ResultadoEnvio
}

/** Puerta de entrada a la cola: guardar y pedir el envío. */
interface ColaDeSalida {
    val claves: Flow<List<String>>

    suspend fun encolar(clave: String, tipo: String, datos: String)
}

@Singleton
class ColaDeSalidaRoom @Inject constructor(private val dao: AccionPendienteDao, @param:ApplicationContext private val contexto: Context) :
    ColaDeSalida {
    override val claves: Flow<List<String>> = dao.observarClaves()

    override suspend fun encolar(clave: String, tipo: String, datos: String) {
        dao.guardar(AccionPendiente(clave, tipo, datos, System.currentTimeMillis()))
        programarEnvio(contexto)
    }

    companion object {
        private const val NOMBRE_TRABAJO = "enviar-acciones-pendientes"

        /** Se ejecuta en cuanto haya internet, con reintentos y espera exponencial. */
        fun programarEnvio(contexto: Context) {
            val trabajo = OneTimeWorkRequestBuilder<EnviarPendientesWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(contexto).enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.REPLACE, trabajo)
        }
    }
}

/** Manda las acciones pendientes en orden. Si alguna falla por red, WorkManager reintenta más tarde. */
@HiltWorker
class EnviarPendientesWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val dao: AccionPendienteDao,
    private val ejecutores: Set<@JvmSuppressWildcards EjecutorDeAccion>
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result = if (enviarPendientes(dao, ejecutores)) Result.success() else Result.retry()
}

/** Lógica del envío, separada del Worker para poder probarla. Devuelve `false` si hay que reintentar. */
suspend fun enviarPendientes(dao: AccionPendienteDao, ejecutores: Set<EjecutorDeAccion>): Boolean {
    var reintentar = false
    val porTipo = ejecutores.associateBy { it.tipo }
    for (accion in dao.todas()) {
        val resultado = porTipo[accion.tipo]?.ejecutar(accion.datos) ?: ResultadoEnvio.DESCARTAR
        when (resultado) {
            ResultadoEnvio.ENVIADA, ResultadoEnvio.DESCARTAR -> dao.borrar(accion.clave)

            ResultadoEnvio.REINTENTAR -> {
                dao.sumarIntento(accion.clave)
                if (accion.intentos + 1 >= MAXIMO_INTENTOS) dao.borrar(accion.clave) else reintentar = true
            }
        }
    }
    return !reintentar
}

private const val MAXIMO_INTENTOS = 20
