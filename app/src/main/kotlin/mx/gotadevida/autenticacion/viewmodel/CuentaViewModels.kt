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
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioPublico
import mx.gotadevida.autenticacion.repository.AutenticacionRepository
import mx.gotadevida.autenticacion.service.ValidadorCuenta
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado

data class RegistroUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val telefono: String = "",
    val errores: Map<CampoCuenta, ErrorCampo> = emptyMap(),
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    val googleNoDisponible: Boolean = false,
    val listo: Boolean = false
)

@HiltViewModel
class RegistroViewModel @Inject constructor(private val repositorio: AutenticacionRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun onNombre(valor: String) = cambiar(CampoCuenta.NOMBRE) { copy(nombre = valor) }

    fun onEmail(valor: String) = cambiar(CampoCuenta.EMAIL) { copy(email = valor) }

    fun onPassword(valor: String) = cambiar(CampoCuenta.PASSWORD) { copy(password = valor) }

    fun onTelefono(valor: String) = cambiar(CampoCuenta.TELEFONO) { copy(telefono = valor) }

    fun registrar(aceptoAviso: Boolean) {
        val actual = _estado.value
        if (actual.enviando) return
        val errores = ValidadorCuenta.registro(actual.nombre, actual.email, actual.password, actual.telefono)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        enviar {
            repositorio.registrar(
                RegistroSolicitud(
                    nombre = actual.nombre.trim(),
                    email = actual.email.trim().lowercase(),
                    password = actual.password,
                    telefono = actual.telefono,
                    aceptaAvisoPrivacidad = aceptoAviso
                )
            )
        }
    }

    fun conGoogle(idToken: String) {
        if (_estado.value.enviando) return
        enviar { repositorio.iniciarConGoogle(idToken) }
    }

    fun googleNoDisponible() = _estado.update { it.copy(googleNoDisponible = true) }

    private fun enviar(llamada: suspend () -> Resultado<UsuarioPublico>) {
        _estado.update { it.copy(enviando = true, fallo = null, googleNoDisponible = false) }
        viewModelScope.launch {
            when (val r = llamada()) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, listo = true) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }

    private fun cambiar(campo: CampoCuenta, cambio: RegistroUiState.() -> RegistroUiState) =
        _estado.update { it.cambio().copy(errores = it.errores - campo, fallo = null) }
}

data class InicioSesionUiState(
    val email: String = "",
    val password: String = "",
    val errores: Map<CampoCuenta, ErrorCampo> = emptyMap(),
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    val googleNoDisponible: Boolean = false,
    val listo: Boolean = false
)

@HiltViewModel
class InicioSesionViewModel @Inject constructor(private val repositorio: AutenticacionRepository) : ViewModel() {

    private val _estado = MutableStateFlow(InicioSesionUiState())
    val estado: StateFlow<InicioSesionUiState> = _estado.asStateFlow()

    fun onEmail(valor: String) = _estado.update { it.copy(email = valor, errores = it.errores - CampoCuenta.EMAIL, fallo = null) }

    fun onPassword(valor: String) = _estado.update { it.copy(password = valor, errores = it.errores - CampoCuenta.PASSWORD, fallo = null) }

    fun entrar() {
        val actual = _estado.value
        if (actual.enviando) return
        val errores = ValidadorCuenta.inicioSesion(actual.email, actual.password)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        enviar { repositorio.iniciarSesion(actual.email.trim().lowercase(), actual.password) }
    }

    fun conGoogle(idToken: String) {
        if (_estado.value.enviando) return
        enviar { repositorio.iniciarConGoogle(idToken) }
    }

    fun googleNoDisponible() = _estado.update { it.copy(googleNoDisponible = true) }

    private fun enviar(llamada: suspend () -> Resultado<UsuarioPublico>) {
        _estado.update { it.copy(enviando = true, fallo = null, googleNoDisponible = false) }
        viewModelScope.launch {
            when (val r = llamada()) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, listo = true) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }
}
