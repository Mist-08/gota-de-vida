package mx.gotadevida.solicitud.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.solicitud.entity.Urgencia
import mx.gotadevida.solicitud.repository.EnvioRespuesta
import mx.gotadevida.solicitud.repository.SolicitudRepository
import javax.inject.Inject

data class UrgenciasUiState(
    val urgencias: List<Urgencia> = emptyList(),
    val pendientes: Set<String> = emptySet(),
    val cargando: Boolean = true,
)

/** Lista "Urgencias cerca" del inicio del donador. */
@HiltViewModel
class UrgenciasViewModel @Inject constructor(
    private val repositorio: SolicitudRepository,
) : ViewModel() {
    private val cargando = MutableStateFlow(true)

    val estado: StateFlow<UrgenciasUiState> = combine(
        repositorio.observarCercanas(),
        repositorio.observarPendientes(),
        cargando,
    ) { urgencias, pendientes, c -> UrgenciasUiState(urgencias, pendientes, c && urgencias.isEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UrgenciasUiState())

    init {
        actualizar()
    }

    fun actualizar() {
        viewModelScope.launch {
            cargando.value = true
            repositorio.refrescarCercanas()
            cargando.value = false
        }
    }
}

/** Ruta de navegación con tipo: el detalle recibe el id de la urgencia. */
@kotlinx.serialization.Serializable
data class DetalleUrgencia(val id: String)

data class DetalleUrgenciaUiState(
    val urgencia: Urgencia? = null,
    val pendiente: Boolean = false,
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    /** true justo después de aceptar: la pantalla muestra el agradecimiento. */
    val acepto: Boolean = false,
)

/** Detalle de una urgencia: mapa, distancia, contador "2 de 3" y botones para responder. */
@HiltViewModel
class DetalleUrgenciaViewModel @Inject constructor(
    private val repositorio: SolicitudRepository,
    estadoGuardado: SavedStateHandle,
) : ViewModel() {
    private val id: String = checkNotNull(estadoGuardado["id"])
    private val local = MutableStateFlow(DetalleUrgenciaUiState())

    val estado: StateFlow<DetalleUrgenciaUiState> = combine(
        repositorio.observar(id),
        repositorio.observarPendientes(),
        local,
    ) { urgencia, pendientes, l -> l.copy(urgencia = urgencia, pendiente = id in pendientes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUrgenciaUiState())

    /** La pantalla lo llama cada 10 s mientras está abierta: así el contador se ve "en vivo". */
    fun actualizar() {
        viewModelScope.launch {
            val r = repositorio.refrescar(id)
            if (r is Resultado.Fallo && r.fallo != FalloApp.SinConexion) local.update { it.copy(fallo = r.fallo) }
        }
    }

    fun responder(acepta: Boolean) {
        if (local.value.enviando) return
        local.update { it.copy(enviando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.responder(id, acepta)) {
                is Resultado.Exito -> local.update {
                    it.copy(enviando = false, acepto = acepta && r.valor == EnvioRespuesta.ENVIADA)
                }
                is Resultado.Fallo -> local.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }
}
