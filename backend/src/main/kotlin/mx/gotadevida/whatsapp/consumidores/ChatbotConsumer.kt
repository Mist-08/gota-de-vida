package mx.gotadevida.whatsapp.consumidores

import kotlinx.serialization.json.jsonPrimitive
import mx.gotadevida.chatbot.service.AsistenteService
import mx.gotadevida.core.dominio.DirectorioDonadores
import mx.gotadevida.core.mensajeria.Consumidor
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.whatsapp.service.ClienteWhatsApp

/** ChatbotConsumer: procesa con Gota cada mensaje de WhatsApp y contesta por WhatsApp. */
fun consumidorChatbot(asistente: AsistenteService, donadores: DirectorioDonadores, whatsapp: ClienteWhatsApp) =
    Consumidor("chatbot", listOf(TiposEvento.WHATSAPP_MENSAJE_RECIBIDO)) { evento ->
        val telefono = evento.datos.getValue("telefono").jsonPrimitive.content
        val texto = evento.datos.getValue("texto").jsonPrimitive.content
        val donador = donadores.porTelefono(telefono)
        if (donador == null) {
            whatsapp.enviarTexto(
                telefono,
                "Hola, soy Gota 🩸 de Gota de Vida. No encontré tu número. Regístrate en la app con este celular para recibir urgencias y agendar tu cita."
            )
        } else {
            val r = asistente.responder(donador.id, texto)
            whatsapp.enviarTexto(telefono, r.respuesta, r.opciones.filter { it.length <= 20 }.take(3))
        }
    }
