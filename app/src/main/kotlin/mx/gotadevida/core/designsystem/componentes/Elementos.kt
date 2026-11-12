package mx.gotadevida.core.designsystem.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio

/** Ícono de res/drawable. Si es decorativo, `descripcion` va en null. */
@Composable
fun Icono(
    @DrawableRes id: Int,
    descripcion: String?,
    modifier: Modifier = Modifier,
    color: Color = GotaTema.colores.ink,
    tamano: Dp = 24.dp,
) {
    Icon(painterResource(id), contentDescription = descripcion, modifier = modifier.size(tamano), tint = color)
}

/** Chip de tipo de sangre (O−, A+…). `destacado` = relleno sangre para urgencias. */
@Composable
fun ChipTipoSangre(
    texto: String,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    val colores = GotaTema.colores
    Box(
        modifier = modifier
            .height(32.dp)
            .background(if (destacado) colores.sangre else colores.sangreSoft, Radio.completo)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.headlineSmall.fontFamily),
            color = if (destacado) colores.onSangre else colores.onSangreSoft,
        )
    }
}

/** Barra superior simple: regresar a la izquierda y acciones a la derecha. */
@Composable
fun BarraSuperior(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    alRegresar: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x1),
    ) {
        if (alRegresar != null) {
            IconButton(onClick = alRegresar) {
                Icono(R.drawable.ic_atras, stringResource(R.string.accion_regresar))
            }
        }
        if (titulo != null) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        } else {
            Box(Modifier.weight(1f))
        }
        acciones()
    }
}

/** Aviso en franja (sin conexión, recordatorios). Lo anuncia el lector de pantalla. */
@Composable
fun BannerAviso(
    texto: String,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
) {
    val colores = GotaTema.colores
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.avisoSoft)
            .padding(horizontal = Espacio.x4, vertical = Espacio.x3)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icono(icono, null, color = colores.aviso, tamano = 20.dp)
        Text(texto, style = MaterialTheme.typography.labelLarge, color = colores.aviso)
    }
}

/** Mensaje de error de un formulario o de la red. */
@Composable
fun MensajeError(texto: String, modifier: Modifier = Modifier) {
    val colores = GotaTema.colores
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.surfaceSunken, Radio.md)
            .padding(Espacio.x3)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalArrangement = Arrangement.spacedBy(Espacio.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icono(R.drawable.ic_alerta, null, color = colores.error, tamano = 20.dp)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = colores.error)
    }
}
