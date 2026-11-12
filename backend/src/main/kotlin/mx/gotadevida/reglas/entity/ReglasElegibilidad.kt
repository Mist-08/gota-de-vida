package mx.gotadevida.reglas.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Criterios para saber si alguien puede donar. Viven en la base de datos y no en el código
 * porque la NOM-253-SSA1-2025 (en vigor desde el 10 de abril de 2027) remite los criterios
 * a la Guía Nacional del Centro Nacional de la Transfusión Sanguínea, que puede cambiar.
 * Los valores por defecto deben confirmarse con el banco de sangre aliado.
 */
@Serializable
data class ReglasElegibilidad(
    @SerialName("_id") val id: String,
    val edadMinima: Int,
    val edadMaxima: Int,
    val pesoMinimoKg: Double,
    val diasEntreDonaciones: Int,
    val maxDonacionesAnualesMasculino: Int,
    val maxDonacionesAnualesFemenino: Int,
    val horasAyunoMinimo: Int,
    val horasAyunoMaximo: Int,
    val mesesEsperaTatuaje: Int,
    val requisitos: List<String>,
    val fuente: String,
    val actualizadoEn: String,
) {
    companion object {
        const val ID_VIGENTES = "vigentes"

        fun porDefecto(ahora: String) = ReglasElegibilidad(
            id = ID_VIGENTES,
            edadMinima = 18,
            edadMaxima = 65,
            pesoMinimoKg = 50.0,
            diasEntreDonaciones = 60,
            maxDonacionesAnualesMasculino = 4,
            maxDonacionesAnualesFemenino = 3,
            horasAyunoMinimo = 4,
            horasAyunoMaximo = 12,
            mesesEsperaTatuaje = 12,
            requisitos = listOf(
                "Tener entre 18 y 65 años.",
                "Pesar al menos 50 kg.",
                "Llegar con 4 a 12 horas de ayuno.",
                "No comer grasas, huevo ni lácteos 24 horas antes.",
                "Llevar identificación oficial vigente con fotografía.",
                "Esperar 1 año después de un tatuaje, perforación o acupuntura.",
                "Reservar entre 40 minutos y 2 horas para la donación.",
            ),
            fuente = "Valores por defecto de fuentes públicas (NOM-253-SSA1-2012 y bancos de sangre). Confirmar con el banco de sangre aliado.",
            actualizadoEn = ahora,
        )
    }
}

/** Cambios que puede hacer el ADMIN. Los campos que no se manden se quedan igual. */
@Serializable
data class ActualizarReglasSolicitud(
    val edadMinima: Int? = null,
    val edadMaxima: Int? = null,
    val pesoMinimoKg: Double? = null,
    val diasEntreDonaciones: Int? = null,
    val maxDonacionesAnualesMasculino: Int? = null,
    val maxDonacionesAnualesFemenino: Int? = null,
    val horasAyunoMinimo: Int? = null,
    val horasAyunoMaximo: Int? = null,
    val mesesEsperaTatuaje: Int? = null,
    val requisitos: List<String>? = null,
    val fuente: String? = null,
)
