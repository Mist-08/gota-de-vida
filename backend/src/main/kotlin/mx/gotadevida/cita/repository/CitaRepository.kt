package mx.gotadevida.cita.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.core.db.Colecciones

interface CitaRepository {
    suspend fun guardar(cita: Cita)

    suspend fun buscarPorId(id: String): Cita?

    suspend fun deDonador(donadorId: String): List<Cita>

    /** Citas agendadas de un hospital cuyo `fechaHora` empieza con `prefijo` (un día o un horario). */
    suspend fun agendadasDeHospital(hospitalId: String, prefijo: String): List<Cita>

    suspend fun contarCompletadas(hospitalId: String): Int
}

class MongoCitaRepository(db: MongoDatabase) : CitaRepository {
    private val coleccion = db.getCollection<Cita>(Colecciones.CITAS)

    override suspend fun guardar(cita: Cita) {
        coleccion.replaceOne(Filters.eq("_id", cita.id), cita, ReplaceOptions().upsert(true))
    }

    override suspend fun buscarPorId(id: String): Cita? = coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun deDonador(donadorId: String): List<Cita> =
        coleccion.find(Filters.eq("donadorId", donadorId)).sort(Sorts.descending("fechaHora")).limit(50).toList()

    override suspend fun agendadasDeHospital(hospitalId: String, prefijo: String): List<Cita> =
        coleccion.find(
            Filters.and(
                Filters.eq("hospitalId", hospitalId),
                Filters.eq("estado", EstadoCita.AGENDADA.name),
                Filters.regex("fechaHora", "^" + prefijo.filter { it.isDigit() || it in "-T:" }),
            ),
        ).sort(Sorts.ascending("fechaHora")).toList()

    override suspend fun contarCompletadas(hospitalId: String): Int =
        coleccion.countDocuments(Filters.and(Filters.eq("hospitalId", hospitalId), Filters.eq("estado", EstadoCita.COMPLETADA.name))).toInt()
}
