package mx.gotadevida.autenticacion.service

import mx.gotadevida.core.correo.Correo

/** Textos de los correos para recuperar la contraseña. */
internal object CorreosRecuperacion {

    fun codigo(email: String, nombre: String, codigo: String, minutos: Long) = Correo(
        para = email,
        nombre = nombre,
        asunto = "Tu código para cambiar la contraseña: $codigo",
        html = plantilla(
            nombre,
            """
            <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta de Gota de Vida.</p>
            <p>Escribe este código en la app:</p>
            <p style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#C62828;margin:24px 0">$codigo</p>
            <p>El código vence en $minutos minutos. Si no fuiste tú, ignora este correo: tu contraseña no cambia.</p>
            """.trimIndent()
        ),
        texto = """
            Hola, $nombre.

            Recibimos una solicitud para cambiar la contraseña de tu cuenta de Gota de Vida.
            Tu código es: $codigo

            Vence en $minutos minutos. Si no fuiste tú, ignora este correo: tu contraseña no cambia.
        """.trimIndent()
    )

    fun cuentaDeGoogle(email: String, nombre: String) = Correo(
        para = email,
        nombre = nombre,
        asunto = "Tu cuenta de Gota de Vida entra con Google",
        html = plantilla(
            nombre,
            """
            <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta de Gota de Vida.</p>
            <p>Tu cuenta no tiene contraseña: entras con el botón <b>Continuar con Google</b> en la app.</p>
            <p>Si no fuiste tú, ignora este correo.</p>
            """.trimIndent()
        ),
        texto = """
            Hola, $nombre.

            Recibimos una solicitud para cambiar la contraseña de tu cuenta de Gota de Vida.
            Tu cuenta no tiene contraseña: entras con el botón "Continuar con Google" en la app.

            Si no fuiste tú, ignora este correo.
        """.trimIndent()
    )

    private fun plantilla(nombre: String, cuerpo: String) = """
        <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#212121">
          <h2 style="color:#C62828">Gota de Vida</h2>
          <p>Hola, ${escapar(nombre)}.</p>
          $cuerpo
        </div>
    """.trimIndent()

    private fun escapar(texto: String) = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
