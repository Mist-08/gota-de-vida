package mx.gotadevida.core.designsystem.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import mx.gotadevida.R
import mx.gotadevida.core.network.FalloApp

/** Texto para mostrar un fallo de red o del servidor. */
@Composable
fun FalloApp.texto(): String = when (this) {
    is FalloApp.Servidor -> mensaje ?: stringResource(R.string.error_desconocido)
    FalloApp.SinConexion -> stringResource(R.string.error_sin_conexion)
    FalloApp.Desconocido -> stringResource(R.string.error_desconocido)
}

/**
 * Idioma con el que se está dibujando la pantalla (el que eligió la persona en Ajustes).
 * Los formatos de fecha se crean con él: un formato guardado en una variable global
 * se quedaría con el idioma que tenía la app al arrancar.
 */
@Composable
fun idiomaActual(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun formatoFecha(estilo: FormatStyle = FormatStyle.LONG): DateTimeFormatter {
    val idioma = idiomaActual()
    return remember(idioma, estilo) { DateTimeFormatter.ofLocalizedDate(estilo).withLocale(idioma) }
}

@Composable
fun formatoFechaHora(): DateTimeFormatter {
    val idioma = idiomaActual()
    return remember(idioma) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(idioma) }
}

@Composable
fun formatoPatron(patron: String): DateTimeFormatter {
    val idioma = idiomaActual()
    return remember(idioma, patron) { DateTimeFormatter.ofPattern(patron, idioma) }
}
