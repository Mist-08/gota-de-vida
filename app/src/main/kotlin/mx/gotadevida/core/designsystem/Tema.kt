package mx.gotadevida.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** Espaciados de 4 dp (space-1 … space-8 en TOKENS.md). */
object Espacio {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x6 = 24.dp
    val x8 = 32.dp
}

/** Radios: sm 8 (campos), md 16 (tarjetas y botones), lg 28 (hojas y tarjeta de estado), full (chips). */
object Radio {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(28.dp)
    val completo = RoundedCornerShape(percent = 50)
}

private val FormasGota = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema de la app. Sin color dinámico de Material You: la marca se ve igual en todos los teléfonos.
 */
@Composable
fun GotaDeVidaTema(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    val colores = if (oscuro) ColoresOscuros else ColoresClaros
    CompositionLocalProvider(LocalColoresGota provides colores) {
        MaterialTheme(
            colorScheme = colores.aEsquemaMaterial(),
            typography = TipografiaGota,
            shapes = FormasGota,
            content = contenido,
        )
    }
}

/** Acceso a los colores propios que Material 3 no tiene (vida, aviso, info…). */
object GotaTema {
    val colores: ColoresGota
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresGota.current
}
