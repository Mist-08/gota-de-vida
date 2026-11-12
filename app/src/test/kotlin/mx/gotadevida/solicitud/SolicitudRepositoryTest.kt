package mx.gotadevida.solicitud

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.sync.AccionPendiente
import mx.gotadevida.core.sync.ResultadoEnvio
import mx.gotadevida.core.sync.enviarPendientes
import mx.gotadevida.prueba.AccionesDaoFalso
import mx.gotadevida.prueba.ColaFalsa
import mx.gotadevida.prueba.SolicitudApiFalsa
import mx.gotadevida.prueba.SolicitudDaoFalso
import mx.gotadevida.prueba.urgenciaDePrueba
import mx.gotadevida.solicitud.repository.EjecutorResponderUrgencia
import mx.gotadevida.solicitud.repository.EnvioRespuesta
import mx.gotadevida.solicitud.repository.SolicitudRepositoryLocalYRed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolicitudRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = SolicitudApiFalsa()
    private val dao = SolicitudDaoFalso()
    private val cola = ColaFalsa()
    private val repo = SolicitudRepositoryLocalYRed(api, dao, cola, json)

    @Test
    fun `las urgencias cercanas quedan guardadas para verlas sin conexion`() = runTest {
        repo.refrescarCercanas()
        api.sinRed = true
        assertTrue(repo.refrescarCercanas() is Resultado.Fallo)
        assertEquals(listOf("u1"), repo.observarCercanas().first().map { it.id })
    }

    @Test
    fun `responder con red se envia y actualiza el contador local`() = runTest {
        repo.refrescarCercanas()
        val r = repo.responder("u1", acepta = true)
        assertEquals(Resultado.Exito(EnvioRespuesta.ENVIADA), r)
        assertEquals(1, repo.observar("u1").first()!!.unidadesComprometidas)
        assertTrue(cola.encoladas.value.isEmpty())
    }

    @Test
    fun `responder sin red queda pendiente de enviar y el worker lo manda al volver la senal`() = runTest {
        repo.refrescarCercanas()
        api.sinRed = true
        assertEquals(Resultado.Exito(EnvioRespuesta.PENDIENTE), repo.responder("u1", acepta = true))
        assertEquals(setOf("u1"), repo.observarPendientes().first())

        // Vuelve la señal: el worker toma lo encolado y lo manda.
        api.sinRed = false
        val acciones = AccionesDaoFalso()
        cola.encoladas.value.forEach { (clave, v) -> acciones.guardar(AccionPendiente(clave, v.first, v.second, 1)) }
        val listo = enviarPendientes(acciones, setOf(EjecutorResponderUrgencia(api, dao, json)))
        assertTrue(listo)
        assertTrue(acciones.datos.isEmpty())
        assertEquals(listOf("u1" to true), api.respuestas)
        assertEquals(true, repo.observar("u1").first()!!.miRespuesta)
    }

    @Test
    fun `si sigue sin red el worker pide reintentar y conserva la accion`() = runTest {
        api.sinRed = true
        val acciones = AccionesDaoFalso()
        acciones.guardar(AccionPendiente("urgencia:u1", "responder_urgencia", """{"solicitudId":"u1","acepta":true}""", 1))
        val ejecutor = EjecutorResponderUrgencia(api, dao, json)
        assertEquals(ResultadoEnvio.REINTENTAR, ejecutor.ejecutar(acciones.datos.values.first().datos))
        assertEquals(false, enviarPendientes(acciones, setOf(ejecutor)))
        assertEquals(1, acciones.datos.getValue("urgencia:u1").intentos)
    }

    @Test
    fun `una urgencia que ya no esta en la lista deja de mostrarse como cercana`() = runTest {
        repo.refrescarCercanas()
        api.lista = listOf(urgenciaDePrueba(id = "u2"))
        repo.refrescarCercanas()
        assertEquals(listOf("u2"), repo.observarCercanas().first().map { it.id })
    }
}
