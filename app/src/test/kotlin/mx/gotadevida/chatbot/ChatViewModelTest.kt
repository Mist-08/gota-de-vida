package mx.gotadevida.chatbot

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mx.gotadevida.chatbot.entity.Autor
import mx.gotadevida.chatbot.entity.MensajeChat
import mx.gotadevida.chatbot.entity.RespuestaChat
import mx.gotadevida.chatbot.repository.ChatRepository
import mx.gotadevida.chatbot.viewmodel.ChatViewModel
import mx.gotadevida.core.network.FalloApp
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.prueba.ReglaDespachadorPrincipal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatFalso(var sinRed: Boolean = false) : ChatRepository {
    val mensajes = MutableStateFlow<List<MensajeChat>>(emptyList())
    override fun observarMensajes() = mensajes
    override suspend fun refrescar() = Resultado.Exito(Unit)
    override suspend fun enviar(texto: String): Resultado<RespuestaChat> {
        if (sinRed) return Resultado.Fallo(FalloApp.SinConexion)
        val r = RespuestaChat("Hay lugar a las 08:00 o 09:30.", listOf("08:00", "09:30"), "MENU")
        mensajes.value = mensajes.value + MensajeChat(Autor.DONADOR, texto, "") + MensajeChat(Autor.GOTA, r.respuesta, "")
        return Resultado.Exito(r)
    }
}

class ChatViewModelTest {
    @get:Rule val regla = ReglaDespachadorPrincipal()

    @Test
    fun `al enviar se limpia el borrador y llegan las opciones de la respuesta`() = runTest {
        val vm = ChatViewModel(ChatFalso())
        vm.estado.test {
            awaitItem()
            vm.onBorrador("  1  ")
            vm.enviar()
            var e = awaitItem()
            while (e.mensajes.size < 2) e = awaitItem()
            assertEquals("1", e.mensajes.first().texto)
            assertEquals("", e.borrador)
            assertEquals(listOf("08:00", "09:30"), e.opciones)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sin red se guarda el texto para reintentar`() = runTest {
        val vm = ChatViewModel(ChatFalso(sinRed = true))
        vm.estado.test {
            awaitItem()
            vm.enviar("¿Puedo donar?")
            var e = awaitItem()
            while (e.sinEnviar == null) e = awaitItem()
            assertEquals("¿Puedo donar?", e.sinEnviar)
            assertEquals(FalloApp.SinConexion, e.fallo)
            assertTrue(e.mensajes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no se manda un mensaje vacio`() = runTest {
        val repo = ChatFalso()
        ChatViewModel(repo).enviar("   ")
        assertTrue(repo.mensajes.value.isEmpty())
    }
}
