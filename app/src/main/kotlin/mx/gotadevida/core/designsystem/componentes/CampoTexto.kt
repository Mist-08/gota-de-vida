package mx.gotadevida.core.designsystem.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio

/**
 * Campo de texto con la etiqueta arriba (como en el diseño) y texto de ayuda o error abajo.
 * El error se muestra en magenta y con texto, nunca solo con color.
 */
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionTeclado: ImeAction = ImeAction.Next,
    alConfirmar: (() -> Unit)? = null,
    esContrasena: Boolean = false,
    habilitado: Boolean = true
) {
    val colores = GotaTema.colores
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = colores.ink)
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado,
            singleLine = true,
            isError = error != null,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = Radio.sm,
            visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionTeclado),
            keyboardActions = KeyboardActions(onDone = { alConfirmar?.invoke() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colores.surfaceSunken,
                unfocusedContainerColor = colores.surfaceSunken,
                disabledContainerColor = colores.surfaceSunken,
                errorContainerColor = colores.surfaceSunken,
                focusedBorderColor = colores.sangre,
                unfocusedBorderColor = colores.outlineStrong,
                errorBorderColor = colores.error,
                cursorColor = colores.sangre
            )
        )
        val texto = error ?: ayuda
        if (texto != null) {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) colores.error else colores.inkMuted
            )
        }
    }
}
