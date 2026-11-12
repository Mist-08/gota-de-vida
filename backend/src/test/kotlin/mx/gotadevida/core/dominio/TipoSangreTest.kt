package mx.gotadevida.core.dominio

import mx.gotadevida.core.dominio.TipoSangre.AB_NEGATIVO
import mx.gotadevida.core.dominio.TipoSangre.AB_POSITIVO
import mx.gotadevida.core.dominio.TipoSangre.A_NEGATIVO
import mx.gotadevida.core.dominio.TipoSangre.A_POSITIVO
import mx.gotadevida.core.dominio.TipoSangre.B_NEGATIVO
import mx.gotadevida.core.dominio.TipoSangre.B_POSITIVO
import mx.gotadevida.core.dominio.TipoSangre.O_NEGATIVO
import mx.gotadevida.core.dominio.TipoSangre.O_POSITIVO
import kotlin.test.Test
import kotlin.test.assertEquals

class TipoSangreTest {

    /** Tabla de la sección 4.1 del prompt maestro: receptor ← donadores aceptados. */
    private val tabla = mapOf(
        O_NEGATIVO to setOf(O_NEGATIVO),
        O_POSITIVO to setOf(O_POSITIVO, O_NEGATIVO),
        A_NEGATIVO to setOf(A_NEGATIVO, O_NEGATIVO),
        A_POSITIVO to setOf(A_POSITIVO, A_NEGATIVO, O_POSITIVO, O_NEGATIVO),
        B_NEGATIVO to setOf(B_NEGATIVO, O_NEGATIVO),
        B_POSITIVO to setOf(B_POSITIVO, B_NEGATIVO, O_POSITIVO, O_NEGATIVO),
        AB_NEGATIVO to setOf(AB_NEGATIVO, A_NEGATIVO, B_NEGATIVO, O_NEGATIVO),
        AB_POSITIVO to TipoSangre.entries.toSet(),
    )

    @Test
    fun `las 64 combinaciones coinciden con la tabla de compatibilidad`() {
        for (receptor in TipoSangre.entries) {
            for (donador in TipoSangre.entries) {
                val esperado = donador in tabla.getValue(receptor)
                assertEquals(esperado, donador.puedeDonarA(receptor), "$donador → $receptor")
            }
        }
    }

    @Test
    fun `O negativo dona a todos y AB positivo recibe de todos`() {
        assertEquals(TipoSangre.entries.size, TipoSangre.entries.count { O_NEGATIVO.puedeDonarA(it) })
        assertEquals(TipoSangre.entries.toSet(), TipoSangre.donadoresCompatiblesCon(AB_POSITIVO).toSet())
    }

    @Test
    fun `para un paciente O negativo solo sirve O negativo`() {
        assertEquals(listOf(O_NEGATIVO), TipoSangre.donadoresCompatiblesCon(O_NEGATIVO))
    }

    @Test
    fun `la etiqueta usa el signo de menos real`() {
        assertEquals("O−", O_NEGATIVO.etiqueta)
    }
}
