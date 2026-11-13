package mx.gotadevida.autenticacion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo
import mx.gotadevida.autenticacion.repository.AutenticacionRepository
import mx.gotadevida.autenticacion.service.ValidadorCuenta
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado

/** Primero se pide el correo; después, el código que llegó y la contraseña nueva. */
enum class PasoRecuperacion { CORREO, CODIGO }

data class RecuperarUiState(
    val paso: PasoRecuperacion = PasoRecuperacion.CORREO,
    val email: String = "",
    val codigo: String = "",
    val password: String = "",
    val errores: Map<CampoCuenta, ErrorCampo> = emptyMap(),
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    val codigoReenviado: Boolean = false,
    val listo: Boolean = false
)

@HiltViewModel
class RecuperarContrasenaViewModel @Inject constructor(private val repositorio: AutenticacionRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RecuperarUiState())
    val estado: StateFlow<RecuperarUiState> = _estado.asStateFlow()

    /** Trae el correo que ya estaba escrito en "Iniciar sesión" (solo la primera vez). */
    fun usarCorreoInicial(email: String) {
        if (_estado.value.email.isEmpty() && email.isNotBlank()) _estado.update { it.copy(email = email.trim()) }
    }

    fun onEmail(valor: String) = cambiar(CampoCuenta.EMAIL) { copy(email = valor) }

    fun onCodigo(valor: String) = cambiar(CampoCuenta.CODIGO) { copy(codigo = valor.filter { it.isDigit() }.take(6)) }

    fun onPassword(valor: String) = cambiar(CampoCuenta.PASSWORD) { copy(password = valor) }

    fun enviarCodigo() {
        val actual = _estado.value
        if (actual.enviando) return
        ValidadorCuenta.email(actual.email)?.let { error ->
            _estado.update { it.copy(errores = mapOf(CampoCuenta.EMAIL to error)) }
            return
        }
        val reenvio = actual.paso == PasoRecuperacion.CODIGO
        _estado.update { it.copy(enviando = true, fallo = null, codigoReenviado = false) }
        viewModelScope.launch {
            when (val r = repositorio.solicitarRecuperacion(actual.email.trim().lowercase())) {
                is Resultado.Exito -> _estado.update {
                    it.copy(enviando = false, paso = PasoRecuperacion.CODIGO, codigoReenviado = reenvio)
                }

                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }

    fun cambiarContrasena() {
        val actual = _estado.value
        if (actual.enviando) return
        val errores = buildMap {
            ValidadorCuenta.codigo(actual.codigo)?.let { put(CampoCuenta.CODIGO, it) }
            ValidadorCuenta.password(actual.password)?.let { put(CampoCuenta.PASSWORD, it) }
        }
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        _estado.update { it.copy(enviando = true, fallo = null, codigoReenviado = false) }
        viewModelScope.launch {
            val r = repositorio.restablecerContrasena(actual.email.trim().lowercase(), actual.codigo, actual.password)
            when (r) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, listo = true) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }

    /** Regresa a escribir el correo (por si se equivocó). */
    fun cambiarCorreo() = _estado.update {
        it.copy(paso = PasoRecuperacion.CORREO, codigo = "", password = "", errores = emptyMap(), fallo = null, codigoReenviado = false)
    }

    private fun cambiar(campo: CampoCuenta, cambio: RecuperarUiState.() -> RecuperarUiState) =
        _estado.update { it.cambio().copy(errores = it.errores - campo, fallo = null) }
}
