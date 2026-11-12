package mx.gotadevida.donador.service

import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.PerfilDonador
import java.time.LocalDate

/** Lo que la tarjeta principal del inicio le dice al donador. */
sealed interface EstadoDonacion {
    data object Cargando : EstadoDonacion

    data object CompletarPerfil : EstadoDonacion

    data class PuedeDonar(val ultimaDonacion: LocalDate?) : EstadoDonacion

    data class Esperar(val dias: Long, val desde: LocalDate) : EstadoDonacion

    data class NoPuede(val motivos: List<String>) : EstadoDonacion
}

object CalculadorEstado {
    fun calcular(perfil: PerfilDonador?, elegibilidad: Elegibilidad?): EstadoDonacion = when {
        perfil == null -> EstadoDonacion.Cargando
        !perfil.perfilCompleto || elegibilidad == null || elegibilidad.faltanDatos -> EstadoDonacion.CompletarPerfil
        elegibilidad.elegible -> EstadoDonacion.PuedeDonar(perfil.ultimaDonacion?.let(LocalDate::parse))
        elegibilidad.proximaFechaElegible != null && elegibilidad.diasParaVolverADonar != null ->
            EstadoDonacion.Esperar(
                dias = elegibilidad.diasParaVolverADonar,
                desde = LocalDate.parse(elegibilidad.proximaFechaElegible),
            )
        else -> EstadoDonacion.NoPuede(elegibilidad.motivos)
    }
}
