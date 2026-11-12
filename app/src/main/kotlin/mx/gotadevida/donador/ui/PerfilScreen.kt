package mx.gotadevida.donador.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.CampoTexto
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.entity.TipoSangre
import mx.gotadevida.donador.service.CampoPerfil
import mx.gotadevida.donador.service.ErrorPerfil
import mx.gotadevida.donador.service.UbicacionAproximada
import mx.gotadevida.donador.service.ZonasLaguna
import mx.gotadevida.donador.viewmodel.PerfilViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(
    esRegistro: Boolean,
    alRegresar: () -> Unit,
    alGuardar: () -> Unit,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val datos = estado.datos
    LaunchedEffect(estado.guardado) { if (estado.guardado) alGuardar() }

    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var ubicacionNoDisponible by rememberSaveable { mutableStateOf(false) }
    val pedirUbicacion = {
        alcance.launch {
            val ubicacion = UbicacionAproximada.obtener(contexto)
            if (ubicacion != null) {
                ubicacionNoDisponible = false
                viewModel.onUbicacionGps(ubicacion.latitude, ubicacion.longitude)
            } else {
                ubicacionNoDisponible = true
            }
        }
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) pedirUbicacion() else ubicacionNoDisponible = true
    }

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x6),
        ) {
            BarraSuperior(alRegresar = alRegresar) {
                if (esRegistro) Text(stringResource(R.string.perfil_paso), style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                Text(
                    stringResource(if (esRegistro) R.string.perfil_titulo else R.string.perfil_titulo_editar),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(stringResource(R.string.perfil_subtitulo), style = MaterialTheme.typography.bodyLarge, color = colores.inkMuted)
            }

            // Tipo de sangre
            Seccion(stringResource(R.string.perfil_tipo_sangre), estado.errores[CampoPerfil.TIPO_SANGRE]) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.x2), verticalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    TipoSangre.entries.forEach { tipo ->
                        Opcion(tipo.etiqueta, seleccionada = datos.tipoSangre == tipo, alPulsar = { viewModel.onTipoSangre(tipo) }, grande = true)
                    }
                    Opcion(stringResource(R.string.perfil_no_se_tipo), seleccionada = datos.noSeTipo, alPulsar = viewModel::onNoSeTipo)
                }
                if (datos.noSeTipo) Ayuda(stringResource(R.string.perfil_no_se_tipo_ayuda))
            }

            // Fecha de nacimiento y peso
            Seccion(stringResource(R.string.perfil_nacimiento), estado.errores[CampoPerfil.NACIMIENTO]) {
                SelectorFecha(datos.fechaNacimiento, alElegir = viewModel::onNacimiento)
            }
            CampoTexto(
                valor = datos.peso,
                alCambiar = viewModel::onPeso,
                etiqueta = stringResource(R.string.perfil_peso),
                error = estado.errores[CampoPerfil.PESO]?.texto(),
                tipoTeclado = KeyboardType.Decimal,
                accionTeclado = ImeAction.Done,
            )

            // Sexo para criterios médicos
            Seccion(stringResource(R.string.perfil_sexo), estado.errores[CampoPerfil.SEXO]) {
                val opciones = listOf(
                    SexoParaCriteriosMedicos.FEMENINO to R.string.sexo_femenino,
                    SexoParaCriteriosMedicos.MASCULINO to R.string.sexo_masculino,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    opciones.forEachIndexed { i, (sexo, texto) ->
                        SegmentedButton(
                            selected = datos.sexo == sexo,
                            onClick = { viewModel.onSexo(sexo) },
                            shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colores.sangreSoft,
                                activeContentColor = colores.onSangreSoft,
                                activeBorderColor = colores.outlineStrong,
                                inactiveBorderColor = colores.outlineStrong,
                            ),
                        ) { Text(stringResource(texto)) }
                    }
                }
                Ayuda(stringResource(R.string.perfil_sexo_ayuda))
            }

            // Zona
            Seccion(stringResource(R.string.perfil_zona), estado.errores[CampoPerfil.UBICACION]) {
                OutlinedButton(
                    onClick = {
                        if (UbicacionAproximada.tienePermiso(contexto)) pedirUbicacion() else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = Radio.md,
                    border = BorderStroke(1.5.dp, if (estado.ubicacionPorGps) colores.vida else colores.outlineStrong),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                        Icono(
                            if (estado.ubicacionPorGps) R.drawable.ic_check_circulo else R.drawable.ic_ubicacion,
                            null,
                            color = if (estado.ubicacionPorGps) colores.vida else colores.sangre,
                            tamano = 20.dp,
                        )
                        Text(
                            stringResource(if (estado.ubicacionPorGps) R.string.perfil_ubicacion_gps else R.string.perfil_usar_ubicacion),
                            color = colores.ink,
                        )
                    }
                }
                if (ubicacionNoDisponible) Ayuda(stringResource(R.string.perfil_ubicacion_no_disponible))
                Ayuda(stringResource(R.string.perfil_o_elige_municipio))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.x2), verticalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    ZonasLaguna.lista.forEach { zona ->
                        Opcion(zona.nombre, seleccionada = !estado.ubicacionPorGps && estado.zona == zona.nombre, alPulsar = { viewModel.onZona(zona) })
                    }
                }
                Ayuda(stringResource(R.string.perfil_zona_ayuda))
            }

            // Donaciones previas
            Seccion(stringResource(R.string.perfil_ha_donado), estado.errores[CampoPerfil.ULTIMA_DONACION]) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    Opcion(stringResource(R.string.perfil_si), seleccionada = datos.haDonado == true, alPulsar = { viewModel.onHaDonado(true) })
                    Opcion(stringResource(R.string.perfil_no), seleccionada = datos.haDonado == false, alPulsar = { viewModel.onHaDonado(false) })
                }
                if (datos.haDonado == true) {
                    Text(stringResource(R.string.perfil_ultima_donacion), style = MaterialTheme.typography.labelLarge)
                    SelectorFecha(datos.fechaUltimaDonacion, alElegir = viewModel::onUltimaDonacion)
                }
            }

            estado.fallo?.let { MensajeError(it.texto()) }
            BotonPrincipal(stringResource(R.string.perfil_guardar), alPulsar = viewModel::guardar, cargando = estado.guardando)
            androidx.compose.foundation.layout.Spacer(Modifier.height(Espacio.x4))
        }
    }
}

