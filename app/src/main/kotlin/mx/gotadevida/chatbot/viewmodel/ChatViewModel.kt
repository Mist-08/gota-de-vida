package mx.gotadevida.chatbot.viewmodel

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
import kotlinx.serialization.Serializable
import mx.gotadevida.chatbot.entity.MensajeChat
import mx.gotadevida.chatbot.repository.ChatRepository
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import javax.inject.Inject

@Serializable
data object Chat

data class ChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val borrador: String = "",
    val opciones: List<String> = listOf("1", "2", "3", "4"),
    val enviando: Boolean = false,
    /** Lo último que se intentó mandar sin conexión, para reintentarlo con un toque. */
    val sinEnviar: String? = null,
    val fallo: FalloApp? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(private val repositorio: ChatRepository) : ViewModel() {
    private val local = MutableStateFlow(ChatUiState())

    val estado: StateFlow<ChatUiState> = combine(repositorio.observarMensajes(), local) { mensajes, l -> l.copy(mensajes = mensajes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    init {
        viewModelScope.launch { repositorio.refrescar() }
    }

    fun onBorrador(texto: String) = local.update { it.copy(borrador = texto.take(500)) }

    fun enviar(texto: String = local.value.borrador) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || local.value.enviando) return
        local.update { it.copy(enviando = true, borrador = "", fallo = null, sinEnviar = null) }
        viewModelScope.launch {
            when (val r = repositorio.enviar(limpio)) {
                is Resultado.Exito -> local.update { it.copy(enviando = false, opciones = r.valor.opciones) }
                is Resultado.Fallo -> local.update { it.copy(enviando = false, fallo = r.fallo, sinEnviar = limpio) }
            }
        }
    }
}
