package mx.gotadevida.autenticacion

import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo
import mx.gotadevida.autenticacion.service.ValidadorCuenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorCuentaTest {
    @Test
    fun `nombre vacio o de una letra es invalido`() {
        assertEquals(ErrorCampo.VACIO, ValidadorCuenta.nombre("  "))
        assertEquals(ErrorCampo.NOMBRE_CORTO, ValidadorCuenta.nombre(" A "))
        assertNull(ValidadorCuenta.nombre("Ana"))
    }

    @Test
    fun `email necesita arroba y dominio`() {
        assertEquals(ErrorCampo.EMAIL_INVALIDO, ValidadorCuenta.email("ana.correo.com"))
        assertEquals(ErrorCampo.EMAIL_INVALIDO, ValidadorCuenta.email("ana@correo"))
        assertNull(ValidadorCuenta.email(" Ana@Correo.com "))
    }

    @Test
    fun `password minimo de 8 caracteres`() {
        assertEquals(ErrorCampo.PASSWORD_CORTA, ValidadorCuenta.password("1234567"))
        assertNull(ValidadorCuenta.password("12345678"))
    }

    @Test
    fun `telefono acepta 10 digitos con espacios guiones o +52`() {
        assertNull(ValidadorCuenta.telefono("871 123 4567"))
        assertNull(ValidadorCuenta.telefono("871-123-4567"))
        assertNull(ValidadorCuenta.telefono("+52 871 123 4567"))
        assertEquals(ErrorCampo.TELEFONO_INVALIDO, ValidadorCuenta.telefono("87112345"))
        assertEquals(ErrorCampo.TELEFONO_INVALIDO, ValidadorCuenta.telefono("+1 871 123 4567"))
    }

    @Test
    fun `registro junta los errores de cada campo`() {
        val errores = ValidadorCuenta.registro(nombre = "", email = "x", password = "corta", telefono = "123")
        assertEquals(
            mapOf(
                CampoCuenta.NOMBRE to ErrorCampo.VACIO,
                CampoCuenta.EMAIL to ErrorCampo.EMAIL_INVALIDO,
                CampoCuenta.PASSWORD to ErrorCampo.PASSWORD_CORTA,
                CampoCuenta.TELEFONO to ErrorCampo.TELEFONO_INVALIDO,
            ),
            errores,
        )
        assertTrue(ValidadorCuenta.registro("Ana", "ana@correo.com", "12345678", "8711234567").isEmpty())
    }
}
