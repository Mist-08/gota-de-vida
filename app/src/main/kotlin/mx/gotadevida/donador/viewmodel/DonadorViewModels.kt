package mx.gotadevida.donador.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.tiempo.Calendario
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.PerfilDonador
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.entity.TipoSangre
import mx.gotadevida.donador.repository.DonadorRepository
import mx.gotadevida.donador.service.CalculadorEstado
import mx.gotadevida.donador.service.CampoPerfil
import mx.gotadevida.donador.service.DatosPerfil
import mx.gotadevida.donador.service.ErrorPerfil
import mx.gotadevida.donador.service.EstadoDonacion
import mx.gotadevida.donador.service.ValidadorPerfil
import mx.gotadevida.donador.service.Zona
import java.time.LocalDate
import javax.inject.Inject

data class PerfilUiState(
    val datos: DatosPerfil = DatosPerfil(),
    val zona: String? = null,
    val ubicacionPorGps: Boolean = false,
    val errores: Map<CampoPerfil, ErrorPerfil> = emptyMap(),
    val guardando: Boolean = false,
    val fallo: FalloApp? = null,
    val guardado: Boolean = false,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val repositorio: DonadorRepository,
    private val calendario: Calendario,
) : ViewModel() {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    init {
        // Si ya había perfil guardado, se precarga una vez para editarlo.
        viewModelScope.launch {
            val perfil = repositorio.observarPerfil().filterNotNull().first()
            _estado.update { it.copy(datos = perfil.aDatos(), zona = perfil.zona) }
        }
    }

    fun onTipoSangre(tipo: TipoSangre) = cambiar(CampoPerfil.TIPO_SANGRE) { copy(tipoSangre = tipo, noSeTipo = false) }

    fun onNoSeTipo() = cambiar(CampoPerfil.TIPO_SANGRE) { copy(tipoSangre = null, noSeTipo = true) }

    fun onNacimiento(fecha: LocalDate) = cambiar(CampoPerfil.NACIMIENTO) { copy(fechaNacimiento = fecha) }

    fun onPeso(texto: String) = cambiar(CampoPerfil.PESO) { copy(peso = texto.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) }

    fun onSexo(sexo: SexoParaCriteriosMedicos) = cambiar(CampoPerfil.SEXO) { copy(sexo = sexo) }

    fun onHaDonado(haDonado: Boolean) = cambiar(CampoPerfil.ULTIMA_DONACION) {
        copy(haDonado = haDonado, fechaUltimaDonacion = if (haDonado) fechaUltimaDonacion else null)
    }

    fun onUltimaDonacion(fecha: LocalDate) = cambiar(CampoPerfil.ULTIMA_DONACION) { copy(fechaUltimaDonacion = fecha) }

    fun onZona(zona: Zona) {
        cambiar(CampoPerfil.UBICACION) { copy(latitud = zona.latitud, longitud = zona.longitud) }
        _estado.update { it.copy(zona = zona.nombre, ubicacionPorGps = false) }
    }

    fun onUbicacionGps(latitud: Double, longitud: Double) {
        cambiar(CampoPerfil.UBICACION) { copy(latitud = latitud, longitud = longitud) }
        _estado.update { it.copy(ubicacionPorGps = true) }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando) return
        val errores = ValidadorPerfil.validar(actual.datos, calendario.hoy())
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        val d = actual.datos
        val cambios = ActualizarPerfilSolicitud(
            tipoSangre = d.tipoSangre,
            fechaNacimiento = d.fechaNacimiento?.toString(),
            pesoKg = ValidadorPerfil.pesoComoNumero(d.peso),
            sexoParaCriteriosMedicos = d.sexo,
            latitud = d.latitud,
            longitud = d.longitud,
            zona = actual.zona,
            fechaUltimaDonacion = if (d.haDonado == true) d.fechaUltimaDonacion?.toString() else null,
        )
        _estado.update { it.copy(guardando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.actualizarPerfil(cambios)) {
                is Resultado.Exito -> _estado.update { it.copy(guardando = false, guardado = true) }
                is Resultado.Fallo -> _estado.update { it.copy(guardando = false, fallo = r.fallo) }
            }
        }
    }

    private fun cambiar(campo: CampoPerfil, cambio: DatosPerfil.() -> DatosPerfil) =
        _estado.update { it.copy(datos = it.datos.cambio(), errores = it.errores - campo, fallo = null) }

    private fun PerfilDonador.aDatos() = DatosPerfil(
        tipoSangre = tipoSangre,
        noSeTipo = false,
        fechaNacimiento = fechaNacimiento?.let(LocalDate::parse),
        peso = pesoKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
        sexo = sexoParaCriteriosMedicos,
        latitud = latitud,
        longitud = longitud,
        haDonado = if (perfilCompleto) totalDonaciones > 0 else null,
        fechaUltimaDonacion = ultimaDonacion?.let(LocalDate::parse),
    )
}

data class InicioUiState(
    val nombre: String = "",
    val etiquetaTipoSangre: String? = null,
    val estadoDonacion: EstadoDonacion = EstadoDonacion.Cargando,
    val actualizando: Boolean = false,
    val sinConexion: Boolean = false,
)

@HiltViewModel
class InicioViewModel @Inject constructor(
    private val repositorio: DonadorRepository,
) : ViewModel() {

    private val refresco = MutableStateFlow(EstadoRefresco())

    private data class EstadoRefresco(val actualizando: Boolean = false, val sinConexion: Boolean = false)

    val estado: StateFlow<InicioUiState> = combine(
        repositorio.observarPerfil(),
        repositorio.observarElegibilidad(),
        refresco,
    ) { perfil, elegibilidad, r ->
        InicioUiState(
            nombre = perfil?.nombre?.substringBefore(' ').orEmpty(),
            etiquetaTipoSangre = perfil?.etiquetaTipoSangre,
            estadoDonacion = if (perfil == null && !r.actualizando && r.sinConexion) {
                EstadoDonacion.CompletarPerfil
            } else {
                CalculadorEstado.calcular(perfil, elegibilidad)
            },
            actualizando = r.actualizando,
            sinConexion = r.sinConexion,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    init {
        actualizar()
    }

    fun actualizar() {
        if (refresco.value.actualizando) return
        refresco.update { it.copy(actualizando = true) }
        viewModelScope.launch {
            val r = repositorio.refrescar()
            val sinConexion = r is Resultado.Fallo && r.fallo == FalloApp.SinConexion
            refresco.value = EstadoRefresco(actualizando = false, sinConexion = sinConexion)
        }
    }
}
