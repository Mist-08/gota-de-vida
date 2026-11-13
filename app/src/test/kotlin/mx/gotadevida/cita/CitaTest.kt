package mx.gotadevida.cita

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mx.gotadevida.cita.entity.AgendarPeticion
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.cita.entity.Horario
import mx.gotadevida.cita.entity.Requisitos
import mx.gotadevida.cita.repository.CitaRepository
import mx.gotadevida.cita.repository.EnvioCita
import mx.gotadevida.cita.service.CodigoQr
import mx.gotadevida.cita.viewmodel.AgendarViewModel
import mx.gotadevida.cita.viewmodel.MiCitaViewModel
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.tiempo.Calendario
import mx.gotadevida.hospital.viewmodel.EscanearViewModel
import mx.gotadevida.prueba.HospitalFalso
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun cita(id: String, fechaHora: String, estado: EstadoCita = EstadoCita.AGENDADA) =
    Cita(id, "h1", "Hospital Universitario", "Av. Morelos 900", 25.54, -103.40, fechaHora, estado, codigoQr = "abc123")

class CitaFalsa(var sinRed: Boolean = false) : CitaRepository {
    val citas = MutableStateFlow<List<Cita>>(emptyList())
    val pendiente = MutableStateFlow(false)
    val agendadas = mutableListOf<AgendarPeticion>()

    override fun observarCitas() = citas
    override fun observarAgendaPendiente() = pendiente
    override suspend fun refrescar() = Resultado.Exito(Unit)
    override suspend fun horarios(hospitalId: String, fecha: String) =
        Resultado.Exito(listOf(Horario("08:00", 0, false), Horario("09:30", 2, true)))
    override suspend fun requisitos() = Resultado.Exito(Requisitos(18, 65, 50.0, 4, 12))
    override suspend fun agendar(peticion: AgendarPeticion): Resultado<EnvioCita> {
        agendadas += peticion
        if (sinRed) {
            pendiente.value = true
            return Resultado.Exito(EnvioCita.PENDIENTE)
        }
        citas.value = citas.value + cita("nueva", peticion.fechaHora)
        return Resultado.Exito(EnvioCita.AGENDADA)
    }
    override suspend fun cancelar(id: String): Resultado<Unit> {
        citas.value = citas.value.map { if (it.id == id) it.copy(estado = EstadoCita.CANCELADA) else it }
        return Resultado.Exito(Unit)
    }
}

class CitaTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    private val hoy = LocalDate.of(2026, 11, 12)
    private val calendario = Calendario().apply {
        reloj =
            Clock.fixed(hoy.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }
    private val ruta = SavedStateHandle(
        mapOf(
            "hospitalId" to "h1",
            "hospitalNombre" to "Hospital Universitario",
            "solicitudId" to "u1"
        )
    )

    @Test
    fun `agendar muestra 7 dias, carga horarios y requisitos y manda la urgencia`() = runTest {
        val repo = CitaFalsa()
        val vm = AgendarViewModel(repo, calendario, ruta)
        val e = vm.estado.value
        assertEquals(7, e.dias.size)
        assertEquals(hoy, e.dia)
        assertEquals(12, e.requisitos!!.horasAyunoMaximo)
        assertEquals(2, e.horarios.size)
        vm.onHora("09:30")
        vm.confirmar()
        assertEquals(AgendarPeticion("h1", "2026-11-12T09:30", "u1"), repo.agendadas.single())
        assertEquals(EnvioCita.AGENDADA, vm.estado.value.resultado)
    }

    @Test
    fun `sin hora elegida no se agenda`() = runTest {
        val repo = CitaFalsa()
        val vm = AgendarViewModel(repo, calendario, ruta)
        vm.confirmar()
        assertTrue(repo.agendadas.isEmpty())
    }

    @Test
    fun `sin red la cita queda pendiente de enviar`() = runTest {
        val vm = AgendarViewModel(CitaFalsa(sinRed = true), calendario, ruta)
        vm.onHora("09:30")
        vm.confirmar()
        assertEquals(EnvioCita.PENDIENTE, vm.estado.value.resultado)
    }

    @Test
    fun `mi cita es la proxima agendada y se puede cancelar`() = runTest {
        val repo = CitaFalsa()
        repo.citas.value = listOf(
            cita("vieja", "2026-10-01T09:00", EstadoCita.COMPLETADA),
            cita("luego", "2026-11-20T10:00"),
            cita("pronto", "2026-11-13T09:30")
        )
        val vm = MiCitaViewModel(repo, calendario)
        vm.estado.test {
            var e = awaitItem()
            while (e.cita == null) e = awaitItem()
            assertEquals("pronto", e.cita!!.id)
            assertEquals("gota:pronto:abc123", e.cita!!.contenidoQr)
            vm.cancelar()
            while (e.cita?.id == "pronto") e = awaitItem()
            assertEquals("luego", e.cita!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `el QR se genera en el telefono`() {
        val m = CodigoQr.matriz("gota:pronto:abc123", 200)
        assertEquals(200, m.size)
        assertTrue(m.flatMap { it.toList() }.count { it } > 1000)
    }

    @Test
    fun `el hospital confirma con el QR y no acepta codigos ajenos`() = runTest {
        val repo = HospitalFalso()
        val vm = EscanearViewModel(repo)
        vm.alLeer("https://otra-cosa.com")
        assertNotNull(vm.estado.value.fallo)
        assertNull(vm.estado.value.confirmada)
        vm.alLeer("gota:c1:abc123")
        assertEquals("Mariana", vm.estado.value.confirmada!!.donadorNombre)
        // Lecturas repetidas de la cámara no confirman dos veces.
        vm.alLeer("gota:c1:abc123")
        assertEquals(2, repo.checkins.size)
    }
}
