package mx.gotadevida.donador.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.donador.entity.Donador
import com.mongodb.client.model.Aggregates
import org.bson.Document
import com.mongodb.client.model.GeoNearOptions
import com.mongodb.client.model.Projections
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import kotlinx.coroutines.flow.toList
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre

interface DonadorRepository {
    suspend fun buscarPorId(id: String): Donador?

    /** Crea o reemplaza el perfil completo. */
    suspend fun guardar(donador: Donador)

    /**
     * Donadores cercanos con uno de los `tipos` y notificaciones activas, del más cercano
     * al más lejano (`$geoNear`, usa el índice 2dsphere). La elegibilidad se revisa después.
     */
    suspend fun cercanos(centro: PuntoGeo, radioKm: Double, tipos: List<TipoSangre>, excluir: Set<String>, limite: Int): List<Donador>

    suspend fun buscarPorTelefono(telefono: String): Donador?
}

class MongoDonadorRepository(db: MongoDatabase) : DonadorRepository {
    private val coleccion = db.getCollection<Donador>(Colecciones.DONADORES)

    override suspend fun buscarPorId(id: String): Donador? =
        coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun guardar(donador: Donador) {
        coleccion.replaceOne(Filters.eq("_id", donador.id), donador, ReplaceOptions().upsert(true))
    }

    override suspend fun cercanos(
        centro: PuntoGeo,
        radioKm: Double,
        tipos: List<TipoSangre>,
        excluir: Set<String>,
        limite: Int,
    ): List<Donador> = buscarCercanos(centro, radioKm, tipos, excluir, limite)

    override suspend fun buscarPorTelefono(telefono: String): Donador? =
        coleccion.find(Filters.eq("telefono", telefono)).firstOrNull()

    private suspend fun buscarCercanos(
        centro: PuntoGeo,
        radioKm: Double,
        tipos: List<TipoSangre>,
        excluir: Set<String>,
        limite: Int,
    ): List<Donador> = coleccion.aggregate<Donador>(
        listOf(
            // $geoNear tiene que ser la primera etapa; calcula la distancia en metros.
            Aggregates.geoNear(
                Point(Position(centro.longitud, centro.latitud)),
                "distanciaM",
                GeoNearOptions.geoNearOptions()
                    .maxDistance(radioKm * 1000)
                    .query(
                        Document("tipoSangre", Document("\$in", tipos.map { it.name }))
                            .append("notificacionesActivas", true)
                            .append("_id", Document("\$nin", excluir.toList())),
                    ),
            ),
            Aggregates.limit(limite),
            // La distancia exacta no se guarda ni se regresa: se recalcula redondeada.
            Aggregates.project(Projections.exclude("distanciaM")),
        ),
    ).toList()
}
