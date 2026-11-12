package mx.gotadevida.core.salud

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import mx.gotadevida.soporte.probarApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
