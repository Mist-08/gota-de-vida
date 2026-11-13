package mx.gotadevida.core.mensajeria

import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Evento que viaja por la cola (exchange `gota.eventos`).
 * `tipo` es también la routing key, por ejemplo `solicitud.creada`.
 */
@Serializable
data class Evento(val eventoId: String, val tipo: String, val ocurridoEn: String, val datos: JsonObject)

/** Routing keys de la sección 11 del prompt maestro. */
object TiposEvento {
    const val SOLICITUD_CREADA = "solicitud.creada"
    const val SOLICITUD_OLEADA = "solicitud.oleada"
    const val RESPUESTA_REGISTRADA = "respuesta.registrada"
    const val NOTIFICACION_ENVIAR = "notificacion.enviar"
    const val CITA_AGENDADA = "cita.agendada"
    const val CITA_RECORDATORIO = "cita.recordatorio"
    const val DONACION_CONFIRMADA = "donacion.confirmada"
    const val WHATSAPP_MENSAJE_RECIBIDO = "whatsapp.mensaje.recibido"
}

/** Quien procesa un tipo de evento. `nombre` identifica su cola y su registro de idempotencia. */
class Consumidor(val nombre: String, val routingKeys: List<String>, val manejar: suspend (Evento) -> Unit)

/**
 * Bus de eventos. En producción es RabbitMQ ([BusRabbitMq]); sin `RABBITMQ_URL`
 * (desarrollo y pruebas) se usa [BusEnMemoria], con el mismo comportamiento.
 */
interface BusDeEventos {
    suspend fun publicar(tipo: String, datos: JsonObject)

    /** Publica el evento cuando pasa `retraso` (oleadas y recordatorios). */
    suspend fun publicarConRetraso(tipo: String, datos: JsonObject, retraso: Duration)

    fun registrar(consumidor: Consumidor)

    /** Abre la conexión y empieza a consumir. Se llama una vez al arrancar. */
    suspend fun iniciar() {}

    fun cerrar() {}
}

fun nuevoEvento(tipo: String, datos: JsonObject, reloj: Clock = Clock.systemUTC()) =
    Evento(UUID.randomUUID().toString(), tipo, reloj.instant().toString(), datos)

/**
 * Bus dentro del mismo proceso. Con `sincrono = true` (pruebas) cada evento se procesa
 * antes de que `publicar` regrese; si no, en una corrutina aparte, como haría RabbitMQ.
 */
class BusEnMemoria(
    private val idempotencia: EventosProcesados,
    private val sincrono: Boolean = false,
    private val reloj: Clock = Clock.systemUTC()
) : BusDeEventos {
    private val log = LoggerFactory.getLogger(BusEnMemoria::class.java)
    private val consumidores = mutableListOf<Consumidor>()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Eventos con retraso pendientes (las pruebas los disparan con [dispararRetrasados]). */
    val retrasados = mutableListOf<Evento>()

    override fun registrar(consumidor: Consumidor) {
        consumidores += consumidor
    }

    override suspend fun publicar(tipo: String, datos: JsonObject) = entregar(nuevoEvento(tipo, datos, reloj))

    override suspend fun publicarConRetraso(tipo: String, datos: JsonObject, retraso: Duration) {
        val evento = nuevoEvento(tipo, datos, reloj)
        if (sincrono) {
            retrasados += evento
        } else {
            alcance.launch {
                delay(retraso)
                entregar(evento)
            }
        }
    }

    /** Solo pruebas: entrega ya los eventos con retraso. */
    suspend fun dispararRetrasados() {
        val pendientes = retrasados.toList()
        retrasados.clear()
        pendientes.forEach { entregar(it) }
    }

    private suspend fun entregar(evento: Evento) {
        val destino = consumidores.filter { evento.tipo in it.routingKeys }
        if (sincrono) {
            destino.forEach { procesar(it, evento) }
        } else {
            destino.forEach { c -> alcance.launch { procesar(c, evento) } }
        }
    }

    private suspend fun procesar(consumidor: Consumidor, evento: Evento) {
        if (!idempotencia.marcar(consumidor.nombre, evento.eventoId)) return
        repeat(INTENTOS) { intento ->
            try {
                consumidor.manejar(evento)
                return
            } catch (e: Exception) {
                log.warn("${consumidor.nombre} falló con ${evento.tipo} (intento ${intento + 1})", e)
                if (sincrono) throw e
            }
        }
        log.error("${consumidor.nombre} descartó ${evento.eventoId} tras $INTENTOS intentos")
    }

    companion object {
        const val INTENTOS = 3
    }
}
