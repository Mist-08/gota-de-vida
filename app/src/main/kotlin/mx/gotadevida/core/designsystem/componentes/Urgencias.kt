package mx.gotadevida.core.designsystem.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio

/** Barra "2 de 3 unidades": del diseño de Claude Design (tarjeta de urgencia). */
@Composable
fun BarraUnidades(cubiertas: Int, total: Int, modifier: Modifier = Modifier) {
    val colores = GotaTema.colores
    val texto = pluralStringResource(R.plurals.unidades_cubiertas, total, cubiertas, total)
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = texto
        },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(8.dp)) {
            repeat(total.coerceIn(1, 20)) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (i < cubiertas) colores.vida else colores.surfaceSunken, Radio.completo)
                )
            }
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
    }
}

/** Tarjeta de una urgencia en la lista del donador o del hospital. */
@Composable
fun TarjetaUrgencia(
    etiquetaTipoSangre: String,
    titulo: String,
    detalle: String,
    cubiertas: Int,
    total: Int,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    insignia: String? = null
) {
    val colores = GotaTema.colores
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colores.surfaceRaised, Radio.lg)
            .border(1.dp, colores.outline, Radio.lg)
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(Espacio.x4),
        verticalArrangement = Arrangement.spacedBy(Espacio.x3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
            ChipTipoSangre(etiquetaTipoSangre, destacado = true)
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
            }
        }
        if (insignia != null) {
            Text(
                insignia,
                style = MaterialTheme.typography.labelLarge,
                color = colores.aviso,
                modifier = Modifier.background(
                    colores.avisoSoft,
                    Radio.completo
                ).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        BarraUnidades(cubiertas, total)
    }
}

/** Estado vacío con ícono, usado en listas sin datos. */
@Composable
fun EstadoVacio(texto: String, icono: Int, modifier: Modifier = Modifier) {
    val colores = GotaTema.colores
    Row(
        modifier = modifier.fillMaxWidth().background(colores.surfaceSunken, Radio.lg).padding(Espacio.x4),
        horizontalArrangement = Arrangement.spacedBy(Espacio.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icono(icono, null, color = colores.inkMuted, tamano = 20.dp)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
    }
}

@Composable
fun textoDistancia(km: Double?): String? = km?.let { stringResource(R.string.distancia_km, it) }
