package mx.gotadevida.donador

import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.entity.TipoSangre
import mx.gotadevida.donador.service.CampoPerfil
import mx.gotadevida.donador.service.DatosPerfil
import mx.gotadevida.donador.service.ErrorPerfil
import mx.gotadevida.donador.service.ValidadorPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorPerfilTest {
    private val hoy = LocalDate.of(2026, 11, 11)
    private val valido = DatosPerfil(
        tipoSangre = TipoSangre.A_POSITIVO,
        fechaNacimiento = LocalDate.of(2000, 3, 15),
        peso = "68,5",
        sexo = SexoParaCriteriosMedicos.MASCULINO,
        latitud = 25.54,
        longitud = -103.406,
        haDonado = true,
        fechaUltimaDonacion = LocalDate.of(2026, 6, 1),
    )

    @Test
    fun `perfil completo no tiene errores`() {
        assertTrue(ValidadorPerfil.validar(valido, hoy).isEmpty())
    }

    @Test
    fun `perfil vacio pide todos los datos`() {
        val errores = ValidadorPerfil.validar(DatosPerfil(), hoy)
        assertEquals(CampoPerfil.entries.toSet(), errores.keys)
        assertTrue(errores.values.all { it == ErrorPerfil.REQUERIDO })
    }

    @Test
    fun `no saber el tipo de sangre es valido`() {
        val errores = ValidadorPerfil.validar(valido.copy(tipoSangre = null, noSeTipo = true), hoy)
        assertNull(errores[CampoPerfil.TIPO_SANGRE])
    }

    @Test
    fun `peso fuera de rango o con letras es invalido`() {
        listOf("29", "251", "abc", "6.5.1").forEach { peso ->
            assertEquals(peso, ErrorPerfil.PESO_INVALIDO, ValidadorPerfil.validar(valido.copy(peso = peso), hoy)[CampoPerfil.PESO])
        }
        assertEquals(68.5, ValidadorPerfil.pesoComoNumero("68,5")!!, 0.0)
    }

    @Test
    fun `fechas futuras se rechazan`() {
        val manana = hoy.plusDays(1)
        assertEquals(ErrorPerfil.FECHA_FUTURA, ValidadorPerfil.validar(valido.copy(fechaNacimiento = manana), hoy)[CampoPerfil.NACIMIENTO])
        assertEquals(ErrorPerfil.FECHA_FUTURA, ValidadorPerfil.validar(valido.copy(fechaUltimaDonacion = manana), hoy)[CampoPerfil.ULTIMA_DONACION])
    }

    @Test
    fun `nacimiento de hace mas de 100 anos es sospechoso`() {
        val errores = ValidadorPerfil.validar(valido.copy(fechaNacimiento = hoy.minusYears(101)), hoy)
        assertEquals(ErrorPerfil.EDAD_INVALIDA, errores[CampoPerfil.NACIMIENTO])
    }

    @Test
    fun `si nunca ha donado no se pide fecha de ultima donacion`() {
        val errores = ValidadorPerfil.validar(valido.copy(haDonado = false, fechaUltimaDonacion = null), hoy)
        assertNull(errores[CampoPerfil.ULTIMA_DONACION])
    }
}
