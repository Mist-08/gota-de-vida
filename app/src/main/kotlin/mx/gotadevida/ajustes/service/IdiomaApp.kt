package mx.gotadevida.ajustes.service

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Idiomas de la app (res/values = español, res/values-en = inglés). */
enum class Idioma(val etiqueta: String) { ESPANOL("es"), INGLES("en") }

/**
 * Idioma por app de Android: se guarda solo y en Android 13+ también aparece en
 * Ajustes del sistema → Apps → Gota de Vida → Idioma.
 */
object IdiomaApp {
    fun actual(): Idioma {
        val elegido = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: LocaleListCompat.getAdjustedDefault()[0]?.language
        return if (elegido == "en") Idioma.INGLES else Idioma.ESPANOL
    }

    fun cambiar(idioma: Idioma) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idioma.etiqueta))
    }
}
