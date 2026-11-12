package mx.gotadevida.ajustes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gotadevida.ajustes.service.Idioma
import mx.gotadevida.ajustes.service.IdiomaApp
import mx.gotadevida.core.sesion.GestorSesion
import javax.inject.Inject

data class AjustesUiState(
    val idioma: Idioma = Idioma.ESPANOL,
    val cerrandoSesion: Boolean = false,
)

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val gestorSesion: GestorSesion,
) : ViewModel() {

    private val _estado = MutableStateFlow(AjustesUiState(idioma = IdiomaApp.actual()))
    val estado: StateFlow<AjustesUiState> = _estado.asStateFlow()

    fun cambiarIdioma(idioma: Idioma) {
        _estado.update { it.copy(idioma = idioma) }
        IdiomaApp.cambiar(idioma)
    }

    /** Al borrarse la sesión, la navegación regresa sola a la bienvenida. */
    fun cerrarSesion() {
        _estado.update { it.copy(cerrandoSesion = true) }
        viewModelScope.launch { gestorSesion.cerrarSesion() }
    }
}
