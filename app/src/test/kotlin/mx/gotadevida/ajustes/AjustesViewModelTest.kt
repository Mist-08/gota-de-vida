package mx.gotadevida.ajustes

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mx.gotadevida.ajustes.repository.AjustesRepository
import mx.gotadevida.ajustes.viewmodel.AjustesViewModel
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.sesion.GestorSesion
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class AjustesFalso(var falla: Boolean = false) : AjustesRepository {
    var activas = true
    var eliminada = false
    override suspend fun notificacionesActivas() = Resultado.Exito(activas)
    override suspend fun cambiarNotificaciones(activas: Boolean): Resultado<Unit> {
        if (falla) return Resultado.Fallo(FalloApp.SinConexion)
        this.activas = activas
        return Resultado.Exito(Unit)
    }
    override suspend fun eliminarCuenta(): Resultado<Unit> {
        if (falla) return Resultado.Fallo(FalloApp.SinConexion)
        eliminada = true
        return Resultado.Exito(Unit)
    }
}

class AjustesViewModelTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()
    private val sesion = mockk<GestorSesion>(relaxed = true)

    @Test
    fun `apagar los avisos se guarda en el servidor`() = runTest {
        val repo = AjustesFalso()
        val vm = AjustesViewModel(sesion, repo)
        vm.cargarPreferencias()
        assertEquals(true, vm.estado.value.notificaciones)
        vm.cambiarNotificaciones(false)
        assertFalse(repo.activas)
    }

    @Test
    fun `si falla la red el interruptor regresa a como estaba`() = runTest {
        val vm = AjustesViewModel(sesion, AjustesFalso(falla = true))
        vm.cargarPreferencias()
        vm.cambiarNotificaciones(false)
        assertEquals(true, vm.estado.value.notificaciones)
        assertEquals(FalloApp.SinConexion, vm.estado.value.fallo)
    }

    @Test
    fun `eliminar la cuenta borra en el servidor y luego cierra la sesion`() = runTest {
        val repo = AjustesFalso()
        AjustesViewModel(sesion, repo).eliminarCuenta()
        assertEquals(true, repo.eliminada)
        coVerify { sesion.cerrarSesion() }
    }

    @Test
    fun `si no se pudo eliminar no se cierra la sesion`() = runTest {
        val vm = AjustesViewModel(sesion, AjustesFalso(falla = true))
        vm.eliminarCuenta()
        assertFalse(vm.estado.value.eliminando)
        coVerify(exactly = 0) { sesion.cerrarSesion() }
    }
}
