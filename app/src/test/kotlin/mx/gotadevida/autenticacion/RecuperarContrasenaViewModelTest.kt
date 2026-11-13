package mx.gotadevida.autenticacion

import kotlinx.coroutines.test.runTest
import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo
import mx.gotadevida.autenticacion.viewmodel.PasoRecuperacion
import mx.gotadevida.autenticacion.viewmodel.RecuperarContrasenaViewModel
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.prueba.AutenticacionFalsa
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecuperarContrasenaViewModelTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    @Test
    fun `un correo invalido no llama a la API`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.onEmail("ana@")
        vm.enviarCodigo()
        assertTrue(repo.recuperaciones.isEmpty())
        assertEquals(ErrorCampo.EMAIL_INVALIDO, vm.estado.value.errores[CampoCuenta.EMAIL])
        assertEquals(PasoRecuperacion.CORREO, vm.estado.value.paso)
    }

    @Test
    fun `trae el correo de iniciar sesion, lo manda en minusculas y pasa al codigo`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.usarCorreoInicial(" Ana@Correo.com ")
        vm.enviarCodigo()
        assertEquals("ana@correo.com", repo.recuperaciones.single())
        assertEquals(PasoRecuperacion.CODIGO, vm.estado.value.paso)
        assertFalse(vm.estado.value.codigoReenviado)
    }

    @Test
    fun `el codigo solo acepta 6 digitos y la contrasena 8 caracteres`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.onEmail("ana@correo.com")
        vm.enviarCodigo()
        vm.onCodigo("12a3 4")
        vm.onPassword("corta")
        vm.cambiarContrasena()
        assertEquals("1234", vm.estado.value.codigo)
        assertEquals(ErrorCampo.CODIGO_INVALIDO, vm.estado.value.errores[CampoCuenta.CODIGO])
        assertEquals(ErrorCampo.PASSWORD_CORTA, vm.estado.value.errores[CampoCuenta.PASSWORD])
        assertTrue(repo.restablecimientos.isEmpty())

        vm.onCodigo("1234567890")
        assertEquals("123456", vm.estado.value.codigo)
    }

    @Test
    fun `con codigo y contrasena validos cambia la contrasena y termina listo`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.onEmail("ana@correo.com")
        vm.enviarCodigo()
        vm.onCodigo("123456")
        vm.onPassword("nueva-segura")
        vm.cambiarContrasena()
        assertEquals(Triple("ana@correo.com", "123456", "nueva-segura"), repo.restablecimientos.single())
        assertTrue(vm.estado.value.listo)
    }

    @Test
    fun `si el codigo es incorrecto se muestra el mensaje del servidor`() = runTest {
        val fallo = FalloApp.Servidor(400, "datos_invalidos", "El código no es correcto.")
        val repo = AutenticacionFalsa(Resultado.Fallo(fallo))
        val vm = RecuperarContrasenaViewModel(repo)
        vm.onEmail("ana@correo.com")
        vm.enviarCodigo()
        vm.onCodigo("000000")
        vm.onPassword("nueva-segura")
        vm.cambiarContrasena()
        assertEquals(fallo, vm.estado.value.fallo)
        assertFalse(vm.estado.value.listo)
    }

    @Test
    fun `reenviar avisa que llego un codigo nuevo y cambiar correo regresa al inicio`() = runTest {
        val repo = AutenticacionFalsa()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.onEmail("ana@correo.com")
        vm.enviarCodigo()
        vm.enviarCodigo()
        assertEquals(2, repo.recuperaciones.size)
        assertTrue(vm.estado.value.codigoReenviado)

        vm.cambiarCorreo()
        assertEquals(PasoRecuperacion.CORREO, vm.estado.value.paso)
        assertEquals("ana@correo.com", vm.estado.value.email)
    }
}
