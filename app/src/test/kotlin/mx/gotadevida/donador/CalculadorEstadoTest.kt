package mx.gotadevida.donador

import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.service.CalculadorEstado
import mx.gotadevida.donador.service.EstadoDonacion
import mx.gotadevida.prueba.perfilDePrueba
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculadorEstadoTest {
    private fun elegibilidad(
        elegible: Boolean = true,
        faltanDatos: Boolean = false,
        motivos: List<String> = emptyList(),
        proxima: String? = null,
        dias: Long? = null,
    ) = Elegibilidad(elegible, faltanDatos, motivos, aviso = "", proximaFechaElegible = proxima, diasParaVolverADonar = dias)

    @Test
    fun `sin perfil todavia esta cargando`() {
        assertEquals(EstadoDonacion.Cargando, CalculadorEstado.calcular(null, null))
    }

    @Test
    fun `perfil incompleto o con datos faltantes pide completarlo`() {
        assertEquals(EstadoDonacion.CompletarPerfil, CalculadorEstado.calcular(perfilDePrueba(completo = false), elegibilidad()))
        assertEquals(EstadoDonacion.CompletarPerfil, CalculadorEstado.calcular(perfilDePrueba(), elegibilidad(faltanDatos = true)))
        assertEquals(EstadoDonacion.CompletarPerfil, CalculadorEstado.calcular(perfilDePrueba(), null))
    }

    @Test
    fun `elegible muestra la ultima donacion`() {
        val estado = CalculadorEstado.calcular(perfilDePrueba(ultimaDonacion = "2026-05-01"), elegibilidad())
        assertEquals(EstadoDonacion.PuedeDonar(LocalDate.of(2026, 5, 1)), estado)
    }

    @Test
    fun `en espera muestra dias y fecha`() {
        val estado = CalculadorEstado.calcular(perfilDePrueba(), elegibilidad(elegible = false, proxima = "2026-12-20", dias = 39))
        assertEquals(EstadoDonacion.Esperar(39, LocalDate.of(2026, 12, 20)), estado)
    }

    @Test
    fun `sin fecha proxima muestra los motivos`() {
        val estado = CalculadorEstado.calcular(perfilDePrueba(), elegibilidad(elegible = false, motivos = listOf("Peso menor a 50 kg")))
        assertEquals(EstadoDonacion.NoPuede(listOf("Peso menor a 50 kg")), estado)
    }
}
