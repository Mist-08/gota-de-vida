package mx.gotadevida.hospital.repository

import javax.inject.Inject
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.hospital.entity.CambiarEstadoPeticion
import mx.gotadevida.hospital.entity.CheckinPeticion
import mx.gotadevida.hospital.entity.CitaDelDia
import mx.gotadevida.hospital.entity.CrearUrgenciaPeticion
import mx.gotadevida.hospital.entity.Estadisticas
import mx.gotadevida.hospital.entity.EstadoUrgenciaHospital
import mx.gotadevida.hospital.entity.UrgenciaHospital
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface HospitalApi {
    @GET("api/v1/hospitales/me/solicitudes")
    suspend fun misUrgencias(): List<UrgenciaHospital>

    @POST("api/v1/solicitudes")
    suspend fun publicar(@Body peticion: CrearUrgenciaPeticion): UrgenciaHospital

    @GET("api/v1/solicitudes/{id}")
    suspend fun detalle(@Path("id") id: String): UrgenciaHospital

    @GET("api/v1/hospitales/me/estadisticas")
    suspend fun estadisticas(): Estadisticas

    @GET("api/v1/hospitales/me/citas")
    suspend fun citasDeHoy(): List<CitaDelDia>

    @POST("api/v1/citas/checkin-qr")
    suspend fun checkin(@Body peticion: CheckinPeticion): CitaDelDia

    @PATCH("api/v1/solicitudes/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: String, @Body peticion: CambiarEstadoPeticion): UrgenciaHospital
}

/**
 * Panel del banco de sangre. Siempre en línea: el hospital necesita ver el dato real,
 * por eso aquí no hay copia en Room.
 */
interface HospitalRepository {
    suspend fun misUrgencias(): Resultado<List<UrgenciaHospital>>

    suspend fun publicar(peticion: CrearUrgenciaPeticion): Resultado<UrgenciaHospital>

    suspend fun detalle(id: String): Resultado<UrgenciaHospital>

    suspend fun cambiarEstado(id: String, estado: EstadoUrgenciaHospital): Resultado<UrgenciaHospital>

    suspend fun estadisticas(): Resultado<Estadisticas>

    suspend fun citasDeHoy(): Resultado<List<CitaDelDia>>

    /** Confirma la donación con el contenido del QR (`gota:<cita>:<código>`). */
    suspend fun checkin(contenidoQr: String): Resultado<CitaDelDia>
}

class HospitalRepositoryRed @Inject constructor(private val api: HospitalApi) : HospitalRepository {
    override suspend fun misUrgencias() = llamarApi { api.misUrgencias() }

    override suspend fun publicar(peticion: CrearUrgenciaPeticion) = llamarApi { api.publicar(peticion) }

    override suspend fun detalle(id: String) = llamarApi { api.detalle(id) }

    override suspend fun cambiarEstado(id: String, estado: EstadoUrgenciaHospital) =
        llamarApi { api.cambiarEstado(id, CambiarEstadoPeticion(estado)) }

    override suspend fun estadisticas() = llamarApi { api.estadisticas() }

    override suspend fun citasDeHoy() = llamarApi { api.citasDeHoy() }

    override suspend fun checkin(contenidoQr: String) = llamarApi { api.checkin(CheckinPeticion(contenidoQr.trim())) }
}
