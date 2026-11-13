package mx.gotadevida.hospital

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.runTest
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.hospital.entity.EstadoUrgenciaHospital
import mx.gotadevida.hospital.entity.TipoSangrePaciente
import mx.gotadevida.hospital.viewmodel.CrearUrgenciaViewModel
import mx.gotadevida.hospital.viewmodel.DetalleUrgenciaHospitalViewModel
import mx.gotadevida.hospital.viewmodel.PanelHospitalViewModel
import mx.gotadevida.prueba.HospitalFalso
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HospitalViewModelsTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    @Test
    fun `no se publica sin elegir el tipo de sangre`() = runTest {
        val repo = HospitalFalso()
        val vm = CrearUrgenciaViewModel(repo)
        vm.publicar()
        assertTrue(vm.estado.value.faltaTipo)
        assertTrue(repo.publicadas.isEmpty())
    }

    @Test
    fun `las unidades se quedan entre 1 y 20`() = runTest {
        val vm = CrearUrgenciaViewModel(HospitalFalso())
        vm.onUnidades(0)
        assertEquals(1, vm.estado.value.unidades)
        vm.onUnidades(99)
        assertEquals(20, vm.estado.value.unidades)
    }

    @Test
    fun `publicar manda tipo, unidades y nota y regresa el id`() = runTest {
        val repo = HospitalFalso()
        val vm = CrearUrgenciaViewModel(repo)
        vm.onTipo(TipoSangrePaciente.O_NEGATIVO)
        vm.onUnidades(3)
        vm.onNota("  Banco de sangre, planta baja  ")
        vm.publicar()
        val enviada = repo.publicadas.single()
        assertEquals(TipoSangrePaciente.O_NEGATIVO, enviada.tipoSangre)
        assertEquals(3, enviada.unidades)
        assertEquals("Banco de sangre, planta baja", enviada.nota)
        assertEquals("u1", vm.estado.value.creadaId)
    }

    @Test
    fun `si falla la red el error se muestra y no se navega`() = runTest {
        val vm = CrearUrgenciaViewModel(HospitalFalso(Resultado.Fallo(FalloApp.SinConexion)))
        vm.onTipo(TipoSangrePaciente.A_POSITIVO)
        vm.publicar()
        assertEquals(FalloApp.SinConexion, vm.estado.value.fallo)
        assertNull(vm.estado.value.creadaId)
    }

    @Test
    fun `el panel carga las urgencias y el detalle permite cerrar`() = runTest {
        val panel = PanelHospitalViewModel(HospitalFalso())
        panel.actualizar()
        assertEquals(1, panel.estado.value.urgencias.size)

        val detalle = DetalleUrgenciaHospitalViewModel(HospitalFalso(), SavedStateHandle(mapOf("id" to "u1")))
        detalle.actualizar()
        assertEquals(12, detalle.estado.value.urgencia!!.donadoresNotificados)
        detalle.terminar(EstadoUrgenciaHospital.CERRADA)
        assertEquals(EstadoUrgenciaHospital.CERRADA, detalle.estado.value.urgencia!!.estado)
    }
}
