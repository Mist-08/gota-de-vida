package mx.gotadevida.solicitud.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.solicitud.entity.EstadoSolicitud
import mx.gotadevida.solicitud.entity.Respuesta
import mx.gotadevida.solicitud.entity.Solicitud

interface SolicitudRepository {
    suspend fun guardar(solicitud: Solicitud)

    suspend fun buscarPorId(id: String): Solicitud?

    /** Urgencias activas para pacientes de `tipos`, dentro de `radioKm` de `centro`. */
    suspend fun activasCerca(centro: PuntoGeo, radioKm: Double, tipos: List<TipoSangre>): List<Solicitud>

    suspend fun delHospital(hospitalId: String): List<Solicitud>
}

interface RespuestaRepository {
    suspend fun guardar(respuesta: Respuesta)

    suspend fun buscar(solicitudId: String, donadorId: String): Respuesta?

    suspend fun deSolicitud(solicitudId: String): List<Respuesta>

    suspend fun deDonador(donadorId: String): List<Respuesta>
}

class MongoSolicitudRepository(db: MongoDatabase) : SolicitudRepository {
    private val coleccion = db.getCollection<Solicitud>(Colecciones.SOLICITUDES)

    override suspend fun guardar(solicitud: Solicitud) {
        coleccion.replaceOne(Filters.eq("_id", solicitud.id), solicitud, ReplaceOptions().upsert(true))
    }

    override suspend fun buscarPorId(id: String): Solicitud? = coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun activasCerca(centro: PuntoGeo, radioKm: Double, tipos: List<TipoSangre>): List<Solicitud> = coleccion.find(
        Filters.and(
            Filters.eq("estado", EstadoSolicitud.ACTIVA.name),
            Filters.`in`("tipoSangre", tipos.map { it.name }),
            // $centerSphere usa radianes: distancia / radio de la Tierra.
            Filters.geoWithinCenterSphere("ubicacion", centro.longitud, centro.latitud, radioKm / RADIO_TIERRA_KM)
        )
    ).toList()

    override suspend fun delHospital(hospitalId: String): List<Solicitud> =
        coleccion.find(Filters.eq("hospitalId", hospitalId)).sort(Sorts.descending("creadaEn")).limit(50).toList()

    companion object {
        const val RADIO_TIERRA_KM = 6378.1
    }
}

class MongoRespuestaRepository(db: MongoDatabase) : RespuestaRepository {
    private val coleccion = db.getCollection<Respuesta>(Colecciones.RESPUESTAS)

    override suspend fun guardar(respuesta: Respuesta) {
        coleccion.replaceOne(Filters.eq("_id", respuesta.id), respuesta, ReplaceOptions().upsert(true))
    }

    override suspend fun buscar(solicitudId: String, donadorId: String): Respuesta? =
        coleccion.find(Filters.eq("_id", "$solicitudId:$donadorId")).firstOrNull()

    override suspend fun deSolicitud(solicitudId: String): List<Respuesta> =
        coleccion.find(Filters.eq("solicitudId", solicitudId)).sort(Sorts.ascending("creadaEn")).toList()

    override suspend fun deDonador(donadorId: String): List<Respuesta> = coleccion.find(Filters.eq("donadorId", donadorId)).toList()
}
