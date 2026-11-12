package mx.gotadevida.notificaciones.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mx.gotadevida.notificaciones.viewmodel.NotificacionesViewModel

/**
 * Al entrar con sesión: pide permiso de notificaciones (Android 13+) y registra
 * el teléfono en el backend para recibir urgencias.
 */
@Composable
fun RegistroDeAvisos(viewModel: NotificacionesViewModel = hiltViewModel()) {
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.registrar()
    }
}
