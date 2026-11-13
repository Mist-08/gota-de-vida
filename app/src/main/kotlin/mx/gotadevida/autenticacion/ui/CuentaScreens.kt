package mx.gotadevida.autenticacion.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mx.gotadevida.R
import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo
import mx.gotadevida.autenticacion.service.CuentaGoogleResultado
import mx.gotadevida.autenticacion.service.InicioConGoogle
import mx.gotadevida.autenticacion.viewmodel.InicioSesionViewModel
import mx.gotadevida.autenticacion.viewmodel.RegistroViewModel
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.CampoTexto
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto

// ---------- Bienvenida ----------

@Composable
fun BienvenidaScreen(alCrearCuenta: () -> Unit, alIniciarSesion: () -> Unit) {
    val colores = GotaTema.colores
    var acepto by rememberSaveable { mutableStateOf(false) }
    var verAviso by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x8),
            verticalArrangement = Arrangement.spacedBy(Espacio.x8)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(
                    modifier = Modifier.size(64.dp).background(colores.sangreSoft, MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Icono(R.drawable.ic_gota, null, color = colores.sangre, tamano = 32.dp)
                }
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = MaterialTheme.typography.displayMedium.fontSize
                    )
                )
                Text(stringResource(R.string.bienvenida_subtitulo), style = MaterialTheme.typography.bodyLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                PuntoBienvenida(
                    R.drawable.ic_campana,
                    R.string.bienvenida_punto1_titulo,
                    R.string.bienvenida_punto1_texto
                )
                PuntoBienvenida(R.drawable.ic_chat, R.string.bienvenida_punto2_titulo, R.string.bienvenida_punto2_texto)
                PuntoBienvenida(
                    R.drawable.ic_escudo,
                    R.string.bienvenida_punto3_titulo,
                    R.string.bienvenida_punto3_texto
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = acepto, role = Role.Checkbox, onValueChange = { acepto = it }),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x3)
                ) {
                    Checkbox(
                        checked = acepto,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colores.sangre,
                            uncheckedColor = colores.outlineStrong
                        )
                    )
                    Text(
                        stringResource(R.string.bienvenida_consentimiento),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = { verAviso = true }) {
                    Text(stringResource(R.string.bienvenida_leer_aviso), color = colores.info)
                }
                BotonPrincipal(stringResource(R.string.bienvenida_crear_cuenta), alCrearCuenta, habilitado = acepto)
                BotonSecundario(stringResource(R.string.bienvenida_ya_tengo_cuenta), alIniciarSesion)
            }
        }
    }
    if (verAviso) DialogoAvisoPrivacidad(alCerrar = { verAviso = false })
}

@Composable
private fun PuntoBienvenida(@DrawableRes icono: Int, titulo: Int, texto: Int) {
    val colores = GotaTema.colores
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(colores.surfaceSunken, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icono(icono, null, color = colores.sangre, tamano = 22.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(titulo),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            )
            Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
        }
    }
}

@Composable
fun DialogoAvisoPrivacidad(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(R.string.aviso_privacidad_titulo)) },
        text = {
            Text(
                stringResource(R.string.aviso_privacidad_texto),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.accion_entendido)) } }
    )
}

// ---------- Registro ----------

@Composable
fun RegistroScreen(
    aceptoAviso: Boolean,
    alRegresar: () -> Unit,
    alRegistrarse: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.listo) { if (estado.listo) alRegistrarse() }

    PantallaFormulario(alRegresar = alRegresar, paso = stringResource(R.string.registro_paso)) {
        Encabezado(stringResource(R.string.registro_titulo), stringResource(R.string.registro_subtitulo))
        BotonGoogle(
            habilitado = !estado.enviando,
            alToken = viewModel::conGoogle,
            alFallar = viewModel::googleNoDisponible
        )
        CampoTexto(
            valor = estado.nombre,
            alCambiar = viewModel::onNombre,
            etiqueta = stringResource(R.string.campo_nombre),
            error = estado.errores[CampoCuenta.NOMBRE]?.texto()
        )
        CampoTexto(
            valor = estado.email,
            alCambiar = viewModel::onEmail,
            etiqueta = stringResource(R.string.campo_correo),
            error = estado.errores[CampoCuenta.EMAIL]?.texto(),
            tipoTeclado = KeyboardType.Email
        )
        CampoTexto(
            valor = estado.password,
            alCambiar = viewModel::onPassword,
            etiqueta = stringResource(R.string.campo_contrasena),
            ayuda = stringResource(R.string.campo_contrasena_ayuda),
            error = estado.errores[CampoCuenta.PASSWORD]?.texto(),
            tipoTeclado = KeyboardType.Password,
            esContrasena = true
        )
        CampoTexto(
            valor = estado.telefono,
            alCambiar = viewModel::onTelefono,
            etiqueta = stringResource(R.string.campo_telefono),
            ayuda = stringResource(R.string.campo_telefono_ayuda),
            error = estado.errores[CampoCuenta.TELEFONO]?.texto(),
            tipoTeclado = KeyboardType.Phone,
            accionTeclado = ImeAction.Done,
            alConfirmar = { viewModel.registrar(aceptoAviso) }
        )
        estado.fallo?.let { MensajeError(it.texto()) }
        if (estado.googleNoDisponible) MensajeError(stringResource(R.string.error_google_no_disponible))
        Spacer(Modifier.height(Espacio.x2))
        BotonPrincipal(
            stringResource(R.string.accion_continuar),
            alPulsar = { viewModel.registrar(aceptoAviso) },
            cargando = estado.enviando
        )
    }
}

