package mx.gotadevida.ajustes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.gotadevida.BuildConfig
import mx.gotadevida.R
import mx.gotadevida.ajustes.service.Idioma
import mx.gotadevida.ajustes.viewmodel.AjustesViewModel
import mx.gotadevida.autenticacion.ui.DialogoAvisoPrivacidad
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonTexto
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto

@Composable
fun AjustesScreen(alRegresar: () -> Unit, alEditarPerfil: (() -> Unit)?, viewModel: AjustesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    var verAviso by rememberSaveable { mutableStateOf(false) }
    var confirmarCierre by rememberSaveable { mutableStateOf(false) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    val esDonador = alEditarPerfil != null
    LaunchedEffect(esDonador) { if (esDonador) viewModel.cargarPreferencias() }

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4)
        ) {
            BarraSuperior(alRegresar = alRegresar)
            Text(
                stringResource(R.string.ajustes_titulo),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics {
                    heading()
                }
            )

            Titulo(stringResource(R.string.ajustes_idioma))
            Column(Modifier.selectableGroup()) {
                listOf(
                    Idioma.ESPANOL to R.string.idioma_espanol,
                    Idioma.INGLES to R.string.idioma_ingles
                ).forEach { (idioma, texto) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = estado.idioma == idioma, role = Role.RadioButton) {
                                viewModel.cambiarIdioma(idioma)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
                    ) {
                        RadioButton(
                            selected = estado.idioma == idioma,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colores.sangre)
                        )
                        Text(stringResource(texto), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            HorizontalDivider(color = colores.outline)

            estado.notificaciones?.let { activas ->
                Titulo(stringResource(R.string.ajustes_notificaciones))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .toggleable(value = activas, role = Role.Switch, onValueChange = viewModel::cambiarNotificaciones),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ajustes_recibir_urgencias), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.ajustes_recibir_urgencias_ayuda),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.inkMuted
                        )
                    }
                    Switch(
                        checked = activas,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = colores.sangre)
                    )
                }
                HorizontalDivider(color = colores.outline)
            }

            Titulo(stringResource(R.string.ajustes_privacidad))
            alEditarPerfil?.let { Opcion(R.drawable.ic_persona, stringResource(R.string.ajustes_editar_perfil), it) }
            Opcion(R.drawable.ic_escudo, stringResource(R.string.ajustes_ver_aviso)) { verAviso = true }
            HorizontalDivider(color = colores.outline)

            estado.fallo?.let { MensajeError(it.texto()) }
            BotonTexto(stringResource(R.string.ajustes_cerrar_sesion), alPulsar = {
                confirmarCierre = true
            }, peligroso = true)
            if (esDonador) {
                BotonTexto(stringResource(R.string.ajustes_eliminar_cuenta), alPulsar = {
                    confirmarEliminar = true
                }, peligroso = true)
            }
            Text(
                stringResource(R.string.ajustes_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.inkMuted
            )
        }
    }

    if (verAviso) DialogoAvisoPrivacidad(alCerrar = { verAviso = false })
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.ajustes_eliminar_titulo)) },
            text = { Text(stringResource(R.string.ajustes_eliminar_texto)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminar = false
                        viewModel.eliminarCuenta()
                    },
                    enabled = !estado.eliminando
                ) { Text(stringResource(R.string.ajustes_eliminar_confirmar), color = colores.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = colores.surfaceRaised
        )
    }
    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text(stringResource(R.string.ajustes_confirmar_cierre_titulo)) },
            text = { Text(stringResource(R.string.ajustes_confirmar_cierre_texto)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarCierre = false
                        viewModel.cerrarSesion()
                    },
                    enabled = !estado.cerrandoSesion
                ) { Text(stringResource(R.string.ajustes_cerrar_sesion), color = colores.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCierre = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
            containerColor = colores.surfaceRaised
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Opcion(icono: Int, texto: String, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = alPulsar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
    ) {
        Icono(icono, null, color = GotaTema.colores.inkMuted)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
