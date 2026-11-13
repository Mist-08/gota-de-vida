package mx.gotadevida.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FalloDeHttpTest {
    @Test
    fun `lee codigo y mensaje del cuerpo de error del backend`() {
        val fallo = falloDeHttp(409, """{"error":"EMAIL_EN_USO","mensaje":"Ese correo ya tiene cuenta.","extra":1}""")
        assertEquals(409, fallo.estado)
        assertEquals("EMAIL_EN_USO", fallo.codigo)
        assertEquals("Ese correo ya tiene cuenta.", fallo.mensaje)
    }

    @Test
    fun `cuerpo vacio o que no es JSON no truena`() {
        listOf(null, "", "<html>502</html>").forEach { cuerpo ->
            val fallo = falloDeHttp(502, cuerpo)
            assertEquals(502, fallo.estado)
            assertNull(fallo.codigo)
            assertNull(fallo.mensaje)
        }
    }
}
