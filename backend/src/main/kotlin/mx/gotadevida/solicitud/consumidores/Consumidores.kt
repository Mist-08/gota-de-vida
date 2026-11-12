package mx.gotadevida.solicitud.consumidores

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import mx.gotadevida.core.mensajeria.Consumidor
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.solicitud.service.AvanceService
import mx.gotadevida.solicitud.service.MatchingService

/** MatchingConsumer: busca donadores compatibles y cercanos y los avisa por oleadas. */
fun consumidorMatching(matching: MatchingService) =
    Consumidor("matching", listOf(TiposEvento.SOLICITUD_CREADA, TiposEvento.SOLICITUD_OLEADA)) { evento ->
        val id = evento.datos.getValue("solicitudId").jsonPrimitive.content
        val oleada = evento.datos["oleada"]?.jsonPrimitive?.int ?: 1
        matching.lanzarOleada(id, oleada)
    }

/** SolicitudConsumer: actualiza el contador de unidades y avisa al hospital. */
fun consumidorAvance(avance: AvanceService) =
    Consumidor("solicitud", listOf(TiposEvento.RESPUESTA_REGISTRADA)) { evento ->
        avance.actualizar(
            evento.datos.getValue("solicitudId").jsonPrimitive.content,
            evento.datos.getValue("acepta").jsonPrimitive.boolean,
        )
    }
