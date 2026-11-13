package mx.gotadevida.core.mensajeria

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import java.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * RabbitMQ en CloudAMQP (plan gratuito Little Lemur).
 *
 * - Una sola conexión AMQP (el plan permite pocas) y un canal por consumidor.
 * - Exchange `gota.eventos` tipo topic; cada consumidor tiene su cola `gota.<consumidor>`.
 * - Si el consumidor falla, se reintenta hasta [INTENTOS] veces; después el mensaje
 *   va a su cola de fallidos `gota.<consumidor>.dlq` (dead-letter).
 * - Retrasos (oleadas, recordatorios): colas `gota.retraso.*` con TTL que, al vencer,
 *   regresan el mensaje al exchange principal con su routing key original.
 *
 * Los consumidores corren como corrutinas dentro del mismo proceso de Ktor:
 * Render Free no tiene workers en segundo plano gratuitos.
 */
class BusRabbitMq(private val uri: String, private val idempotencia: EventosProcesados, private val reloj: Clock = Clock.systemUTC()) :
    BusDeEventos {
    private val log = LoggerFactory.getLogger(BusRabbitMq::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val consumidores = mutableListOf<Consumidor>()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var conexion: Connection
    private lateinit var canalPublicacion: Channel
    private val colasDeRetraso = mutableSetOf<String>()

    override fun registrar(consumidor: Consumidor) {
        consumidores += consumidor
    }

    override suspend fun iniciar() {
        val fabrica = ConnectionFactory().apply {
            setUri(uri)
            isAutomaticRecoveryEnabled = true
            requestedHeartbeat = 30
        }
        conexion = fabrica.newConnection("gota-de-vida-api")
        canalPublicacion = conexion.createChannel()
        canalPublicacion.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true)
        canalPublicacion.exchangeDeclare(EXCHANGE_FALLIDOS, BuiltinExchangeType.DIRECT, true)
        consumidores.forEach { iniciarConsumidor(it) }
        log.info("RabbitMQ listo: ${consumidores.size} consumidores en una sola conexión")
    }

    private fun iniciarConsumidor(consumidor: Consumidor) {
        val canal = conexion.createChannel()
        canal.basicQos(5)
        val cola = "gota.${consumidor.nombre}"
        val dlq = "$cola.dlq"
        canal.queueDeclare(dlq, true, false, false, null)
        canal.queueBind(dlq, EXCHANGE_FALLIDOS, cola)
        canal.queueDeclare(
            cola,
            true,
            false,
            false,
            mapOf("x-dead-letter-exchange" to EXCHANGE_FALLIDOS, "x-dead-letter-routing-key" to cola)
        )
        consumidor.routingKeys.forEach { canal.queueBind(cola, EXCHANGE, it) }

        val alRecibir = DeliverCallback { _, entrega ->
            alcance.launch {
                val etiqueta = entrega.envelope.deliveryTag
                val intentos = (entrega.properties.headers?.get(CABECERA_INTENTOS) as? Number)?.toInt() ?: 0
                val evento = runCatching { json.decodeFromString<Evento>(String(entrega.body)) }.getOrNull()
                if (evento == null) {
                    log.error("Mensaje ilegible en $cola; va a la cola de fallidos")
                    canal.basicNack(etiqueta, false, false)
                    return@launch
                }
                if (!idempotencia.marcar(consumidor.nombre, evento.eventoId)) {
                    canal.basicAck(etiqueta, false)
                    return@launch
                }
                try {
                    consumidor.manejar(evento)
                    canal.basicAck(etiqueta, false)
                } catch (e: Exception) {
                    idempotencia.desmarcar(consumidor.nombre, evento.eventoId)
                    if (intentos + 1 < INTENTOS) {
                        log.warn("${consumidor.nombre} falló (intento ${intentos + 1}); se reintenta", e)
                        val props = entrega.properties.builder()
                            .headers((entrega.properties.headers.orEmpty()) + (CABECERA_INTENTOS to intentos + 1))
                            .build()
                        synchronized(canalPublicacion) {
                            canalPublicacion.basicPublish("", cola, props, entrega.body)
                        }
                        canal.basicAck(etiqueta, false)
                    } else {
                        log.error("${consumidor.nombre} agotó los reintentos; va a $dlq", e)
                        canal.basicNack(etiqueta, false, false)
                    }
                }
            }
        }
        canal.basicConsume(cola, false, alRecibir) { _ -> }
    }

    override suspend fun publicar(tipo: String, datos: JsonObject) {
        enviar(EXCHANGE, tipo, nuevoEvento(tipo, datos, reloj))
    }

    override suspend fun publicarConRetraso(tipo: String, datos: JsonObject, retraso: Duration) {
        val ms = retraso.inWholeMilliseconds
        val cola = "gota.retraso.$tipo.$ms"
        synchronized(canalPublicacion) {
            if (colasDeRetraso.add(cola)) {
                canalPublicacion.queueDeclare(
                    cola,
                    true,
                    false,
                    false,
                    mapOf(
                        "x-message-ttl" to ms,
                        "x-dead-letter-exchange" to EXCHANGE,
                        "x-dead-letter-routing-key" to tipo
                    )
                )
            }
        }
        enviar("", cola, nuevoEvento(tipo, datos, reloj))
    }

    private fun enviar(exchange: String, routingKey: String, evento: Evento) {
        val props = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2) // persistente
            .messageId(evento.eventoId)
            .build()
        synchronized(canalPublicacion) {
            canalPublicacion.basicPublish(
                exchange,
                routingKey,
                props,
                json.encodeToString(Evento.serializer(), evento).toByteArray()
            )
        }
    }

    override fun cerrar() {
        runCatching { conexion.close() }
    }

    companion object {
        const val EXCHANGE = "gota.eventos"
        const val EXCHANGE_FALLIDOS = "gota.fallidos"
        const val CABECERA_INTENTOS = "x-intentos"
        const val INTENTOS = 3
    }
}
