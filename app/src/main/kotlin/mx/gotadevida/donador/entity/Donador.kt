package mx.gotadevida.donador.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Tipos de sangre con su etiqueta (signo de menos real, U+2212). Mismos nombres que la API. */
@Serializable
enum class TipoSangre(val etiqueta: String) {
    O_NEGATIVO("O−"),
    O_POSITIVO("O+"),
    A_NEGATIVO("A−"),
    A_POSITIVO("A+"),
    B_NEGATIVO("B−"),
    B_POSITIVO("B+"),
    AB_NEGATIVO("AB−"),
    AB_POSITIVO("AB+"),
}

/** Solo se usa para calcular intervalos y máximos anuales. */
@Serializable
enum class SexoParaCriteriosMedicos { FEMENINO, MASCULINO }

/** Perfil tal como lo devuelve `GET /api/v1/donadores/me`. */
@Serializable
data class PerfilDonador(
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

/** Cuerpo de `PUT /api/v1/donadores/me`. Lo que va en null no se cambia. */
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
    val notificacionesActivas: Boolean? = null,
    val radioMaximoKm: Int? = null,
)

/** Respuesta de `GET /api/v1/donadores/me/elegibilidad`. */
@Serializable
data class Elegibilidad(
    val elegible: Boolean,
    val faltanDatos: Boolean,
    val motivos: List<String>,
    val aviso: String,
    val proximaFechaElegible: String? = null,
    val diasParaVolverADonar: Long? = null,
)

/** Copia local del perfil (Room) para verlo sin conexión. */
@Entity(tableName = "perfil_donador")
data class PerfilEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val notificacionesActivas: Boolean,
    val radioMaximoKm: Int,
    val totalDonaciones: Int,
    val perfilCompleto: Boolean,
    val telefono: String?,
    val tipoSangre: String?,
    val fechaNacimiento: String?,
    val pesoKg: Double?,
    val sexo: String?,
    val latitud: Double?,
    val longitud: Double?,
    val zona: String?,
    val ultimaDonacion: String?,
    val guardadoEn: Long,
)

/** Copia local de la última elegibilidad consultada. */
@Entity(tableName = "elegibilidad")
data class ElegibilidadEntity(
    @PrimaryKey val donadorId: String,
    val elegible: Boolean,
    val faltanDatos: Boolean,
    val motivos: List<String>,
    val aviso: String,
    val proximaFechaElegible: String?,
    val diasParaVolverADonar: Long?,
    val consultadoEn: Long,
)

fun PerfilDonador.aEntidad(ahora: Long) = PerfilEntity(
    id = id,
    nombre = nombre,
    notificacionesActivas = notificacionesActivas,
    radioMaximoKm = radioMaximoKm,
    totalDonaciones = totalDonaciones,
    perfilCompleto = perfilCompleto,
    telefono = telefono,
    tipoSangre = tipoSangre?.name,
    fechaNacimiento = fechaNacimiento,
    pesoKg = pesoKg,
    sexo = sexoParaCriteriosMedicos?.name,
    latitud = latitud,
    longitud = longitud,
    zona = zona,
    ultimaDonacion = ultimaDonacion,
    guardadoEn = ahora,
)

fun PerfilEntity.aModelo(): PerfilDonador {
    val tipo = tipoSangre?.let { nombre -> TipoSangre.entries.firstOrNull { it.name == nombre } }
    return PerfilDonador(
        id = id,
        nombre = nombre,
        notificacionesActivas = notificacionesActivas,
        radioMaximoKm = radioMaximoKm,
        totalDonaciones = totalDonaciones,
        perfilCompleto = perfilCompleto,
        telefono = telefono,
        tipoSangre = tipo,
        etiquetaTipoSangre = tipo?.etiqueta,
        fechaNacimiento = fechaNacimiento,
        pesoKg = pesoKg,
        sexoParaCriteriosMedicos = sexo?.let { s -> SexoParaCriteriosMedicos.entries.firstOrNull { it.name == s } },
        latitud = latitud,
        longitud = longitud,
        zona = zona,
        ultimaDonacion = ultimaDonacion,
    )
}

fun Elegibilidad.aEntidad(donadorId: String, ahora: Long) = ElegibilidadEntity(
    donadorId = donadorId,
    elegible = elegible,
    faltanDatos = faltanDatos,
    motivos = motivos,
    aviso = aviso,
    proximaFechaElegible = proximaFechaElegible,
    diasParaVolverADonar = diasParaVolverADonar,
    consultadoEn = ahora,
)

fun ElegibilidadEntity.aModelo() = Elegibilidad(
    elegible = elegible,
    faltanDatos = faltanDatos,
    motivos = motivos,
    aviso = aviso,
    proximaFechaElegible = proximaFechaElegible,
    diasParaVolverADonar = diasParaVolverADonar,
)
