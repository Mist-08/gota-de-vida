package mx.gotadevida.donador.entity

import kotlinx.serialization.Serializable
import mx.gotadevida.core.dominio.TipoSangre

/** Cambios al perfil. Los campos que no se manden se quedan igual. */
@Serializable
data class ActualizarPerfilSolicitud(
    val nombre: String? = null,
    val telefono: String? = null,
    val tipoSangre: TipoSangre? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
    val sexoParaCriteriosMedicos: SexoParaCriteriosMedicos? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val zona: String? = null,
    val fechaUltimaDonacion: String? = null,
    val fechaUltimoTatuaje: String? = null,
    val notificacionesActivas: Boolean? = null,
    val radioMaximoKm: Int? = null,
)

/** Perfil tal como lo ve la app. La ubicación sale como latitud y longitud aproximadas. */
@Serializable
data class PerfilDonadorRespuesta(
    val id: String,
    val nombre: String,
    val notificacionesActivas: Boolean,
    val radioMaximoKm: Int,
    val totalDonaciones: Int,
    val perfilCompleto: Boolean,
    val telefono: String? = null,
    val tipoSangre: TipoSangre? = null,
    val etiquetaTipoSangre: String? = null,
    val fechaNacimiento: String? = null,
    val pesoKg: Double? = null,
    val sexoParaCriteriosMedicos: SexoParaCriteriosMedicos? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val zona: String? = null,
    val ultimaDonacion: String? = null,
)

/** Resultado de revisar si el donador puede donar hoy. */
@Serializable
data class Elegibilidad(
    val elegible: Boolean,
    val faltanDatos: Boolean,
    val motivos: List<String>,
    val aviso: String,
    val proximaFechaElegible: String? = null,
    val diasParaVolverADonar: Long? = null,
)

fun Donador.aRespuesta() = PerfilDonadorRespuesta(
    id = id,
    nombre = nombre,
    notificacionesActivas = notificacionesActivas,
    radioMaximoKm = radioMaximoKm,
    totalDonaciones = fechasDonacion.size,
    perfilCompleto = tipoSangre != null && fechaNacimiento != null && pesoKg != null &&
        sexoParaCriteriosMedicos != null && ubicacion != null,
    telefono = telefono,
    tipoSangre = tipoSangre,
    etiquetaTipoSangre = tipoSangre?.etiqueta,
    fechaNacimiento = fechaNacimiento,
    pesoKg = pesoKg,
    sexoParaCriteriosMedicos = sexoParaCriteriosMedicos,
    latitud = ubicacion?.latitud,
    longitud = ubicacion?.longitud,
    zona = zona,
    ultimaDonacion = fechasDonacion.maxOrNull(),
)
