package mx.gotadevida.solicitud

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.gotadevida.prueba.ColaFalsa
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import mx.gotadevida.prueba.SolicitudApiFalsa
import mx.gotadevida.prueba.SolicitudDaoFalso
import mx.gotadevida.solicitud.repository.SolicitudRepositoryLocalYRed
import mx.gotadevida.solicitud.viewmodel.DetalleUrgenciaViewModel
import mx.gotadevida.solicitud.viewmodel.UrgenciasViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UrgenciasViewModelsTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    private val api = SolicitudApiFalsa()
    private val repo = SolicitudRepositoryLocalYRed(api, SolicitudDaoFalso(), ColaFalsa(), Json)

    @Test
    fun `la lista muestra las urgencias cercanas`() = runTest {
        val vm = UrgenciasViewModel(repo)
        vm.estado.test {
            var e = awaitItem()
            while (e.urgencias.isEmpty()) e = awaitItem()
            assertEquals("Hospital Universitario", e.urgencias.single().hospitalNombre)
            assertFalse(e.cargando)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aceptar con red muestra el agradecimiento`() = runTest {
        repo.refrescarCercanas()
        val vm = DetalleUrgenciaViewModel(repo, SavedStateHandle(mapOf("id" to "u1")))
        vm.estado.test {
            awaitItem()
            vm.responder(true)
            var e = awaitItem()
            while (!e.acepto) e = awaitItem()
            assertEquals(1, e.urgencia!!.unidadesComprometidas)
            assertFalse(e.pendiente)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aceptar sin red queda pendiente de enviar`() = runTest {
        repo.refrescarCercanas()
        api.sinRed = true
        val vm = DetalleUrgenciaViewModel(repo, SavedStateHandle(mapOf("id" to "u1")))
        vm.estado.test {
            awaitItem()
            vm.responder(true)
            var e = awaitItem()
            while (!e.pendiente) e = awaitItem()
            assertTrue(e.pendiente)
            assertFalse(e.acepto)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
