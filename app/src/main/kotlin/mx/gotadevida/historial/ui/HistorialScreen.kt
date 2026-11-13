package mx.gotadevida.historial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BannerAviso
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.EstadoVacio
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.formatoFecha
import mx.gotadevida.historial.entity.Insignia
import mx.gotadevida.historial.viewmodel.HistorialViewModel

@Composable
private fun Insignia.nombre(): String = stringResource(
    when (this) {
        Insignia.PRIMERA_DONACION -> R.string.insignia_primera
        Insignia.TRES_DONACIONES -> R.string.insignia_tres
        Insignia.CINCO_DONACIONES -> R.string.insignia_cinco
        Insignia.HEROE_DE_URGENCIA -> R.string.insignia_heroe
    }
)

@Composable
fun HistorialScreen(alRegresar: () -> Unit, viewModel: HistorialViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val formato = formatoFecha()
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4)
        ) {
            BarraSuperior(alRegresar = alRegresar, titulo = stringResource(R.string.historial_titulo))
            if (estado.sinConexion) {
                BannerAviso(
                    stringResource(R.string.inicio_sin_conexion),
                    R.drawable.ic_nube_tachada
                )
            }
            val h = estado.historial ?: run {
                if (estado.cargando) CircularProgressIndicator(color = colores.sangre)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                Cifra(h.totalDonaciones.toString(), stringResource(R.string.historial_donaciones), Modifier.weight(1f))
                Cifra(h.vidasQuePuedesAyudar.toString(), stringResource(R.string.historial_vidas), Modifier.weight(1f))
            }
            Text(
                if (h.elegible) {
                    stringResource(R.string.inicio_puedes_donar)
                } else {
                    h.proximaFechaElegible?.let {
                        stringResource(R.string.historial_proxima, LocalDate.parse(it).format(formato))
                    }.orEmpty()
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (h.elegible) colores.vida else colores.aviso
            )
            Text(
                stringResource(R.string.historial_insignias),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            h.insignias.forEach { i ->
                val estadoTexto =
                    stringResource(if (i.obtenida) R.string.insignia_obtenida else R.string.insignia_pendiente)
                val nombre = i.codigo.nombre()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (i.obtenida) colores.vidaSoft else colores.surfaceSunken, Radio.md)
                        .padding(Espacio.x3)
                        .semantics(mergeDescendants = true) { contentDescription = "$nombre: $estadoTexto" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
                ) {
                    Icono(
                        if (i.obtenida) R.drawable.ic_check_circulo else R.drawable.ic_escudo,
                        null,
                        color = if (i.obtenida) colores.vida else colores.inkMuted
                    )
                    Text(nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(estadoTexto, style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
                }
            }
            Text(
                stringResource(R.string.historial_donaciones),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            if (h.donaciones.isEmpty()) EstadoVacio(stringResource(R.string.inicio_sin_donaciones), R.drawable.ic_gota)
            h.donaciones.forEach { d ->
                Column(Modifier.fillMaxWidth().background(colores.surfaceRaised, Radio.md).padding(Espacio.x3)) {
                    Text(LocalDate.parse(d.fecha).format(formato), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (d.porUrgencia) {
                            stringResource(
                                R.string.historial_por_urgencia,
                                d.hospitalNombre
                            )
                        } else {
                            d.hospitalNombre
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.inkMuted
                    )
                }
            }
            Spacer(Modifier.height(Espacio.x4))
        }
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier) {
    val colores = GotaTema.colores
    Column(modifier.background(colores.sangreSoft, Radio.lg).padding(Espacio.x4)) {
        Text(valor, style = MaterialTheme.typography.displayMedium, color = colores.onSangreSoft)
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = colores.onSangreSoft)
    }
}
