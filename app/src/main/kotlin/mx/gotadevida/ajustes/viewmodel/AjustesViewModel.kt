package mx.gotadevida.ajustes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gotadevida.ajustes.repository.AjustesRepository
import mx.gotadevida.ajustes.service.Idioma
import mx.gotadevida.ajustes.service.IdiomaApp
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.sesion.GestorSesion

data class AjustesUiState(
    val idioma: Idioma = Idioma.ESPANOL,
    /** null = todavía no se sabe (o la cuenta es de hospital). */
    val notificaciones: Boolean? = null,
    val cerrandoSesion: Boolean = false,
    val eliminando: Boolean = false,
    val fallo: FalloApp? = null
)

@HiltViewModel
class AjustesViewModel @Inject constructor(private val gestorSesion: GestorSesion, private val repositorio: AjustesRepository) :
    ViewModel() {
    private val _estado = MutableStateFlow(AjustesUiState(idioma = IdiomaApp.actual()))
    val estado: StateFlow<AjustesUiState> = _estado.asStateFlow()

    /** Solo el donador tiene avisos que apagar; la pantalla lo llama si la cuenta es de donador. */
    fun cargarPreferencias() {
        viewModelScope.launch {
            (repositorio.notificacionesActivas() as? Resultado.Exito)?.let { r -> _estado.update { it.copy(notificaciones = r.valor) } }
        }
    }

    fun cambiarIdioma(idioma: Idioma) {
        _estado.update { it.copy(idioma = idioma) }
        IdiomaApp.cambiar(idioma)
    }

    fun cambiarNotificaciones(activas: Boolean) {
        val anterior = _estado.value.notificaciones
        _estado.update { it.copy(notificaciones = activas, fallo = null) }
        viewModelScope.launch {
            val r = repositorio.cambiarNotificaciones(activas)
            if (r is Resultado.Fallo) _estado.update { it.copy(notificaciones = anterior, fallo = r.fallo) }
        }
    }

    /** Al borrarse la sesión, la navegación regresa sola a la bienvenida. */
    fun cerrarSesion() {
        _estado.update { it.copy(cerrandoSesion = true) }
        viewModelScope.launch { gestorSesion.cerrarSesion() }
    }

    /** Derecho de cancelación: borra la cuenta en el servidor y luego los datos del teléfono. */
    fun eliminarCuenta() {
        if (_estado.value.eliminando) return
        _estado.update { it.copy(eliminando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.eliminarCuenta()) {
                is Resultado.Exito -> gestorSesion.cerrarSesion()
                is Resultado.Fallo -> _estado.update { it.copy(eliminando = false, fallo = r.fallo) }
            }
        }
    }
}
