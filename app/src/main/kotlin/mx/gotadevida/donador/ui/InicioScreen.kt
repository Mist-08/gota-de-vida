package mx.gotadevida.donador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BannerAviso
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.ChipTipoSangre
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.donador.service.EstadoDonacion
import mx.gotadevida.donador.viewmodel.InicioViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    alAbrirAjustes: () -> Unit,
    alEditarPerfil: () -> Unit,
    urgencias: @Composable () -> Unit = {},
    cita: @Composable () -> Unit = {},
    alHistorial: () -> Unit = {},
    viewModel: InicioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            if (estado.sinConexion) BannerAviso(stringResource(R.string.inicio_sin_conexion), R.drawable.ic_nube_tachada)
            PullToRefreshBox(
                isRefreshing = estado.actualizando,
                onRefresh = viewModel::actualizar,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
                    verticalArrangement = Arrangement.spacedBy(Espacio.x6),
                ) {
                    BarraSuperior {
                        IconButton(onClick = alAbrirAjustes) {
                            Icono(R.drawable.ic_ajustes, stringResource(R.string.accion_ajustes))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.inicio_hola), style = MaterialTheme.typography.bodyLarge, color = colores.inkMuted)
                            Text(
                                estado.nombre,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        estado.etiquetaTipoSangre?.let { tipo ->
                            val descripcion = stringResource(R.string.cd_tipo_sangre, tipo)
                            ChipTipoSangre(tipo, Modifier.semantics { contentDescription = descripcion })
                        }
                    }

                    TarjetaEstado(estado.estadoDonacion, alEditarPerfil)
                    cita()

                    // La lista viene de la funcionalidad `solicitud`; la navegación la conecta aquí.
                    urgencias()

                    if (estado.estadoDonacion !is EstadoDonacion.CompletarPerfil && estado.estadoDonacion !is EstadoDonacion.Cargando) {
                        BotonSecundario(stringResource(R.string.historial_titulo), alPulsar = alHistorial)
                        BotonSecundario(stringResource(R.string.inicio_editar_perfil), alPulsar = alEditarPerfil)
                    }
                    Text(stringResource(R.string.aviso_medico), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
                    Spacer(Modifier.height(Espacio.x4))
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstado(estado: EstadoDonacion, alCompletarPerfil: () -> Unit) {
    val colores = GotaTema.colores
    val formato = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    when (estado) {
        EstadoDonacion.Cargando -> Tarjeta(colores.surfaceSunken) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                CircularProgressIndicator(Modifier.height(20.dp), color = colores.sangre, strokeWidth = 2.dp)
                Text(stringResource(R.string.inicio_cargando), style = MaterialTheme.typography.bodyLarge, color = colores.inkMuted)
            }
        }

        EstadoDonacion.CompletarPerfil -> Tarjeta(colores.sangreSoft) {
            Icono(R.drawable.ic_persona, null, color = colores.onSangreSoft)
            Text(stringResource(R.string.inicio_completa_perfil), style = MaterialTheme.typography.headlineSmall, color = colores.onSangreSoft)
            Text(stringResource(R.string.inicio_completa_perfil_texto), style = MaterialTheme.typography.bodyLarge, color = colores.onSangreSoft)
            BotonPrincipal(stringResource(R.string.accion_completar_perfil), alPulsar = alCompletarPerfil)
        }

        is EstadoDonacion.PuedeDonar -> Tarjeta(colores.vidaSoft) {
            Etiqueta(stringResource(R.string.inicio_elegible), R.drawable.ic_check_circulo, colores.vida)
            Text(stringResource(R.string.inicio_puedes_donar), style = MaterialTheme.typography.headlineMedium, color = colores.onVidaSoft)
            Text(
                estado.ultimaDonacion?.let { stringResource(R.string.inicio_ultima_donacion, it.format(formato)) }
                    ?: stringResource(R.string.inicio_sin_donaciones),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onVidaSoft,
            )
        }

        is EstadoDonacion.Esperar -> Tarjeta(colores.avisoSoft) {
            Etiqueta(stringResource(R.string.inicio_podras_donar), R.drawable.ic_reloj, colores.aviso)
            Text(
                pluralStringResource(R.plurals.inicio_dias, estado.dias.toInt(), estado.dias.toInt()),
                style = MaterialTheme.typography.displayMedium,
                color = colores.onAvisoSoft,
            )
            Text(stringResource(R.string.inicio_a_partir_de, estado.desde.format(formato)), style = MaterialTheme.typography.bodyLarge, color = colores.onAvisoSoft)
        }

        is EstadoDonacion.NoPuede -> Tarjeta(colores.surfaceSunken) {
            Etiqueta(stringResource(R.string.inicio_no_puedes), R.drawable.ic_info, colores.inkMuted)
            estado.motivos.forEach { motivo ->
                Text("• $motivo", style = MaterialTheme.typography.bodyLarge, color = colores.ink)
            }
        }
    }
}

@Composable
private fun Tarjeta(fondo: Color, contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(fondo, Radio.lg).padding(Espacio.x6)) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x3)) { contenido() }
    }
}

@Composable
private fun Etiqueta(texto: String, icono: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
        Icono(icono, null, color = color, tamano = 20.dp)
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
