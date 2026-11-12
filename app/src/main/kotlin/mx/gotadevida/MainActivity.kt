package mx.gotadevida

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import mx.gotadevida.core.designsystem.GotaDeVidaTema
import mx.gotadevida.core.navegacion.NavegacionGota

/**
 * Única actividad. Es AppCompatActivity para que el cambio de idioma desde Ajustes
 * (AppCompatDelegate.setApplicationLocales) funcione también en Android 12 o anterior.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GotaDeVidaTema(oscuro = isSystemInDarkTheme()) {
                NavegacionGota()
            }
        }
    }
}
