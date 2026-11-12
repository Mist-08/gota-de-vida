package mx.gotadevida.hospital.service

import java.time.Duration
import java.time.Instant
import kotlinx.serialization.Serializable
import mx.gotadevida.core.dominio.DirectorioUrgencias
import mx.gotadevida.core.errores.Prohibido

@Serializable
data class TipoSolicitado(val etiquetaTipoSangre: String, val urgencias: Int)

/** Métricas del panel del hospital. */
@Serializable
data class Estadisticas(
    val urgenciasTotales: Int,
    val urgenciasActivas: Int,
    val urgenciasCubiertas: Int,
    /** Minutos promedio desde que se publica hasta que se comprometen todas las unidades. */
    val minutosPromedioParaCubrir: Int?,
    val tiposMasSolicitados: List<TipoSolicitado>,
    val donacionesConfirmadas: Int,
    val citasHoy: Int
)

class EstadisticasService(
    private val urgencias: DirectorioUrgencias,
    private val hospitalDeUsuario: suspend (String) -> String?,
    private val donacionesDe: suspend (hospitalId: String) -> Int,
    private val citasHoyDe: suspend (hospitalId: String) -> Int
) {
    suspend fun deMiHospital(usuarioId: String): Estadisticas {
        val hospitalId = hospitalDeUsuario(usuarioId) ?: throw Prohibido("Tu cuenta no pertenece a un hospital.")
        val lista = urgencias.deHospital(hospitalId)
        val tiempos = lista.mapNotNull { u ->
            u.cubiertaEn?.let { Duration.between(Instant.parse(u.creadaEn), Instant.parse(it)).toMinutes() }
        }
        return Estadisticas(
            urgenciasTotales = lista.size,
            urgenciasActivas = lista.count { it.estado == "ACTIVA" },
            urgenciasCubiertas = lista.count { it.cubiertaEn != null },
            minutosPromedioParaCubrir = tiempos.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            tiposMasSolicitados = lista.groupingBy { it.etiquetaTipoSangre }.eachCount()
                .entries.sortedByDescending { it.value }.take(3).map { TipoSolicitado(it.key, it.value) },
            donacionesConfirmadas = donacionesDe(hospitalId),
            citasHoy = citasHoyDe(hospitalId)
        )
    }
}
