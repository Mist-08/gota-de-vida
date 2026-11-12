package mx.gotadevida.prueba

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import mx.gotadevida.core.sync.AccionPendiente
import mx.gotadevida.core.sync.AccionPendienteDao
import mx.gotadevida.core.sync.ColaDeSalida
import mx.gotadevida.hospital.entity.CrearUrgenciaPeticion
import mx.gotadevida.hospital.entity.EstadoUrgenciaHospital
import mx.gotadevida.hospital.entity.UrgenciaHospital
import mx.gotadevida.hospital.repository.HospitalRepository
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.solicitud.entity.EstadoUrgencia
import mx.gotadevida.solicitud.entity.ResponderPeticion
import mx.gotadevida.solicitud.entity.Urgencia
import mx.gotadevida.solicitud.entity.UrgenciaEntity
import mx.gotadevida.solicitud.repository.SolicitudApi
import mx.gotadevida.solicitud.repository.SolicitudDao
import java.io.IOException

fun urgenciaDePrueba(id: String = "u1", comprometidas: Int = 0, miRespuesta: Boolean? = null) = Urgencia(
    id = id,
    hospitalId = "h1",
    hospitalNombre = "Hospital Universitario",
    hospitalDireccion = "Av. Morelos 900, Torreón",
    latitud = 25.5428,
    longitud = -103.4068,
    etiquetaTipoSangre = "O−",
    unidades = 3,
    unidadesComprometidas = comprometidas,
    estado = EstadoUrgencia.ACTIVA,
    creadaEn = "2026-11-12T10:00:00Z",
    distanciaKm = 3.2,
    miRespuesta = miRespuesta,
)

/** API falsa: con `sinRed = true` lanza IOException, como un teléfono en modo avión. */
class SolicitudApiFalsa(var sinRed: Boolean = false) : SolicitudApi {
    val respuestas = mutableListOf<Pair<String, Boolean>>()
    var lista = listOf(urgenciaDePrueba())

    override suspend fun cercanas(): List<Urgencia> {
        if (sinRed) throw IOException("sin red")
        return lista
    }

    override suspend fun detalle(id: String): Urgencia {
        if (sinRed) throw IOException("sin red")
        return lista.first { it.id == id }
    }

    override suspend fun responder(id: String, peticion: ResponderPeticion): Urgencia {
        if (sinRed) throw IOException("sin red")
        respuestas += id to peticion.acepta
        return lista.first { it.id == id }.let {
            it.copy(unidadesComprometidas = it.unidadesComprometidas + if (peticion.acepta) 1 else 0, miRespuesta = peticion.acepta)
        }
    }
}

class SolicitudDaoFalso : SolicitudDao {
    val datos = MutableStateFlow<Map<String, UrgenciaEntity>>(emptyMap())

    override fun observarCercanas(): Flow<List<UrgenciaEntity>> =
        datos.map { m -> m.values.filter { it.cercana && it.estado == "ACTIVA" }.sortedBy { it.distanciaKm } }

    override fun observar(id: String): Flow<UrgenciaEntity?> = datos.map { it[id] }

    override suspend fun buscar(id: String) = datos.value[id]

    override suspend fun guardar(urgencias: List<UrgenciaEntity>) {
        datos.value = datos.value + urgencias.associateBy { it.id }
    }

    override suspend fun desmarcarCercanas() {
        datos.value = datos.value.mapValues { it.value.copy(cercana = false) }
    }
}

class ColaFalsa : ColaDeSalida {
    val encoladas = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    override val claves: Flow<List<String>> = encoladas.map { it.keys.toList() }

    override suspend fun encolar(clave: String, tipo: String, datos: String) {
        encoladas.value = encoladas.value + (clave to (tipo to datos))
    }
}

class AccionesDaoFalso : AccionPendienteDao {
    val datos = linkedMapOf<String, AccionPendiente>()
    override suspend fun guardar(accion: AccionPendiente) {
        datos[accion.clave] = accion
    }

    override suspend fun todas() = datos.values.sortedBy { it.creadaEn }

    override fun observarClaves(): Flow<List<String>> = MutableStateFlow(datos.keys.toList())

    override suspend fun borrar(clave: String) {
        datos.remove(clave)
    }

    override suspend fun sumarIntento(clave: String) {
        datos[clave]?.let { datos[clave] = it.copy(intentos = it.intentos + 1) }
    }
}

fun urgenciaHospital(id: String = "u1", estado: EstadoUrgenciaHospital = EstadoUrgenciaHospital.ACTIVA) = UrgenciaHospital(
    id = id, hospitalNombre = "Hospital Universitario", etiquetaTipoSangre = "O−", unidades = 3,
    unidadesComprometidas = 1, estado = estado, creadaEn = "2026-11-12T10:00:00Z", oleada = 1, donadoresNotificados = 12,
)

class HospitalFalso(var respuesta: Resultado<UrgenciaHospital> = Resultado.Exito(urgenciaHospital())) : HospitalRepository {
    val publicadas = mutableListOf<CrearUrgenciaPeticion>()
    override suspend fun misUrgencias() = Resultado.Exito(listOf(urgenciaHospital()))

    override suspend fun publicar(peticion: CrearUrgenciaPeticion): Resultado<UrgenciaHospital> {
        publicadas += peticion
        return respuesta
    }

    override suspend fun detalle(id: String) = respuesta

    override suspend fun cambiarEstado(id: String, estado: EstadoUrgenciaHospital) =
        (respuesta as? Resultado.Exito)?.let { Resultado.Exito(it.valor.copy(estado = estado)) } ?: respuesta
}
