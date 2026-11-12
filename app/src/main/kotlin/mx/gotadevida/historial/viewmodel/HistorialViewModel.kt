package mx.gotadevida.historial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.historial.entity.Historial
import mx.gotadevida.historial.repository.HistorialRepository
import javax.inject.Inject

@Serializable
data object HistorialRuta

data class HistorialUiState(
    val historial: Historial? = null,
    val cargando: Boolean = true,
    val sinConexion: Boolean = false,
)

@HiltViewModel
class HistorialViewModel @Inject constructor(private val repositorio: HistorialRepository) : ViewModel() {
    private val red = MutableStateFlow(Pair(true, false))

    val estado: StateFlow<HistorialUiState> = combine(repositorio.observar(), red) { h, (cargando, sinConexion) ->
        HistorialUiState(h, cargando && h == null, sinConexion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    init {
        actualizar()
    }

    fun actualizar() {
        viewModelScope.launch {
            val r = repositorio.refrescar()
            red.value = false to (r is Resultado.Fallo && r.fallo == FalloApp.SinConexion)
        }
    }
}
