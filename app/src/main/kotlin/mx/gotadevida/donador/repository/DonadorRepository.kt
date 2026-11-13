package mx.gotadevida.donador.repository

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.ElegibilidadEntity
import mx.gotadevida.donador.entity.PerfilDonador
import mx.gotadevida.donador.entity.PerfilEntity
import mx.gotadevida.donador.entity.aEntidad
import mx.gotadevida.donador.entity.aModelo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface DonadorApi {
    @GET("api/v1/donadores/me")
    suspend fun obtenerPerfil(): PerfilDonador

    @PUT("api/v1/donadores/me")
    suspend fun actualizarPerfil(@Body cambios: ActualizarPerfilSolicitud): PerfilDonador

    @GET("api/v1/donadores/me/elegibilidad")
    suspend fun obtenerElegibilidad(): Elegibilidad
}

@Dao
interface DonadorDao {
    @Query("SELECT * FROM perfil_donador WHERE id = :id")
    fun observarPerfil(id: String): Flow<PerfilEntity?>

    @Upsert
    suspend fun guardarPerfil(perfil: PerfilEntity)

    @Query("SELECT * FROM elegibilidad WHERE donadorId = :id")
    fun observarElegibilidad(id: String): Flow<ElegibilidadEntity?>

    @Upsert
    suspend fun guardarElegibilidad(elegibilidad: ElegibilidadEntity)
}

/**
 * Fuente única de verdad del perfil: la UI siempre lee de Room y [refrescar] trae lo nuevo
 * de la API. Así la app muestra lo último guardado aunque no haya señal.
 */
interface DonadorRepository {
    fun observarPerfil(): Flow<PerfilDonador?>

    fun observarElegibilidad(): Flow<Elegibilidad?>

    suspend fun refrescar(): Resultado<Unit>

    suspend fun actualizarPerfil(cambios: ActualizarPerfilSolicitud): Resultado<PerfilDonador>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DonadorRepositoryLocalYRed @Inject constructor(
    private val api: DonadorApi,
    private val dao: DonadorDao,
    private val almacen: AlmacenSesion
) : DonadorRepository {

    private val idSesion: Flow<String?> = almacen.sesion.map { it?.usuarioId }

    override fun observarPerfil(): Flow<PerfilDonador?> = idSesion.flatMapLatest { id ->
        if (id == null) flowOf(null) else dao.observarPerfil(id).map { it?.aModelo() }
    }

    override fun observarElegibilidad(): Flow<Elegibilidad?> = idSesion.flatMapLatest { id ->
        if (id == null) flowOf(null) else dao.observarElegibilidad(id).map { it?.aModelo() }
    }

    override suspend fun refrescar(): Resultado<Unit> {
        val id = almacen.actual()?.usuarioId ?: return Resultado.Fallo(FalloApp.Desconocido)
        val perfil = llamarApi { api.obtenerPerfil() }
        if (perfil is Resultado.Fallo) return perfil
        dao.guardarPerfil((perfil as Resultado.Exito).valor.aEntidad(System.currentTimeMillis()))
        return when (val elegibilidad = llamarApi { api.obtenerElegibilidad() }) {
            is Resultado.Fallo -> elegibilidad

            is Resultado.Exito -> {
                dao.guardarElegibilidad(elegibilidad.valor.aEntidad(id, System.currentTimeMillis()))
                Resultado.Exito(Unit)
            }
        }
    }

    override suspend fun actualizarPerfil(cambios: ActualizarPerfilSolicitud): Resultado<PerfilDonador> {
        val resultado = llamarApi { api.actualizarPerfil(cambios) }
        if (resultado is Resultado.Exito) {
            dao.guardarPerfil(resultado.valor.aEntidad(System.currentTimeMillis()))
            // La elegibilidad depende del perfil: se vuelve a consultar.
            val id = resultado.valor.id
            (llamarApi { api.obtenerElegibilidad() } as? Resultado.Exito)?.let {
                dao.guardarElegibilidad(it.valor.aEntidad(id, System.currentTimeMillis()))
            }
        }
        return resultado
    }
}