@Composable
private fun Seccion(titulo: String, error: ErrorPerfil?, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        contenido()
        if (error != null) Text(error.texto(), style = MaterialTheme.typography.bodyMedium, color = GotaTema.colores.error)
    }
}

@Composable
private fun Ayuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = GotaTema.colores.inkMuted)
}

/** Opción seleccionable en forma de píldora (tipos de sangre, municipios, sí/no). */
@Composable
private fun Opcion(texto: String, seleccionada: Boolean, alPulsar: () -> Unit, grande: Boolean = false) {
    val colores = GotaTema.colores
    FilterChip(
        selected = seleccionada,
        onClick = alPulsar,
        label = {
            Text(
                texto,
                style = if (grande) MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.headlineSmall.fontFamily) else MaterialTheme.typography.labelLarge,
            )
        },
        modifier = Modifier.height(48.dp),
        shape = Radio.completo,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colores.sangre,
            selectedLabelColor = colores.onSangre,
            labelColor = colores.ink,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = seleccionada,
            borderColor = colores.outlineStrong,
            selectedBorderColor = colores.sangre,
            borderWidth = 1.5.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(fecha: LocalDate?, alElegir: (LocalDate) -> Unit) {
    val colores = GotaTema.colores
    var abierto by rememberSaveable { mutableStateOf(false) }
    val formato = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    OutlinedButton(
        onClick = { abierto = true },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = Radio.sm,
        border = BorderStroke(1.5.dp, colores.outlineStrong),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
            Icono(R.drawable.ic_reloj, null, color = colores.inkMuted, tamano = 20.dp)
            Text(fecha?.format(formato) ?: stringResource(R.string.perfil_elegir_fecha), color = if (fecha == null) colores.inkMuted else colores.ink)
        }
    }
    if (abierto) {
        val hoyMillis = remember { LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= hoyMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    abierto = false
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        ) {
            DatePicker(state = estado)
        }
    }
}

@Composable
private fun ErrorPerfil.texto(): String = stringResource(
    when (this) {
        ErrorPerfil.REQUERIDO -> R.string.error_elige_opcion
        ErrorPerfil.PESO_INVALIDO -> R.string.error_peso_invalido
        ErrorPerfil.FECHA_FUTURA -> R.string.error_fecha_futura
        ErrorPerfil.EDAD_INVALIDA -> R.string.error_edad_invalida
    },
)
