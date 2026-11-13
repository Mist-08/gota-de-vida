package mx.gotadevida.core.salud

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mx.gotadevida.soporte.probarApi

class SaludRoutesTest {

    @Test
    fun `health responde ok`() = probarApi { cliente, _ ->
        val respuesta = cliente.get("/health")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertTrue(respuesta.bodyAsText().contains("\"estado\":\"ok\""))
    }

    @Test
    fun `swagger esta disponible`() = probarApi { cliente, _ ->
        assertEquals(HttpStatusCode.OK, cliente.get("/swagger").status)
    }

    @Test
    fun `aviso de privacidad es publico`() = probarApi { cliente, _ ->
        val respuesta = cliente.get("/privacidad")

        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertTrue(respuesta.bodyAsText().contains("Aviso de privacidad"))
    }
}
