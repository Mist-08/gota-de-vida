package mx.gotadevida.donador.service

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.donador.entity.Donador
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.reglas.entity.ReglasElegibilidad

class ElegibilidadServiceTest {

    private val servicio = ElegibilidadService()
    private val reglas = ReglasElegibilidad.porDefecto("2026-11-12T00:00:00Z")
    private val hoy = LocalDate.parse("2026-11-12")

    private fun donador(
        nacimiento: String? = "2001-03-14",
        peso: Double? = 68.0,
        sexo: SexoParaCriteriosMedicos? = SexoParaCriteriosMedicos.FEMENINO,
        donaciones: List<String> = emptyList(),
        tatuaje: String? = null,
        tipo: TipoSangre? = TipoSangre.O_NEGATIVO
    ) = Donador(
        id = "d1",
        nombre = "Mariana",
        notificacionesActivas = true,
        radioMaximoKm = 15,
        fechasDonacion = donaciones,
        actualizadoEn = "2026-11-12T00:00:00Z",
        tipoSangre = tipo,
        fechaNacimiento = nacimiento,
        pesoKg = peso,
        sexoParaCriteriosMedicos = sexo,
        ubicacion = PuntoGeo.de(25.54, -103.41),
        fechaUltimoTatuaje = tatuaje
    )

    @Test
    fun `donadora con datos completos y sin donaciones recientes puede donar`() {
        val resultado = servicio.evaluar(donador(donaciones = listOf("2026-07-12")), reglas, hoy)

        assertTrue(resultado.elegible)
        assertEquals("2026-11-12", resultado.proximaFechaElegible)
        assertEquals(0L, resultado.diasParaVolverADonar)
    }

    @Test
    fun `quien dono hace 30 dias debe esperar a completar 60`() {
        val resultado = servicio.evaluar(donador(donaciones = listOf("2026-10-13")), reglas, hoy)

        assertFalse(resultado.elegible)
        assertEquals("2026-12-12", resultado.proximaFechaElegible)
        assertEquals(30L, resultado.diasParaVolverADonar)
    }

    @Test
    fun `quien dono hoy podra volver en 60 dias`() {
        val resultado = servicio.evaluar(donador(donaciones = listOf("2026-11-12")), reglas, hoy)

        assertEquals("2027-01-11", resultado.proximaFechaElegible)
        assertEquals(60L, resultado.diasParaVolverADonar)
    }

    @Test
    fun `una menor de edad puede donar el dia que cumpla 18`() {
        val resultado = servicio.evaluar(donador(nacimiento = "2009-01-20"), reglas, hoy)

        assertFalse(resultado.elegible)
        assertEquals("2027-01-20", resultado.proximaFechaElegible)
    }

    @Test
    fun `con peso menor al minimo no se promete fecha`() {
        val resultado = servicio.evaluar(donador(peso = 45.0), reglas, hoy)

        assertFalse(resultado.elegible)
        assertNull(resultado.proximaFechaElegible)
        assertTrue(resultado.motivos.any { it.contains("50 kg") })
    }

    @Test
    fun `una mujer con 3 donaciones en 12 meses espera a que la mas antigua cumpla un ano`() {
        val donaciones = listOf("2026-01-10", "2026-04-15", "2026-08-20")
        val resultado = servicio.evaluar(donador(donaciones = donaciones), reglas, hoy)

        assertFalse(resultado.elegible)
        assertEquals("2027-01-10", resultado.proximaFechaElegible)
    }

    @Test
    fun `un hombre con 3 donaciones en 12 meses todavia puede donar`() {
        val donaciones = listOf("2026-01-10", "2026-04-15", "2026-08-20")
        val resultado = servicio.evaluar(
            donador(donaciones = donaciones, sexo = SexoParaCriteriosMedicos.MASCULINO),
            reglas,
            hoy
        )

        assertTrue(resultado.elegible)
    }

    @Test
    fun `tatuaje reciente obliga a esperar 12 meses`() {
        val resultado = servicio.evaluar(donador(tatuaje = "2026-05-01"), reglas, hoy)

        assertFalse(resultado.elegible)
        assertEquals("2027-05-01", resultado.proximaFechaElegible)
    }

    @Test
    fun `si faltan datos lo dice y no promete fecha`() {
        val resultado = servicio.evaluar(donador(tipo = null, peso = null), reglas, hoy)

        assertFalse(resultado.elegible)
        assertTrue(resultado.faltanDatos)
        assertNull(resultado.proximaFechaElegible)
        assertEquals(2, resultado.motivos.size)
    }

    @Test
    fun `siempre recuerda que la decision final es del personal medico`() {
        val resultado = servicio.evaluar(donador(), reglas, hoy)

        assertEquals(ElegibilidadService.AVISO_MEDICO, resultado.aviso)
    }
}
