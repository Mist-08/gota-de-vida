package mx.gotadevida.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mx.gotadevida.R

/** Bricolage Grotesque: números y encabezados. Figtree: todo lo demás. Ambas con licencia OFL. */
val Bricolage = FontFamily(
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

/** Cifras de ancho fijo para que "1 de 3" o "23 días" no brinquen al cambiar. */
private const val CIFRAS_TABULARES = "tnum"

/**
 * Estilos del sistema de diseño mapeados a Material 3:
 * numeral → displayMedium, display → headlineLarge, headline → headlineSmall, title → titleMedium,
 * body → bodyLarge, body-sm → bodyMedium, label → labelLarge, caption → labelSmall.
 */
val TipografiaGota: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold),
        displayMedium = TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = CIFRAS_TABULARES,
        ),
        displaySmall = base.displaySmall.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = base.headlineMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = base.titleSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Figtree),
        labelLarge = TextStyle(
            fontFamily = Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = base.labelMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(
            fontFamily = Figtree,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        ),
    )
}
