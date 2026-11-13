package mx.gotadevida.notificaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import mx.gotadevida.notificaciones.repository.DispositivoRepository

@HiltViewModel
class NotificacionesViewModel @Inject constructor(private val dispositivos: DispositivoRepository) : ViewModel() {
    fun registrar() {
        viewModelScope.launch { runCatching { dispositivos.registrarTokenActual() } }
    }
}
