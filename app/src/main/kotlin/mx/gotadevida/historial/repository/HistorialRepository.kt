package mx.gotadevida.historial.repository

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.historial.entity.Historial
import mx.gotadevida.historial.entity.HistorialEntity
import retrofit2.http.GET
import javax.inject.Inject

interface HistorialApi {
    @GET("api/v1/donaciones/me")
    suspend fun historial(): Historial
}

@Dao
interface HistorialDao {
    @Query("SELECT * FROM historial WHERE usuarioId = :id")
    fun observar(id: String): Flow<HistorialEntity?>

    @Upsert
    suspend fun guardar(historial: HistorialEntity)
}

interface HistorialRepository {
    fun observar(): Flow<Historial?>

    suspend fun refrescar(): Resultado<Unit>
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialRepositoryLocalYRed @Inject constructor(
    private val api: HistorialApi,
    private val dao: HistorialDao,
    private val almacen: AlmacenSesion,
    private val json: Json,
) : HistorialRepository {
    override fun observar(): Flow<Historial?> = almacen.sesion.flatMapLatest { s ->
        if (s == null) flowOf(null) else dao.observar(s.usuarioId).map { it?.let { e -> json.decodeFromString(Historial.serializer(), e.json) } }
    }

    override suspend fun refrescar(): Resultado<Unit> {
        val id = almacen.actual()?.usuarioId ?: return Resultado.Fallo(FalloApp.Desconocido)
        return when (val r = llamarApi { api.historial() }) {
            is Resultado.Exito -> Resultado.Exito(dao.guardar(HistorialEntity(id, json.encodeToString(Historial.serializer(), r.valor))))
            is Resultado.Fallo -> r
        }
    }
}
