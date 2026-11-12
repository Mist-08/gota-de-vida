package mx.gotadevida.donador

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.tiempo.Calendario
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.entity.TipoSangre
import mx.gotadevida.donador.service.CampoPerfil
import mx.gotadevida.donador.service.EstadoDonacion
import mx.gotadevida.donador.service.ZonasLaguna
import mx.gotadevida.donador.viewmodel.InicioViewModel
import mx.gotadevida.donador.viewmodel.PerfilViewModel
import mx.gotadevida.prueba.DonadorFalso
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import mx.gotadevida.prueba.perfilDePrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class DonadorViewModelsTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    private val calendario = Calendario().apply {
        reloj = Clock.fixed(LocalDate.of(2026, 11, 11).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    }

    private fun llenar(vm: PerfilViewModel) = vm.apply {
        onTipoSangre(TipoSangre.B_POSITIVO)
        onNacimiento(LocalDate.of(1999, 1, 20))
        onPeso("70kg")
        onSexo(SexoParaCriteriosMedicos.FEMENINO)
        onZona(ZonasLaguna.lista.first())
        onHaDonado(false)
    }

    @Test
    fun `perfil precarga lo guardado para editar`() = runTest {
        val vm = PerfilViewModel(DonadorFalso(perfil = perfilDePrueba(ultimaDonacion = "2026-04-02")), calendario)
        val datos = vm.estado.value.datos
        assertEquals(TipoSangre.O_NEGATIVO, datos.tipoSangre)
        assertEquals("62.5", datos.peso)
        assertEquals(true, datos.haDonado)
        assertEquals(LocalDate.of(2026, 4, 2), datos.fechaUltimaDonacion)
        assertEquals("Torreón", vm.estado.value.zona)
    }

    @Test
    fun `peso solo acepta numeros`() = runTest {
        val vm = PerfilViewModel(DonadorFalso(), calendario)
        vm.onPeso("70kg")
        assertEquals("70", vm.estado.value.datos.peso)
    }

    @Test
    fun `guardar perfil incompleto marca errores sin llamar a la API`() = runTest {
        val repo = DonadorFalso()
        val vm = PerfilViewModel(repo, calendario)
        vm.guardar()
        assertTrue(repo.actualizaciones.isEmpty())
        assertTrue(CampoPerfil.TIPO_SANGRE in vm.estado.value.errores)
    }

    @Test
    fun `guardar perfil valido envia los datos y termina`() = runTest {
        val repo = DonadorFalso()
        val vm = llenar(PerfilViewModel(repo, calendario))
        vm.guardar()
        val enviado = repo.actualizaciones.single()
        assertEquals(TipoSangre.B_POSITIVO, enviado.tipoSangre)
        assertEquals("1999-01-20", enviado.fechaNacimiento)
        assertEquals(70.0, enviado.pesoKg!!, 0.0)
        assertEquals(ZonasLaguna.lista.first().nombre, enviado.zona)
        assertNull(enviado.fechaUltimaDonacion)
        assertTrue(vm.estado.value.guardado)
    }

    @Test
    fun `si falla el guardado se queda en la pantalla con el error`() = runTest {
        val repo = DonadorFalso(respuestaActualizar = Resultado.Fallo(FalloApp.SinConexion))
        val vm = llenar(PerfilViewModel(repo, calendario))
        vm.guardar()
        assertEquals(FalloApp.SinConexion, vm.estado.value.fallo)
        assertFalse(vm.estado.value.guardado)
    }

    @Test
    fun `inicio muestra nombre corto tipo de sangre y estado`() = runTest {
        val repo = DonadorFalso(
            perfil = perfilDePrueba(),
            elegibilidad = Elegibilidad(true, false, emptyList(), aviso = ""),
        )
        val vm = InicioViewModel(repo)
        vm.estado.test {
            var estado = awaitItem()
            while (estado.nombre.isEmpty()) estado = awaitItem()
            assertEquals("Ana", estado.nombre)
            assertEquals("O−", estado.etiquetaTipoSangre)
            assertEquals(EstadoDonacion.PuedeDonar(null), estado.estadoDonacion)
            assertFalse(estado.sinConexion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inicio sin conexion avisa y sigue mostrando lo guardado`() = runTest {
        val repo = DonadorFalso(
            perfil = perfilDePrueba(),
            elegibilidad = Elegibilidad(false, false, emptyList(), aviso = "", proximaFechaElegible = "2026-12-01", diasParaVolverADonar = 20),
            respuestaRefresco = Resultado.Fallo(FalloApp.SinConexion),
        )
        val vm = InicioViewModel(repo)
        vm.estado.test {
            var estado = awaitItem()
            while (!estado.sinConexion) estado = awaitItem()
            assertEquals(EstadoDonacion.Esperar(20, LocalDate.of(2026, 12, 1)), estado.estadoDonacion)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
