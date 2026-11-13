package mx.gotadevida.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores del sistema de diseño "Gota de Vida".
 * Los nombres son los mismos que en el diseño para que diseño y código hablen igual.
 */
@Immutable
data class ColoresGota(
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkMuted: Color,
    val outline: Color,
    val outlineStrong: Color,
    val sangre: Color,
    val onSangre: Color,
    val sangreSoft: Color,
    val onSangreSoft: Color,
    val vida: Color,
    val vidaSoft: Color,
    val onVidaSoft: Color,
    val aviso: Color,
    val avisoSoft: Color,
    val onAvisoSoft: Color,
    val info: Color,
    val error: Color,
    val esOscuro: Boolean
)

val ColoresClaros = ColoresGota(
    surface = Color(0xFFFFFBFA),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF6EDEC),
    ink = Color(0xFF231819),
    inkMuted = Color(0xFF6A5657),
    outline = Color(0xFFDCCBCB),
    outlineStrong = Color(0xFF8C7778),
    sangre = Color(0xFFB4161F),
    onSangre = Color(0xFFFFFFFF),
    sangreSoft = Color(0xFFFCE4E2),
    onSangreSoft = Color(0xFF6E0B10),
    vida = Color(0xFF1C7547),
    vidaSoft = Color(0xFFDDF3E6),
    onVidaSoft = Color(0xFF123D27),
    aviso = Color(0xFF8A5300),
    avisoSoft = Color(0xFFFFF0D6),
    onAvisoSoft = Color(0xFF4A3000),
    info = Color(0xFF1E5AA8),
    error = Color(0xFF9C1C5A),
    esOscuro = false
)

val ColoresOscuros = ColoresGota(
    surface = Color(0xFF151011),
    surfaceRaised = Color(0xFF1F1819),
    surfaceSunken = Color(0xFF0F0B0C),
    ink = Color(0xFFF5ECEB),
    inkMuted = Color(0xFFBCA9AA),
    outline = Color(0xFF463839),
    outlineStrong = Color(0xFF8F7B7C),
    sangre = Color(0xFFFF8C85),
    onSangre = Color(0xFF410507),
    sangreSoft = Color(0xFF5A1518),
    onSangreSoft = Color(0xFFFFD9D6),
    vida = Color(0xFF73D49E),
    vidaSoft = Color(0xFF123D27),
    onVidaSoft = Color(0xFFDDF3E6),
    aviso = Color(0xFFF3BF5C),
    avisoSoft = Color(0xFF4A3000),
    onAvisoSoft = Color(0xFFFFF0D6),
    info = Color(0xFF94BEF5),
    error = Color(0xFFFF9BC6),
    esOscuro = true
)

internal val LocalColoresGota = staticCompositionLocalOf { ColoresClaros }

/** Traduce los tokens a los roles de Material 3. */
internal fun ColoresGota.aEsquemaMaterial(): ColorScheme {
    val base = if (esOscuro) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = sangre,
        onPrimary = onSangre,
        primaryContainer = sangreSoft,
        onPrimaryContainer = onSangreSoft,
        secondary = vida,
        onSecondary = surface,
        secondaryContainer = vidaSoft,
        onSecondaryContainer = onVidaSoft,
        tertiary = aviso,
        onTertiary = surface,
        tertiaryContainer = avisoSoft,
        onTertiaryContainer = onAvisoSoft,
        error = error,
        onError = surface,
        background = surface,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = if (esOscuro) surfaceSunken else surfaceRaised,
        surfaceContainerLow = surfaceRaised,
        surfaceContainer = surfaceSunken,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceSunken,
        outline = outlineStrong,
        outlineVariant = outline
    )
}
