package mx.gotadevida.solicitud.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre

@Serializable
enum class EstadoSolicitud { ACTIVA, CUBIERTA, CERRADA, CANCELADA }

/**
 * Urgencia publicada por un hospital. `tipoSangre` es el del paciente (receptor):
 * se avisa a los donadores cuyo tipo puede donarle (ver [TipoSangre.puedeDonarA]).
 * `notificados` evita avisar dos veces al mismo donador entre oleadas.
 */
@Serializable
data class Solicitud(
    @SerialName("_id") val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val ubicacion: PuntoGeo,
    val tipoSangre: TipoSangre,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val estado: EstadoSolicitud,
    val creadaEn: String,
    val actualizadaEn: String,
    val oleada: Int = 0,
    val radioKm: Double = 0.0,
    val notificados: List<String> = emptyList(),
    val nota: String? = null,
    val cubiertaEn: String? = null
)

/** Respuesta de un donador. Su `_id` es `solicitudId:donadorId`: una por donador. */
@Serializable
data class Respuesta(
    @SerialName("_id") val id: String,
    val solicitudId: String,
    val donadorId: String,
    val acepta: Boolean,
    val distanciaKm: Double?,
    val creadaEn: String
)

@Serializable
data class CrearSolicitudPeticion(val tipoSangre: TipoSangre, val unidades: Int, val nota: String? = null)

@Serializable
data class CambiarEstadoPeticion(val estado: EstadoSolicitud)

@Serializable
data class ResponderPeticion(val acepta: Boolean)

/** Lo que el hospital ve de un donador que aceptó (nunca su ubicación). */
@Serializable
data class DonadorQueAcepto(
    val donadorId: String,
    val nombre: String,
    val telefono: String?,
    val distanciaKm: Double?,
    val respondioEn: String
)

@Serializable
data class SolicitudRespuesta(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val hospitalDireccion: String,
    val latitud: Double,
    val longitud: Double,
    val tipoSangre: TipoSangre,
    val etiquetaTipoSangre: String,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val estado: EstadoSolicitud,
    val creadaEn: String,
    val nota: String? = null,
    /** Solo para el donador: distancia aproximada desde su zona. */
    val distanciaKm: Double? = null,
    /** Solo para el donador: true si aceptó, false si dijo que no, null si no ha respondido. */
    val miRespuesta: Boolean? = null,
    /** Solo para el hospital dueño. */
    val oleada: Int? = null,
    val donadoresNotificados: Int? = null,
    val aceptaciones: List<DonadorQueAcepto>? = null
)

fun Solicitud.aRespuesta(distanciaKm: Double? = null, miRespuesta: Boolean? = null, aceptaciones: List<DonadorQueAcepto>? = null) =
    SolicitudRespuesta(
        id = id,
        hospitalId = hospitalId,
        hospitalNombre = hospitalNombre,
        hospitalDireccion = hospitalDireccion,
        latitud = ubicacion.latitud,
        longitud = ubicacion.longitud,
        tipoSangre = tipoSangre,
        etiquetaTipoSangre = tipoSangre.etiqueta,
        unidades = unidades,
        unidadesComprometidas = unidadesComprometidas,
        estado = estado,
        creadaEn = creadaEn,
        nota = nota,
        distanciaKm = distanciaKm?.let { Math.round(it * 10) / 10.0 },
        miRespuesta = miRespuesta,
        oleada = aceptaciones?.let { oleada },
        donadoresNotificados = aceptaciones?.let { notificados.size },
        aceptaciones = aceptaciones
    )
