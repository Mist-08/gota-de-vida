package mx.gotadevida.hospital.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BarraUnidades
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.BotonTexto
import mx.gotadevida.core.designsystem.componentes.CampoTexto
import mx.gotadevida.core.designsystem.componentes.ChipTipoSangre
import mx.gotadevida.core.designsystem.componentes.EstadoVacio
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.TarjetaUrgencia
import mx.gotadevida.core.designsystem.componentes.texto
import mx.gotadevida.core.designsystem.componentes.textoDistancia
import mx.gotadevida.hospital.entity.EstadoUrgenciaHospital
import mx.gotadevida.hospital.entity.TipoSangrePaciente
import mx.gotadevida.hospital.viewmodel.CrearUrgenciaViewModel
import mx.gotadevida.hospital.viewmodel.DetalleUrgenciaHospitalViewModel
import mx.gotadevida.hospital.viewmodel.PanelHospitalViewModel

@Composable
private fun EstadoUrgenciaHospital.texto(): String = stringResource(
    when (this) {
        EstadoUrgenciaHospital.ACTIVA -> R.string.estado_activa
        EstadoUrgenciaHospital.CUBIERTA -> R.string.estado_cubierta
        EstadoUrgenciaHospital.CERRADA -> R.string.estado_cerrada
        EstadoUrgenciaHospital.CANCELADA -> R.string.estado_cancelada
    },
)

