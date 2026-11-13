"""Fase 5: carpeta 7. Chatbot y WhatsApp. Ejecutar: python3 docs/api/scripts/fase5_chatbot.py"""
from postman import cargar, carpeta, guardar, req

c = cargar()
OK200 = 'pm.test("Responde 200", () => pm.response.to.have.status(200));'
items = [
    req("Pregunta frecuente sin gastar cuota de Gemini", "POST", "/api/v1/chat/mensajes", "tokenDonador",
        {"texto": "¿Tengo que ir en ayunas?"},
        [OK200, "const r = pm.response.json();",
         'pm.test("Sale de preguntas frecuentes", () => pm.expect(r.origen).to.eql("PREGUNTA_FRECUENTE"));',
         'pm.test("Usa las reglas vigentes (4 a 12 horas)", () => pm.expect(r.respuesta).to.include("4 a 12 horas"));']),
    req("Opción del menú: ¿puedo donar?", "POST", "/api/v1/chat/mensajes", "tokenDonador", {"texto": "4"},
        [OK200, "const r = pm.response.json();",
         'pm.test("Contesta sin IA", () => pm.expect(r.origen).to.eql("MENU"));',
         'pm.test("Recuerda que decide el personal médico o da la fecha", () => pm.expect(r.respuesta.length).to.be.above(10));']),
    req("Historial de la conversación", "GET", "/api/v1/chat/mensajes", "tokenDonador", None,
        [OK200, 'pm.test("Guarda los mensajes de ambos", () => pm.expect(pm.response.json().mensajes.length).to.be.at.least(4));']),
    req("Mensaje vacío (400)", "POST", "/api/v1/chat/mensajes", "tokenDonador", {"texto": "   "},
        ['pm.test("Responde 400", () => pm.response.to.have.status(400));']),
    req("El hospital no usa el chat (403)", "POST", "/api/v1/chat/mensajes", "tokenHospital", {"texto": "hola"},
        ['pm.test("Responde 403", () => pm.response.to.have.status(403));']),
    req("Webhook: token de verificación incorrecto (403)", "GET",
        "/api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=incorrecto&hub.challenge=123", None, None,
        ['pm.test("Responde 403", () => pm.response.to.have.status(403));']),
    req("Webhook: mensaje sin firma de Meta (401)", "POST", "/api/v1/webhooks/whatsapp", None,
        {"object": "whatsapp_business_account", "entry": []},
        ['pm.test("Responde 401", () => pm.response.to.have.status(401));']),
]
carpeta(c, "7. Chatbot y WhatsApp", "Gota: preguntas frecuentes, menú sin IA, historial y seguridad del webhook.", items)
guardar(c)
print(len(items), "requests en 7. Chatbot y WhatsApp")
