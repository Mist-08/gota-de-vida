package mx.gotadevida.core.privacidad

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Aviso de privacidad público en `/privacidad`. Es el mismo texto que muestra la app y es la URL
 * que pide Meta para publicar la app de WhatsApp (incluye cómo eliminar los datos en `#eliminar`).
 */
fun Route.privacidadRoutes() {
    get("/privacidad") {
        call.respondText(AVISO_PRIVACIDAD_HTML, ContentType.Text.Html)
    }
}

internal val AVISO_PRIVACIDAD_HTML =
    """
    <!DOCTYPE html>
    <html lang="es">
    <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Aviso de privacidad · Gota de Vida</title>
    <style>
      body { font-family: system-ui, sans-serif; max-width: 680px; margin: 2rem auto; padding: 0 1rem;
             line-height: 1.6; color: #231819; }
      h1 { color: #b3261e; }
      h2 { margin-top: 2rem; }
    </style>
    </head>
    <body>
    <h1>Aviso de privacidad</h1>
    <p>Gota de Vida conecta a los bancos de sangre de la Comarca Lagunera con donadores voluntarios
    compatibles y cercanos cuando hay una urgencia.</p>

    <h2>Qué datos guardamos</h2>
    <p>Tu nombre, correo, celular, tipo de sangre, fecha de nacimiento, peso, sexo para criterios
    médicos y una ubicación aproximada (unos 100 metros, nunca tu dirección).</p>

    <h2>Para qué los usamos</h2>
    <p>Solo para saber si puedes donar y avisarte de urgencias de sangre compatibles cerca de ti, por
    notificación en la app y por WhatsApp. No vendemos ni compartimos tus datos con fines comerciales.</p>

    <h2>Quién los ve</h2>
    <p>Un hospital solo ve qué tan lejos estás. Tu nombre y celular los ve únicamente si aceptas una
    urgencia. Al asistente Gota (Gemini) no se le envían nombres, teléfonos, correos ni ubicaciones.</p>

    <h2 id="eliminar">Cómo corregir o eliminar tus datos</h2>
    <p>Puedes corregir tus datos desde <strong>Ajustes → Editar mi perfil</strong> y borrar tu cuenta
    con todos sus datos desde <strong>Ajustes → Eliminar mi cuenta</strong> en la app.</p>

    <p><small>Este es el aviso del prototipo escolar; antes de usar la app con personas reales debe
    revisarlo un especialista en protección de datos.</small></p>
    </body>
    </html>
    """.trimIndent()
