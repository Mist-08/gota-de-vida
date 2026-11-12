package mx.gotadevida.donador.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre

/** Solo se usa para calcular intervalos y máximos anuales de donación. */
@Serializable
enum class SexoParaCriteriosMedicos {
    FEMENINO,
    MASCULINO,
}

/**
 * Perfil del donador. Su `_id` es el mismo que el de su [mx.gotadevida.autenticacion.entity.Usuario].
 * Fechas en formato ISO (`2026-11-12`). La ubicación se guarda con precisión reducida.
 */
@Serializable
data class Donador(
    @SerialName("_id") val id: String,
    val nombre: String,
    val notificacionesActivas: Boolean,
    val radioMaximoKm: Int,
    val fechasDonacion: List<String>,
    val actualizadoEn: String,
    val telefono: String? = null,
    val tipoSangre: TipoSangre? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
    val sexoParaCriteriosMedicos: SexoParaCriteriosMedicos? = null,
    val ubicacion: PuntoGeo? = null,
    val zona: String? = null,
    val fechaUltimoTatuaje: String? = null,
) {
    companion object {
        const val RADIO_POR_DEFECTO_KM = 15

        fun nuevo(id: String, nombre: String, telefono: String?, ahora: String) = Donador(
            id = id,
            nombre = nombre,
            notificacionesActivas = true,
            radioMaximoKm = RADIO_POR_DEFECTO_KM,
            fechasDonacion = emptyList(),
            actualizadoEn = ahora,
            telefono = telefono,
        )
    }
}
