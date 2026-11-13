package mx.gotadevida.donador.service

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import mx.gotadevida.donador.entity.Donador
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.reglas.entity.ReglasElegibilidad

/**
 * Decide si un donador puede donar en una fecha, según las reglas vigentes.
 * Es una función pura (sin base de datos) para poder probarla con cualquier fecha.
 */
class ElegibilidadService {

    fun evaluar(donador: Donador, reglas: ReglasElegibilidad, hoy: LocalDate): Elegibilidad {
        val faltantes = mutableListOf<String>()
        val impedimentos = mutableListOf<String>()
        var proximaFecha = hoy

        if (donador.tipoSangre == null) faltantes += "Agrega tu tipo de sangre."

        val nacimiento = donador.fechaNacimiento?.let(LocalDate::parse)
        if (nacimiento == null) {
            faltantes += "Agrega tu fecha de nacimiento."
        } else {
            val edad = Period.between(nacimiento, hoy).years
            if (edad < reglas.edadMinima) {
                impedimentos += "Debes tener al menos ${reglas.edadMinima} años para donar."
                proximaFecha = maxOf(proximaFecha, nacimiento.plusYears(reglas.edadMinima.toLong()))
            }
            if (edad > reglas.edadMaxima) {
                impedimentos += "La edad máxima para donar es de ${reglas.edadMaxima} años."
            }
        }

        val peso = donador.pesoKg
        if (peso == null) {
            faltantes += "Agrega tu peso."
        } else if (peso < reglas.pesoMinimoKg) {
            impedimentos += "El peso mínimo para donar es de ${formatearKg(reglas.pesoMinimoKg)} kg."
        }

        val sexo = donador.sexoParaCriteriosMedicos
        if (sexo == null) faltantes += "Indica el sexo para criterios médicos."

        val donaciones = donador.fechasDonacion.map(LocalDate::parse).filter { !it.isAfter(hoy) }.sorted()
        donaciones.lastOrNull()?.let { ultima ->
            val desde = ultima.plusDays(reglas.diasEntreDonaciones.toLong())
            if (desde.isAfter(hoy)) {
                impedimentos += "Deben pasar ${reglas.diasEntreDonaciones} días entre donaciones."
                proximaFecha = maxOf(proximaFecha, desde)
            }
        }

        if (sexo != null) {
            val maximo = when (sexo) {
                SexoParaCriteriosMedicos.FEMENINO -> reglas.maxDonacionesAnualesFemenino
                SexoParaCriteriosMedicos.MASCULINO -> reglas.maxDonacionesAnualesMasculino
            }
            val haceUnAnio = hoy.minusYears(1)
            val enElUltimoAnio = donaciones.filter { it.isAfter(haceUnAnio) }
            if (enElUltimoAnio.size >= maximo) {
                impedimentos += "Ya llegaste al máximo de $maximo donaciones en 12 meses."
                // Cuando la donación más antigua de las que cuentan cumpla un año, baja el conteo.
                val liberaCupo = enElUltimoAnio[enElUltimoAnio.size - maximo].plusYears(1)
                proximaFecha = maxOf(proximaFecha, liberaCupo)
            }
        }

        donador.fechaUltimoTatuaje?.let(LocalDate::parse)?.let { tatuaje ->
            val desde = tatuaje.plusMonths(reglas.mesesEsperaTatuaje.toLong())
            if (desde.isAfter(hoy)) {
                impedimentos +=
                    "Después de un tatuaje o perforación hay que esperar ${reglas.mesesEsperaTatuaje} meses."
                proximaFecha = maxOf(proximaFecha, desde)
            }
        }

        val edadMaximaSuperada = impedimentos.any { it.startsWith("La edad máxima") }
        val pesoBajo = impedimentos.any { it.startsWith("El peso mínimo") }
        val elegible = faltantes.isEmpty() && impedimentos.isEmpty()
        // Si faltan datos o el impedimento no depende del tiempo, no hay fecha que prometer.
        val fecha = when {
            elegible -> hoy
            faltantes.isNotEmpty() || edadMaximaSuperada || pesoBajo -> null
            else -> proximaFecha
        }

        return Elegibilidad(
            elegible = elegible,
            faltanDatos = faltantes.isNotEmpty(),
            motivos = faltantes + impedimentos,
            aviso = AVISO_MEDICO,
            proximaFechaElegible = fecha?.toString(),
            diasParaVolverADonar = fecha?.let { ChronoUnit.DAYS.between(hoy, it) }
        )
    }

    private fun formatearKg(valor: Double): String = if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()

    companion object {
        const val AVISO_MEDICO = "La decisión final de donar la toma el personal médico del banco de sangre."
    }
}
