package mx.gotadevida.autenticacion

import kotlinx.coroutines.test.runTest
import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo
import mx.gotadevida.autenticacion.viewmodel.InicioSesionViewModel
import mx.gotadevida.autenticacion.viewmodel.RegistroViewModel
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.prueba.AutenticacionFalsa
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CuentaViewModelsTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    private fun registroLleno(repo: AutenticacionFalsa) = RegistroViewModel(repo).apply {
        onNombre("  Ana López ")
        onEmail(" Ana@Correo.com ")
        onPassword("secreta123")
        onTelefono("871 123 4567")
    }

    @Test
    fun `registro invalido no llama a la API y marca los campos`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RegistroViewModel(repo)
        vm.registrar(aceptoAviso = true)
        assertTrue(repo.registros.isEmpty())
        assertEquals(ErrorCampo.VACIO, vm.estado.value.errores[CampoCuenta.NOMBRE])
        assertFalse(vm.estado.value.listo)
    }

    @Test
    fun `escribir en un campo borra solo su error`() = runTest {
        val vm = RegistroViewModel(AutenticacionFalsa())
        vm.registrar(aceptoAviso = true)
        vm.onNombre("Ana")
        assertFalse(CampoCuenta.NOMBRE in vm.estado.value.errores)
        assertTrue(CampoCuenta.EMAIL in vm.estado.value.errores)
    }

    @Test
    fun `registro valido normaliza datos y termina listo`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = registroLleno(repo)
        vm.registrar(aceptoAviso = true)
        val enviado = repo.registros.single()
        assertEquals("Ana López", enviado.nombre)
        assertEquals("ana@correo.com", enviado.email)
        assertTrue(enviado.aceptaAvisoPrivacidad)
        assertTrue(vm.estado.value.listo)
        assertFalse(vm.estado.value.enviando)
    }

    @Test
    fun `si el correo ya existe se muestra el fallo del servidor`() = runTest {
        val fallo = FalloApp.Servidor(409, "EMAIL_EN_USO", "Ese correo ya tiene cuenta.")
        val vm = registroLleno(AutenticacionFalsa(Resultado.Fallo(fallo)))
        vm.registrar(aceptoAviso = true)
        assertEquals(fallo, vm.estado.value.fallo)
        assertFalse(vm.estado.value.listo)
    }

    @Test
    fun `inicio de sesion manda el correo en minusculas`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = InicioSesionViewModel(repo)
        vm.onEmail("ANA@correo.com ")
        vm.onPassword("secreta123")
        vm.entrar()
        assertEquals("ana@correo.com" to "secreta123", repo.inicios.single())
        assertTrue(vm.estado.value.listo)
    }

    @Test
    fun `sin conexion el inicio de sesion no termina`() = runTest {
        val vm = InicioSesionViewModel(AutenticacionFalsa(Resultado.Fallo(FalloApp.SinConexion)))
        vm.onEmail("ana@correo.com")
        vm.onPassword("secreta123")
        vm.entrar()
        assertEquals(FalloApp.SinConexion, vm.estado.value.fallo)
        assertFalse(vm.estado.value.listo)
    }
}
