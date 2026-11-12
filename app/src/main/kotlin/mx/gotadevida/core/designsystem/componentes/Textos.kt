package mx.gotadevida.core.designsystem.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mx.gotadevida.R
import mx.gotadevida.core.network.FalloApp

/** Texto para mostrar un fallo de red o del servidor. */
@Composable
fun FalloApp.texto(): String = when (this) {
    is FalloApp.Servidor -> mensaje ?: stringResource(R.string.error_desconocido)
    FalloApp.SinConexion -> stringResource(R.string.error_sin_conexion)
    FalloApp.Desconocido -> stringResource(R.string.error_desconocido)
}
