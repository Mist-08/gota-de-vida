package mx.gotadevida.core.designsystem.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio

/** La acción principal de la pantalla (una sola por pantalla), en rojo sangre. */
@Composable
fun BotonPrincipal(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false
) {
    val colores = GotaTema.colores
    Button(
        onClick = alPulsar,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = Radio.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = colores.sangre,
            contentColor = colores.onSangre,
            disabledContainerColor = colores.surfaceSunken,
            disabledContentColor = colores.inkMuted
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colores.inkMuted
                )
            }
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize)
            )
        }
    }
}

/** Acción secundaria: borde visible, sin relleno. */
@Composable
fun BotonSecundario(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val colores = GotaTema.colores
    OutlinedButton(
        onClick = alPulsar,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = Radio.md,
        border = BorderStroke(1.5.dp, colores.outlineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.ink)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize)
        )
    }
}

/** Acción de bajo peso, como "Ahora no puedo" o "Cerrar sesión". */
@Composable
fun BotonTexto(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, peligroso: Boolean = false) {
    val colores = GotaTema.colores
    TextButton(
        onClick = alPulsar,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = if (peligroso) colores.error else colores.ink)
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}