/** Panel del banco de sangre: sus urgencias con el avance en vivo. */
@Composable
fun PanelHospitalScreen(
    alNuevaUrgencia: () -> Unit,
    alAbrir: (String) -> Unit,
    alAjustes: () -> Unit,
    alEscanear: () -> Unit = {},
    viewModel: PanelHospitalViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.actualizar()
            delay(15_000)
        }
    }
    Scaffold(containerColor = GotaTema.colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        ) {
            BarraSuperior {
                IconButton(onClick = alAjustes) { Icono(R.drawable.ic_ajustes, stringResource(R.string.accion_ajustes)) }
            }
            Text(stringResource(R.string.panel_titulo), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            BotonPrincipal(stringResource(R.string.panel_nueva_urgencia), alPulsar = alNuevaUrgencia)
            BotonSecundario(stringResource(R.string.panel_escanear), alPulsar = alEscanear)
            estado.estadisticas?.let { e ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    Dato(e.urgenciasActivas.toString(), stringResource(R.string.panel_activas), Modifier.weight(1f))
                    Dato(e.donacionesConfirmadas.toString(), stringResource(R.string.panel_donaciones), Modifier.weight(1f))
                    Dato(e.minutosPromedioParaCubrir?.let { "$it" } ?: "—", stringResource(R.string.panel_minutos), Modifier.weight(1f))
                }
                if (e.tiposMasSolicitados.isNotEmpty()) {
                    Text(
                        stringResource(R.string.panel_mas_solicitados, e.tiposMasSolicitados.joinToString { it.etiquetaTipoSangre }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GotaTema.colores.inkMuted,
                    )
                }
            }
            if (estado.citasHoy.isNotEmpty()) {
                Text(stringResource(R.string.panel_citas_hoy), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                estado.citasHoy.forEach { c ->
                    Text("${c.fechaHora.takeLast(5)} · ${c.donadorNombre.orEmpty()}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            estado.fallo?.let { MensajeError(it.texto()) }
            when {
                estado.cargando -> CircularProgressIndicator(color = GotaTema.colores.sangre)
                estado.urgencias.isEmpty() -> EstadoVacio(stringResource(R.string.panel_vacio), R.drawable.ic_campana)
                else -> estado.urgencias.forEach { u ->
                    TarjetaUrgencia(
                        etiquetaTipoSangre = u.etiquetaTipoSangre,
                        titulo = u.estado.texto(),
                        detalle = pluralStringResource(R.plurals.panel_avisados, u.donadoresNotificados ?: 0, u.donadoresNotificados ?: 0),
                        cubiertas = u.unidadesComprometidas,
                        total = u.unidades,
                        alPulsar = { alAbrir(u.id) },
                    )
                }
            }
            Spacer(Modifier.height(Espacio.x4))
        }
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String, modifier: Modifier) {
    val colores = GotaTema.colores
    Column(modifier.background(colores.surfaceSunken, Radio.md).padding(Espacio.x3)) {
        Text(valor, style = MaterialTheme.typography.headlineSmall)
        Text(etiqueta, style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrearUrgenciaScreen(alRegresar: () -> Unit, alPublicar: (String) -> Unit, viewModel: CrearUrgenciaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    LaunchedEffect(estado.creadaId) { estado.creadaId?.let(alPublicar) }
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x6),
        ) {
            BarraSuperior(alRegresar = alRegresar)
            Text(stringResource(R.string.crear_titulo), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.crear_tipo_paciente), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.x2), verticalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    TipoSangrePaciente.entries.forEach { tipo ->
                        FilterChip(
                            selected = estado.tipo == tipo,
                            onClick = { viewModel.onTipo(tipo) },
                            label = { Text(tipo.etiqueta, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.height(48.dp),
                            shape = Radio.completo,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colores.sangre,
                                selectedLabelColor = colores.onSangre,
                            ),
                        )
                    }
                }
                if (estado.faltaTipo) Text(stringResource(R.string.error_elige_opcion), color = colores.error, style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.crear_unidades), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    OutlinedIconButton(onClick = { viewModel.onUnidades(estado.unidades - 1) }, enabled = estado.unidades > 1) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(estado.unidades.toString(), style = MaterialTheme.typography.displaySmall)
                    OutlinedIconButton(onClick = { viewModel.onUnidades(estado.unidades + 1) }, enabled = estado.unidades < CrearUrgenciaViewModel.MAXIMO_UNIDADES) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            CampoTexto(
                valor = estado.nota,
                alCambiar = viewModel::onNota,
                etiqueta = stringResource(R.string.crear_nota),
                ayuda = stringResource(R.string.crear_nota_ayuda),
                tipoTeclado = KeyboardType.Text,
            )
            estado.fallo?.let { MensajeError(it.texto()) }
            BotonPrincipal(stringResource(R.string.crear_publicar), alPulsar = viewModel::publicar, cargando = estado.enviando)
            Text(stringResource(R.string.crear_ayuda_oleadas), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
        }
    }
}

@Composable
fun DetalleUrgenciaHospitalScreen(alRegresar: () -> Unit, viewModel: DetalleUrgenciaHospitalViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.actualizar()
            delay(5_000)
        }
    }
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        ) {
            BarraSuperior(alRegresar = alRegresar, titulo = stringResource(R.string.urgencia_titulo))
            estado.fallo?.let { MensajeError(it.texto()) }
            val u = estado.urgencia ?: run {
                CircularProgressIndicator(color = colores.sangre)
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                ChipTipoSangre(u.etiquetaTipoSangre, destacado = true)
                Text(u.estado.texto(), style = MaterialTheme.typography.headlineSmall)
            }
            BarraUnidades(u.unidadesComprometidas, u.unidades)
            if (u.estado == EstadoUrgenciaHospital.ACTIVA) {
                Text(
                    pluralStringResource(R.plurals.panel_oleada, u.donadoresNotificados ?: 0, u.oleada ?: 0, u.donadoresNotificados ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.inkMuted,
                )
            }
            u.nota?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(stringResource(R.string.panel_aceptaron), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (u.aceptaciones.isEmpty()) EstadoVacio(stringResource(R.string.panel_nadie_acepto), R.drawable.ic_reloj)
            u.aceptaciones.forEach { d ->
                Row(
                    Modifier.fillMaxWidth().background(colores.surfaceRaised, Radio.md).padding(Espacio.x3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x3),
                ) {
                    Icono(R.drawable.ic_persona, null, color = colores.vida)
                    Column(Modifier.weight(1f)) {
                        Text(d.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(listOfNotNull(textoDistancia(d.distanciaKm), d.telefono).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
                    }
                    d.telefono?.let { tel ->
                        TextButton(onClick = { runCatching { contexto.startActivity(Intent(Intent.ACTION_DIAL, "tel:$tel".toUri())) } }) {
                            Text(stringResource(R.string.panel_llamar))
                        }
                    }
                }
            }
            if (u.estado == EstadoUrgenciaHospital.ACTIVA || u.estado == EstadoUrgenciaHospital.CUBIERTA) {
                BotonTexto(stringResource(R.string.panel_cerrar), alPulsar = { viewModel.terminar(EstadoUrgenciaHospital.CERRADA) })
                BotonTexto(stringResource(R.string.panel_cancelar), alPulsar = { viewModel.terminar(EstadoUrgenciaHospital.CANCELADA) }, peligroso = true)
            }
            Spacer(Modifier.size(Espacio.x4))
        }
    }
}
