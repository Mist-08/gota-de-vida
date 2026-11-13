package mx.gotadevida.donacion.service

import java.time.Clock
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import mx.gotadevida.core.dominio.DirectorioDonadores
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.Consumidor
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.donacion.entity.Donacion
import mx.gotadevida.donacion.entity.DonacionRespuesta
import mx.gotadevida.donacion.entity.HistorialRespuesta
import mx.gotadevida.donacion.entity.Insignia
import mx.gotadevida.donacion.entity.InsigniaRespuesta
import mx.gotadevida.donacion.repository.DonacionRepository

/** Historial del donador, insignias y registro de donaciones confirmadas. */
class HistorialService(
    private val donaciones: DonacionRepository,
    private val donadores: DirectorioDonadores,
    private val bus: BusDeEventos,
    private val reloj: Clock = Clock.systemUTC()
) {
    suspend fun historial(donadorId: String): HistorialRespuesta {
        val donador = donadores.resumen(donadorId) ?: throw NoEncontrado("No encontramos tu perfil de donador.")
        val lista = donaciones.deDonador(donadorId)
        return HistorialRespuesta(
            donaciones = lista.map { DonacionRespuesta(it.id, it.hospitalNombre, it.fecha, it.solicitudId != null) },
            totalDonaciones = lista.size,
            vidasQuePuedesAyudar = lista.size * 3,
            elegible = donador.elegible,
            proximaFechaElegible = donador.proximaFechaElegible,
            insignias = insignias(lista)
        )
    }

    /** HistorialConsumer: guarda la donación, actualiza la próxima fecha y da las gracias. */
    suspend fun registrar(
        citaId: String,
        donadorId: String,
        hospitalId: String,
        hospitalNombre: String,
        fecha: String,
        solicitudId: String?
    ) {
        donaciones.guardar(
            Donacion(citaId, donadorId, hospitalId, hospitalNombre, fecha, reloj.instant().toString(), solicitudId)
        )
        // Con la fecha nueva, la elegibilidad calcula sola la próxima fecha posible.
        donadores.registrarDonacion(donadorId, fecha)
        bus.publicar(
            TiposEvento.NOTIFICACION_ENVIAR,
            buildJsonObject {
                putJsonArray("usuarioIds") { add(donadorId) }
                putJsonObject("datos") { put("tipo", "donacion_confirmada") }
            }
        )
    }

    suspend fun contarDeHospital(hospitalId: String): Int = donaciones.deHospital(hospitalId).size

    companion object {
        fun insignias(lista: List<Donacion>): List<InsigniaRespuesta> = listOf(
            InsigniaRespuesta(Insignia.PRIMERA_DONACION, lista.isNotEmpty()),
            InsigniaRespuesta(Insignia.TRES_DONACIONES, lista.size >= 3),
            InsigniaRespuesta(Insignia.CINCO_DONACIONES, lista.size >= 5),
            InsigniaRespuesta(Insignia.HEROE_DE_URGENCIA, lista.any { it.solicitudId != null })
        )
    }
}

fun consumidorHistorial(servicio: HistorialService) = Consumidor("historial", listOf(TiposEvento.DONACION_CONFIRMADA)) { evento ->
    val d = evento.datos
    servicio.registrar(
        citaId = d.getValue("citaId").jsonPrimitive.content,
        donadorId = d.getValue("donadorId").jsonPrimitive.content,
        hospitalId = d.getValue("hospitalId").jsonPrimitive.content,
        hospitalNombre = d.getValue("hospitalNombre").jsonPrimitive.content,
        fecha = d.getValue("fecha").jsonPrimitive.content,
        solicitudId = d["solicitudId"]?.jsonPrimitive?.content
    )
}
