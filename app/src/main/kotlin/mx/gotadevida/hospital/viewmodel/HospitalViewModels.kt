package mx.gotadevida.hospital.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.hospital.entity.CitaDelDia
import mx.gotadevida.hospital.entity.CrearUrgenciaPeticion
import mx.gotadevida.hospital.entity.Estadisticas
import mx.gotadevida.hospital.entity.EstadoUrgenciaHospital
import mx.gotadevida.hospital.entity.TipoSangrePaciente
import mx.gotadevida.hospital.entity.UrgenciaHospital
import mx.gotadevida.hospital.repository.HospitalRepository

data class PanelUiState(
    val urgencias: List<UrgenciaHospital> = emptyList(),
    val estadisticas: Estadisticas? = null,
    val citasHoy: List<CitaDelDia> = emptyList(),
    val cargando: Boolean = true,
    val fallo: FalloApp? = null
)

@HiltViewModel
class PanelHospitalViewModel @Inject constructor(private val repositorio: HospitalRepository) : ViewModel() {
    private val _estado = MutableStateFlow(PanelUiState())
    val estado: StateFlow<PanelUiState> = _estado.asStateFlow()

    /** La pantalla lo llama al abrir y cada 15 s: así las respuestas se ven en vivo. */
    fun actualizar() {
        viewModelScope.launch {
            when (val r = repositorio.misUrgencias()) {
                is Resultado.Exito -> _estado.update { it.copy(urgencias = r.valor, cargando = false, fallo = null) }
                is Resultado.Fallo -> _estado.update { it.copy(cargando = false, fallo = r.fallo) }
            }
            (repositorio.estadisticas() as? Resultado.Exito)?.let { r ->
                _estado.update { it.copy(estadisticas = r.valor) }
            }
            (repositorio.citasDeHoy() as? Resultado.Exito)?.let { r -> _estado.update { it.copy(citasHoy = r.valor) } }
        }
    }
}

data class CrearUrgenciaUiState(
    val tipo: TipoSangrePaciente? = null,
    val unidades: Int = 1,
    val nota: String = "",
    val faltaTipo: Boolean = false,
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    val creadaId: String? = null
)

@HiltViewModel
class CrearUrgenciaViewModel @Inject constructor(private val repositorio: HospitalRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CrearUrgenciaUiState())
    val estado: StateFlow<CrearUrgenciaUiState> = _estado.asStateFlow()

    fun onTipo(tipo: TipoSangrePaciente) = _estado.update { it.copy(tipo = tipo, faltaTipo = false) }

    fun onUnidades(unidades: Int) = _estado.update { it.copy(unidades = unidades.coerceIn(1, MAXIMO_UNIDADES)) }

    fun onNota(nota: String) = _estado.update { it.copy(nota = nota.take(200)) }

    fun publicar() {
        val actual = _estado.value
        if (actual.enviando) return
        val tipo = actual.tipo ?: return _estado.update { it.copy(faltaTipo = true) }
        _estado.update { it.copy(enviando = true, fallo = null) }
        viewModelScope.launch {
            when (
                val r = repositorio.publicar(
                    CrearUrgenciaPeticion(
                        tipo,
                        actual.unidades,
                        actual.nota.trim().ifEmpty {
                            null
                        }
                    )
                )
            ) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, creadaId = r.valor.id) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }

    companion object {
        const val MAXIMO_UNIDADES = 20
    }
}

@Serializable
data class DetalleUrgenciaHospital(val id: String)

data class DetalleHospitalUiState(val urgencia: UrgenciaHospital? = null, val fallo: FalloApp? = null, val cambiando: Boolean = false)

@HiltViewModel
class DetalleUrgenciaHospitalViewModel @Inject constructor(private val repositorio: HospitalRepository, estadoGuardado: SavedStateHandle) :
    ViewModel() {
    private val id: String = checkNotNull(estadoGuardado["id"])
    private val _estado = MutableStateFlow(DetalleHospitalUiState())
    val estado: StateFlow<DetalleHospitalUiState> = _estado.asStateFlow()

    fun actualizar() {
        viewModelScope.launch {
            when (val r = repositorio.detalle(id)) {
                is Resultado.Exito -> _estado.update { it.copy(urgencia = r.valor, fallo = null) }
                is Resultado.Fallo -> _estado.update { it.copy(fallo = r.fallo) }
            }
        }
    }

    fun terminar(estado: EstadoUrgenciaHospital) {
        if (_estado.value.cambiando) return
        _estado.update { it.copy(cambiando = true) }
        viewModelScope.launch {
            when (val r = repositorio.cambiarEstado(id, estado)) {
                is Resultado.Exito -> _estado.update { it.copy(urgencia = r.valor, cambiando = false, fallo = null) }
                is Resultado.Fallo -> _estado.update { it.copy(cambiando = false, fallo = r.fallo) }
            }
        }
    }
}

@Serializable
data object EscanearQr

data class EscanearUiState(
    val codigoManual: String = "",
    val enviando: Boolean = false,
    val confirmada: CitaDelDia? = null,
    val fallo: FalloApp? = null
)

/** Check-in: la cámara lee el QR (o se escribe el código) y se confirma la donación. */
@HiltViewModel
class EscanearViewModel @Inject constructor(private val repositorio: HospitalRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EscanearUiState())
    val estado: StateFlow<EscanearUiState> = _estado.asStateFlow()

    fun onCodigo(texto: String) = _estado.update { it.copy(codigoManual = texto.trim(), fallo = null) }

    fun alLeer(contenido: String) {
        val actual = _estado.value
        if (actual.enviando || actual.confirmada != null) return
        _estado.update { it.copy(enviando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.checkin(contenido)) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, confirmada = r.valor) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }

    fun confirmarManual() = alLeer(_estado.value.codigoManual)

    fun otra() = _estado.update { EscanearUiState() }
}
