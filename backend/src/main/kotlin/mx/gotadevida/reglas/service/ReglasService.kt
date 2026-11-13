package mx.gotadevida.reglas.service

import java.time.Clock
import mx.gotadevida.core.errores.validar
import mx.gotadevida.reglas.entity.ActualizarReglasSolicitud
import mx.gotadevida.reglas.entity.ReglasElegibilidad
import mx.gotadevida.reglas.repository.ReglasRepository

class ReglasService(private val repositorio: ReglasRepository, private val reloj: Clock = Clock.systemUTC()) {
    /** Reglas vigentes; si nadie las ha guardado, las de por defecto. */
    suspend fun vigentes(): ReglasElegibilidad = repositorio.obtenerVigentes() ?: ReglasElegibilidad.porDefecto(reloj.instant().toString())

    /** Guarda las reglas por defecto si la base está vacía. */
    suspend fun asegurarReglas() {
        if (repositorio.obtenerVigentes() == null) {
            repositorio.guardar(ReglasElegibilidad.porDefecto(reloj.instant().toString()))
        }
    }

    suspend fun actualizar(cambios: ActualizarReglasSolicitud): ReglasElegibilidad {
        val actuales = vigentes()
        val nuevas = actuales.copy(
            edadMinima = cambios.edadMinima ?: actuales.edadMinima,
            edadMaxima = cambios.edadMaxima ?: actuales.edadMaxima,
            pesoMinimoKg = cambios.pesoMinimoKg ?: actuales.pesoMinimoKg,
            diasEntreDonaciones = cambios.diasEntreDonaciones ?: actuales.diasEntreDonaciones,
            maxDonacionesAnualesMasculino =
                cambios.maxDonacionesAnualesMasculino ?: actuales.maxDonacionesAnualesMasculino,
            maxDonacionesAnualesFemenino = cambios.maxDonacionesAnualesFemenino ?: actuales.maxDonacionesAnualesFemenino,
            horasAyunoMinimo = cambios.horasAyunoMinimo ?: actuales.horasAyunoMinimo,
            horasAyunoMaximo = cambios.horasAyunoMaximo ?: actuales.horasAyunoMaximo,
            mesesEsperaTatuaje = cambios.mesesEsperaTatuaje ?: actuales.mesesEsperaTatuaje,
            requisitos = cambios.requisitos?.map { it.trim() }?.filter { it.isNotEmpty() } ?: actuales.requisitos,
            fuente = cambios.fuente?.trim() ?: actuales.fuente,
            actualizadoEn = reloj.instant().toString()
        )
        validarReglas(nuevas)
        repositorio.guardar(nuevas)
        return nuevas
    }

    private fun validarReglas(r: ReglasElegibilidad) {
        validar(r.edadMinima in 16..r.edadMaxima) { "La edad mínima debe ser al menos 16 y no mayor que la máxima." }
        validar(r.edadMaxima <= 80) { "La edad máxima no puede pasar de 80." }
        validar(r.pesoMinimoKg in 40.0..80.0) { "El peso mínimo debe estar entre 40 y 80 kg." }
        validar(r.diasEntreDonaciones in 28..365) { "Los días entre donaciones deben estar entre 28 y 365." }
        validar(r.maxDonacionesAnualesMasculino in 1..6) { "El máximo anual (masculino) debe estar entre 1 y 6." }
        validar(r.maxDonacionesAnualesFemenino in 1..6) { "El máximo anual (femenino) debe estar entre 1 y 6." }
        validar(r.horasAyunoMinimo in 0..r.horasAyunoMaximo) {
            "Las horas de ayuno mínimas no pueden pasar de las máximas."
        }
        validar(r.mesesEsperaTatuaje in 0..24) { "La espera por tatuaje debe estar entre 0 y 24 meses." }
        validar(r.requisitos.isNotEmpty()) { "Debe haber al menos un requisito." }
    }
}
