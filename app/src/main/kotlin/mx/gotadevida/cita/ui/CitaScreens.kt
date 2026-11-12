package mx.gotadevida.cita.ui

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import mx.gotadevida.R
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.service.CodigoQr
import mx.gotadevida.cita.service.HorarioCita
import mx.gotadevida.cita.viewmodel.AgendarViewModel
import mx.gotadevida.cita.viewmodel.MiCitaViewModel
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BannerAviso
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.BotonTexto
import mx.gotadevida.core.designsystem.componentes.EstadoVacio
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto

private val formatoDia = DateTimeFormatter.ofPattern("EEE d")
private val formatoLargo = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendarScreen(alRegresar: () -> Unit, alAgendar: () -> Unit, viewModel: AgendarViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    LaunchedEffect(estado.resultado) { if (estado.resultado != null) alAgendar() }
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4)
        ) {
            BarraSuperior(alRegresar = alRegresar)
            Text(
                stringResource(R.string.agendar_titulo),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Text(estado.hospitalNombre, style = MaterialTheme.typography.titleMedium, color = colores.inkMuted)
            Text(stringResource(R.string.agendar_dia), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacio.x2),
                verticalArrangement = Arrangement.spacedBy(Espacio.x2)
            ) {
                estado.dias.forEach { dia ->
                    FilterChip(
                        selected = dia == estado.dia,
                        onClick = { viewModel.onDia(dia) },
                        label = { Text(dia.format(formatoDia)) },
                        shape = Radio.completo,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colores.sangre,
                            selectedLabelColor = colores.onSangre
                        )
                    )
                }
            }
            Text(stringResource(R.string.agendar_hora), style = MaterialTheme.typography.labelLarge)
            if (estado.cargandoHorarios) CircularProgressIndicator(color = colores.sangre)
            val libres = estado.horarios.filter { it.disponible }
            if (!estado.cargandoHorarios && libres.isEmpty() && estado.fallo == null) {
                EstadoVacio(stringResource(R.string.agendar_sin_horarios), R.drawable.ic_reloj)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espacio.x2),
                verticalArrangement = Arrangement.spacedBy(Espacio.x2)
            ) {
                libres.forEach { h ->
                    FilterChip(
                        selected = h.hora == estado.hora,
                        onClick = { viewModel.onHora(h.hora) },
                        label = { Text(h.hora, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.height(48.dp),
                        shape = Radio.completo,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colores.sangre,
                            selectedLabelColor = colores.onSangre
                        )
                    )
                }
            }
            estado.requisitos?.let { r ->
                Column(
                    Modifier.fillMaxWidth().background(colores.surfaceSunken, Radio.lg).padding(Espacio.x4),
                    verticalArrangement = Arrangement.spacedBy(Espacio.x2)
                ) {
                    Text(stringResource(R.string.agendar_requisitos), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.requisito_ayuno, r.horasAyunoMinimo, r.horasAyunoMaximo),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(stringResource(R.string.requisito_comida), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.requisito_identificacion), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.requisito_edad_peso,
                            r.edadMinima,
                            r.edadMaxima,
                            r.pesoMinimoKg.toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            estado.fallo?.let { MensajeError(it.texto()) }
            BotonPrincipal(
                stringResource(R.string.agendar_confirmar),
                alPulsar = viewModel::confirmar,
                habilitado = estado.hora != null,
                cargando = estado.enviando
            )
            Text(
                stringResource(R.string.aviso_medico),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.inkMuted
            )
            Spacer(Modifier.height(Espacio.x4))
        }
    }
}

/** Tarjeta del inicio con la próxima cita (si hay). */
@Composable
fun TarjetaMiCita(alAbrir: () -> Unit, viewModel: MiCitaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val cita = estado.cita
    when {
        cita != null -> Row(
            Modifier
                .fillMaxWidth()
                .background(colores.vidaSoft, Radio.lg)
                .clickable(role = Role.Button, onClick = alAbrir)
                .padding(Espacio.x4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
        ) {
            Icono(R.drawable.ic_reloj, null, color = colores.vida)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.cita_tu_cita),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onVidaSoft
                )
                Text(
                    "${LocalDateTime.parse(cita.fechaHora).format(formatoLargo)} · ${cita.hospitalNombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onVidaSoft
                )
            }
            Text(
                stringResource(R.string.cita_ver_qr),
                style = MaterialTheme.typography.labelLarge,
                color = colores.vida
            )
        }

        estado.pendiente -> BannerAviso(stringResource(R.string.cita_pendiente), R.drawable.ic_nube_tachada)
    }
}

@Composable
fun MiCitaScreen(alRegresar: () -> Unit, viewModel: MiCitaViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val contexto = LocalContext.current
    val tituloEvento = stringResource(R.string.cita_evento_titulo)
    val descripcionEvento = stringResource(R.string.cita_evento_descripcion)
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BarraSuperior(alRegresar = alRegresar, titulo = stringResource(R.string.cita_titulo))
            if (estado.pendiente) BannerAviso(stringResource(R.string.cita_pendiente), R.drawable.ic_nube_tachada)
            val cita = estado.cita ?: run {
                if (!estado.pendiente) EstadoVacio(stringResource(R.string.cita_sin_cita), R.drawable.ic_reloj)
                return@Column
            }
            Text(
                stringResource(R.string.cita_muestra_qr),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.inkMuted
            )
            CodigoQrCita(cita)
            Text(
                cita.hospitalNombre,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Text(LocalDateTime.parse(cita.fechaHora).format(formatoLargo), style = MaterialTheme.typography.titleMedium)
            Text(cita.hospitalDireccion, style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
            BotonSecundario(stringResource(R.string.cita_agregar_calendario), alPulsar = {
                val intento = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, tituloEvento)
                    .putExtra(
                        CalendarContract.Events.EVENT_LOCATION,
                        "${cita.hospitalNombre}, ${cita.hospitalDireccion}"
                    )
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, HorarioCita.inicioMillis(cita.fechaHora))
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, HorarioCita.finMillis(cita.fechaHora))
                    .putExtra(CalendarContract.Events.DESCRIPTION, descripcionEvento)
                runCatching { contexto.startActivity(intento) }
            })
            BotonSecundario(stringResource(R.string.urgencia_abrir_ruta), alPulsar = {
                val uri = "geo:0,0?q=${cita.latitud},${cita.longitud}(${android.net.Uri.encode(
                    cita.hospitalNombre
                )})".toUri()
                runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            })
            estado.fallo?.let { MensajeError(it.texto()) }
            BotonTexto(stringResource(R.string.cita_cancelar), alPulsar = viewModel::cancelar, peligroso = true)
            Spacer(Modifier.height(Espacio.x4))
        }
    }
}

@Composable
private fun CodigoQrCita(cita: Cita) {
    val contenido = cita.contenidoQr ?: return
    val imagen = remember(contenido) { CodigoQr.bitmap(contenido).asImageBitmap() }
    Image(
        bitmap = imagen,
        contentDescription = stringResource(R.string.cd_codigo_qr),
        modifier = Modifier
            .size(240.dp)
            .background(androidx.compose.ui.graphics.Color.White, Radio.md)
            .border(1.dp, GotaTema.colores.outline, Radio.md)
            .padding(Espacio.x3)
    )
    Spacer(Modifier.width(1.dp))
}

/** Lo usa el detalle de una urgencia aceptada para ir a agendar. */
@Composable
fun BotonAgendar(alPulsar: () -> Unit) = BotonPrincipal(stringResource(R.string.agendar_titulo), alPulsar = alPulsar)
