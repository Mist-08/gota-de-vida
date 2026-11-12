package mx.gotadevida.cita.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mx.gotadevida.cita.entity.AgendarPeticion
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.cita.entity.Horario
import mx.gotadevida.cita.entity.Requisitos
import mx.gotadevida.cita.repository.CitaRepository
import mx.gotadevida.cita.repository.EnvioCita
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.tiempo.Calendario
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Ruta: agendar en un hospital, opcionalmente por una urgencia aceptada. */
@Serializable
data class AgendarCita(val hospitalId: String, val hospitalNombre: String, val solicitudId: String? = null)

@Serializable
data object MiCita

data class AgendarUiState(
    val hospitalNombre: String = "",
    val dias: List<LocalDate> = emptyList(),
    val dia: LocalDate? = null,
    val horarios: List<Horario> = emptyList(),
    val hora: String? = null,
    val requisitos: Requisitos? = null,
    val cargandoHorarios: Boolean = false,
    val enviando: Boolean = false,
    val fallo: FalloApp? = null,
    val resultado: EnvioCita? = null,
)

@HiltViewModel
class AgendarViewModel @Inject constructor(
    private val repositorio: CitaRepository,
    private val calendario: Calendario,
    estadoGuardado: SavedStateHandle,
) : ViewModel() {
    private val hospitalId: String = checkNotNull(estadoGuardado["hospitalId"])
    private val solicitudId: String? = estadoGuardado["solicitudId"]
    private val _estado = MutableStateFlow(
        AgendarUiState(
            hospitalNombre = estadoGuardado["hospitalNombre"] ?: "",
            dias = (0L until 7L).map { calendario.hoy().plusDays(it) },
        ),
    )
    val estado: StateFlow<AgendarUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            (repositorio.requisitos() as? Resultado.Exito)?.let { r -> _estado.update { it.copy(requisitos = r.valor) } }
        }
        onDia(_estado.value.dias.first())
    }

    fun onDia(dia: LocalDate) {
        _estado.update { it.copy(dia = dia, hora = null, horarios = emptyList(), cargandoHorarios = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.horarios(hospitalId, dia.toString())) {
                is Resultado.Exito -> _estado.update { if (it.dia == dia) it.copy(horarios = r.valor, cargandoHorarios = false) else it }
                is Resultado.Fallo -> _estado.update { it.copy(cargandoHorarios = false, fallo = r.fallo) }
            }
        }
    }

    fun onHora(hora: String) = _estado.update { it.copy(hora = hora) }

    fun confirmar() {
        val actual = _estado.value
        val dia = actual.dia ?: return
        val hora = actual.hora ?: return
        if (actual.enviando) return
        _estado.update { it.copy(enviando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.agendar(AgendarPeticion(hospitalId, "${dia}T$hora", solicitudId))) {
                is Resultado.Exito -> _estado.update { it.copy(enviando = false, resultado = r.valor) }
                is Resultado.Fallo -> _estado.update { it.copy(enviando = false, fallo = r.fallo) }
            }
        }
    }
}

data class MiCitaUiState(
    val cita: Cita? = null,
    val pendiente: Boolean = false,
    val cancelando: Boolean = false,
    val fallo: FalloApp? = null,
)

/** La cita próxima con su QR. Se lee de Room: funciona sin señal dentro del hospital. */
@HiltViewModel
class MiCitaViewModel @Inject constructor(
    private val repositorio: CitaRepository,
    private val calendario: Calendario,
) : ViewModel() {
    private val local = MutableStateFlow(MiCitaUiState())

    val estado: StateFlow<MiCitaUiState> = combine(
        repositorio.observarCitas(),
        repositorio.observarAgendaPendiente(),
        local,
    ) { citas, pendiente, l -> l.copy(cita = proxima(citas), pendiente = pendiente) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiCitaUiState())

    init {
        viewModelScope.launch { repositorio.refrescar() }
    }

    private fun proxima(citas: List<Cita>): Cita? {
        val hoy = calendario.hoy()
        return citas.filter { it.estado == EstadoCita.AGENDADA && !LocalDateTime.parse(it.fechaHora).toLocalDate().isBefore(hoy) }
            .minByOrNull { it.fechaHora }
    }

    fun cancelar() {
        val cita = estado.value.cita ?: return
        local.update { it.copy(cancelando = true, fallo = null) }
        viewModelScope.launch {
            when (val r = repositorio.cancelar(cita.id)) {
                is Resultado.Exito -> local.update { it.copy(cancelando = false) }
                is Resultado.Fallo -> local.update { it.copy(cancelando = false, fallo = r.fallo) }
            }
        }
    }
}
