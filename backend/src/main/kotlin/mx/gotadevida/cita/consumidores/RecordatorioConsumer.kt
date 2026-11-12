package mx.gotadevida.cita.consumidores

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.cita.service.CitaService
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.Consumidor
import mx.gotadevida.core.mensajeria.TiposEvento
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/**
 * RecordatorioConsumer: al agendar, programa avisos 24 h y 2 h antes de la cita.
 *
 * RabbitMQ no tiene "entregar a tal hora": se usan colas con TTL que regresan el mensaje
 * al exchange al vencer (dead-letter). Para no crear una cola por cada retraso distinto,
 * se usan saltos fijos de 60, 15, 5 y 1 minutos: el mensaje "brinca" hasta que llega su hora.
 */
class RecordatorioService(
    private val citas: CitaService,
    private val bus: BusDeEventos,
    private val reloj: Clock = Clock.systemUTC(),
) {
    suspend fun programar(citaId: String) {
        val cita = citas.buscar(citaId) ?: return
        val hora = LocalDateTime.parse(cita.fechaHora).atZone(CitaService.ZONA).toInstant()
        listOf(24L, 2L).forEach { horasAntes ->
            val momento = hora.minus(Duration.ofHours(horasAntes))
            if (momento.isAfter(reloj.instant())) esperar(citaId, momento)
        }
    }

    suspend fun alVencer(citaId: String, momento: Instant) {
        if (momento.isAfter(reloj.instant().plusSeconds(30))) return esperar(citaId, momento)
        val cita = citas.buscar(citaId) ?: return
        if (cita.estado != EstadoCita.AGENDADA) return
        bus.publicar(
            TiposEvento.NOTIFICACION_ENVIAR,
            buildJsonObject {
                putJsonArray("usuarioIds") { add(cita.donadorId) }
                putJsonObject("datos") {
                    put("tipo", "recordatorio_cita")
                    put("citaId", cita.id)
                    put("hospital", cita.hospitalNombre)
                    put("hora", cita.fechaHora.substring(11, 16))
                    cita.solicitudId?.let { put("solicitudId", it) }
                }
            },
        )
    }

    private suspend fun esperar(citaId: String, momento: Instant) {
        val falta = Duration.between(reloj.instant(), momento).toKotlinDuration()
        val salto = SALTOS.firstOrNull { it <= falta } ?: SALTOS.last()
        bus.publicarConRetraso(
            TiposEvento.CITA_RECORDATORIO,
            buildJsonObject {
                put("citaId", citaId)
                put("momento", momento.toString())
            },
            salto,
        )
    }

    companion object {
        val SALTOS = listOf(60.minutes, 15.minutes, 5.minutes, 1.minutes)
    }
}

fun consumidorRecordatorios(servicio: RecordatorioService) =
    Consumidor("recordatorio", listOf(TiposEvento.CITA_AGENDADA, TiposEvento.CITA_RECORDATORIO)) { evento ->
        val citaId = evento.datos.getValue("citaId").jsonPrimitive.content
        when (evento.tipo) {
            TiposEvento.CITA_AGENDADA -> servicio.programar(citaId)
            else -> servicio.alVencer(citaId, Instant.parse(evento.datos.getValue("momento").jsonPrimitive.content))
        }
    }
