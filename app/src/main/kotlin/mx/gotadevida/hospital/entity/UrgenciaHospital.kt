package mx.gotadevida.hospital.entity

import kotlinx.serialization.Serializable

/** Tipos de sangre que el hospital puede pedir (mismos nombres que la API). */
@Serializable
enum class TipoSangrePaciente(val etiqueta: String) {
    O_NEGATIVO("O−"), O_POSITIVO("O+"), A_NEGATIVO("A−"), A_POSITIVO("A+"),
    B_NEGATIVO("B−"), B_POSITIVO("B+"), AB_NEGATIVO("AB−"), AB_POSITIVO("AB+"),
}

@Serializable
enum class EstadoUrgenciaHospital { ACTIVA, CUBIERTA, CERRADA, CANCELADA }

@Serializable
data class DonadorQueAcepto(
    val donadorId: String,
    val nombre: String,
    val telefono: String? = null,
    val distanciaKm: Double? = null,
    val respondioEn: String,
)

/** Urgencia vista por su hospital: con oleada, avisados y quién aceptó. */
@Serializable
data class UrgenciaHospital(
    val id: String,
    val hospitalNombre: String,
    val etiquetaTipoSangre: String,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val estado: EstadoUrgenciaHospital,
    val creadaEn: String,
    val nota: String? = null,
    val oleada: Int? = null,
    val donadoresNotificados: Int? = null,
    val aceptaciones: List<DonadorQueAcepto> = emptyList(),
)

@Serializable
data class CrearUrgenciaPeticion(val tipoSangre: TipoSangrePaciente, val unidades: Int, val nota: String? = null)

@Serializable
data class CambiarEstadoPeticion(val estado: EstadoUrgenciaHospital)
