package mx.gotadevida

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import mx.gotadevida.core.designsystem.GotaDeVidaTema
import mx.gotadevida.core.navegacion.NavegacionGota
import mx.gotadevida.notificaciones.service.AvisosPush

/**
 * Única actividad. Es AppCompatActivity para que el cambio de idioma desde Ajustes
 * (AppCompatDelegate.setApplicationLocales) funcione también en Android 12 o anterior.
 * Si se abre desde una notificación, trae el id de la urgencia para ir directo a ella.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val solicitudAbierta = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        solicitudAbierta.value = intent?.getStringExtra(AvisosPush.EXTRA_SOLICITUD)
        setContent {
            GotaDeVidaTema(oscuro = isSystemInDarkTheme()) {
                NavegacionGota(
                    solicitudPorAbrir = solicitudAbierta.value,
                    alAbrirSolicitud = { solicitudAbierta.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(AvisosPush.EXTRA_SOLICITUD)?.let { solicitudAbierta.value = it }
    }
}