// ---------- Inicio de sesión ----------

@Composable
fun InicioSesionScreen(alRegresar: () -> Unit, alEntrar: () -> Unit, viewModel: InicioSesionViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.listo) { if (estado.listo) alEntrar() }

    PantallaFormulario(alRegresar = alRegresar) {
        Encabezado(stringResource(R.string.inicio_sesion_titulo), stringResource(R.string.inicio_sesion_subtitulo))
        BotonGoogle(
            habilitado = !estado.enviando,
            alToken = viewModel::conGoogle,
            alFallar = viewModel::googleNoDisponible
        )
        CampoTexto(
            valor = estado.email,
            alCambiar = viewModel::onEmail,
            etiqueta = stringResource(R.string.campo_correo),
            error = estado.errores[CampoCuenta.EMAIL]?.texto(),
            tipoTeclado = KeyboardType.Email
        )
        CampoTexto(
            valor = estado.password,
            alCambiar = viewModel::onPassword,
            etiqueta = stringResource(R.string.campo_contrasena),
            error = estado.errores[CampoCuenta.PASSWORD]?.texto(),
            tipoTeclado = KeyboardType.Password,
            accionTeclado = ImeAction.Done,
            alConfirmar = viewModel::entrar,
            esContrasena = true
        )
        estado.fallo?.let { MensajeError(it.texto()) }
        if (estado.googleNoDisponible) MensajeError(stringResource(R.string.error_google_no_disponible))
        Spacer(Modifier.height(Espacio.x2))
        BotonPrincipal(stringResource(R.string.accion_entrar), alPulsar = viewModel::entrar, cargando = estado.enviando)
    }
}

// ---------- Piezas compartidas ----------

@Composable
private fun PantallaFormulario(alRegresar: () -> Unit, paso: String? = null, contenido: @Composable () -> Unit) {
    val colores = GotaTema.colores
    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x6)
        ) {
            BarraSuperior(alRegresar = alRegresar) {
                if (paso != null) {
                    Text(paso, style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
                }
            }
            contenido()
        }
    }
}

@Composable
private fun Encabezado(titulo: String, subtitulo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x2)) {
        Text(titulo, style = MaterialTheme.typography.headlineLarge)
        Text(subtitulo, style = MaterialTheme.typography.bodyLarge, color = GotaTema.colores.inkMuted)
    }
}

/** "Continuar con Google": solo aparece si la app tiene configurado el ID de cliente. */
@Composable
private fun BotonGoogle(habilitado: Boolean, alToken: (String) -> Unit, alFallar: () -> Unit) {
    if (!InicioConGoogle.estaConfigurado) return
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x6)) {
        BotonSecundario(
            texto = stringResource(R.string.continuar_con_google),
            habilitado = habilitado,
            alPulsar = {
                alcance.launch {
                    when (val r = InicioConGoogle.pedirToken(contexto)) {
                        is CuentaGoogleResultado.Token -> alToken(r.idToken)
                        CuentaGoogleResultado.NoDisponible -> alFallar()
                        CuentaGoogleResultado.Cancelado -> Unit
                    }
                }
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
            HorizontalDivider(Modifier.weight(1f), color = GotaTema.colores.outline)
            Text(
                stringResource(R.string.registro_o_con_correo),
                style = MaterialTheme.typography.bodyMedium,
                color = GotaTema.colores.inkMuted
            )
            HorizontalDivider(Modifier.weight(1f), color = GotaTema.colores.outline)
        }
    }
}

@Composable
private fun ErrorCampo.texto(): String = stringResource(
    when (this) {
        ErrorCampo.VACIO -> R.string.error_campo_vacio
        ErrorCampo.NOMBRE_CORTO -> R.string.error_nombre_corto
        ErrorCampo.EMAIL_INVALIDO -> R.string.error_email_invalido
        ErrorCampo.PASSWORD_CORTA -> R.string.error_password_corta
        ErrorCampo.TELEFONO_INVALIDO -> R.string.error_telefono_invalido
    }
)
