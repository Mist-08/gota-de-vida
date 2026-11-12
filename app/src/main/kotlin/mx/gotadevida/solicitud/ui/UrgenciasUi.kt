package mx.gotadevida.solicitud.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mx.gotadevida.BuildConfig
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BannerAviso
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BarraUnidades
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.ChipTipoSangre
import mx.gotadevida.core.designsystem.componentes.EstadoVacio
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.TarjetaUrgencia
import mx.gotadevida.core.designsystem.componentes.texto
import mx.gotadevida.core.designsystem.componentes.textoDistancia
import mx.gotadevida.solicitud.entity.EstadoUrgencia
import mx.gotadevida.solicitud.entity.Urgencia
import mx.gotadevida.solicitud.viewmodel.DetalleUrgenciaViewModel
import mx.gotadevida.solicitud.viewmodel.UrgenciasViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Sección "Urgencias cerca" que el inicio del donador muestra debajo de su estado. */
@Composable
fun SeccionUrgencias(alAbrir: (String) -> Unit, viewModel: UrgenciasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x3)) {
        Text(stringResource(R.string.inicio_urgencias), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        when {
            estado.cargando -> CircularProgressIndicator(Modifier.padding(Espacio.x2), color = GotaTema.colores.sangre)
            estado.urgencias.isEmpty() -> EstadoVacio(stringResource(R.string.inicio_urgencias_vacio), R.drawable.ic_campana)
            else -> estado.urgencias.forEach { u ->
                TarjetaUrgencia(
                    etiquetaTipoSangre = u.etiquetaTipoSangre,
                    titulo = u.hospitalNombre,
                    detalle = listOfNotNull(textoDistancia(u.distanciaKm), u.hospitalDireccion).joinToString(" · "),
                    cubiertas = u.unidadesComprometidas,
                    total = u.unidades,
                    alPulsar = { alAbrir(u.id) },
                    insignia = when {
                        u.id in estado.pendientes -> stringResource(R.string.urgencia_pendiente_enviar)
                        u.miRespuesta == true -> stringResource(R.string.urgencia_ya_aceptaste)
                        else -> null
                    },
                )
            }
        }
    }
}

@Composable
fun DetalleUrgenciaScreen(
    alRegresar: () -> Unit,
    alAgendar: (hospitalId: String, hospitalNombre: String, solicitudId: String) -> Unit = { _, _, _ -> },
    viewModel: DetalleUrgenciaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val contexto = LocalContext.current
    // Contador en vivo: se consulta cada 10 segundos mientras la pantalla está abierta.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.actualizar()
            delay(10_000)
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
            val u = estado.urgencia
            if (u == null) {
                CircularProgressIndicator(color = colores.sangre)
                return@Column
            }
            if (estado.pendiente) BannerAviso(stringResource(R.string.urgencia_pendiente_texto), R.drawable.ic_nube_tachada)
            MapaHospital(u.latitud, u.longitud, u.hospitalNombre)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                val cd = stringResource(R.string.cd_tipo_sangre, u.etiquetaTipoSangre)
                ChipTipoSangre(u.etiquetaTipoSangre, Modifier.semantics { contentDescription = cd }, destacado = true)
                Column(Modifier.weight(1f)) {
                    Text(u.hospitalNombre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(u.hospitalDireccion, style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
                }
            }
            textoDistancia(u.distanciaKm)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            BarraUnidades(u.unidadesComprometidas, u.unidades)
            u.nota?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            estado.fallo?.let { MensajeError(it.texto()) }
            Acciones(u, estado.pendiente, estado.enviando, estado.acepto, viewModel::responder, { alAgendar(u.hospitalId, u.hospitalNombre, u.id) }) {
                val uri = "geo:0,0?q=${u.latitud},${u.longitud}(${Uri.encode(u.hospitalNombre)})".toUri()
                runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
            Text(stringResource(R.string.aviso_medico), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
            Spacer(Modifier.height(Espacio.x4))
        }
    }
}

@Composable
private fun Acciones(
    u: Urgencia,
    pendiente: Boolean,
    enviando: Boolean,
    acepto: Boolean,
    responder: (Boolean) -> Unit,
    agendar: () -> Unit,
    abrirRuta: () -> Unit,
) {
    val colores = GotaTema.colores
    when {
        u.miRespuesta == true || acepto -> {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x3)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
                    Icono(R.drawable.ic_check_circulo, null, color = colores.vida)
                    Text(stringResource(R.string.urgencia_gracias), style = MaterialTheme.typography.titleMedium, color = colores.vida)
                }
                BotonPrincipal(stringResource(R.string.agendar_titulo), alPulsar = agendar)
                BotonSecundario(stringResource(R.string.urgencia_abrir_ruta), alPulsar = abrirRuta)
            }
        }
        pendiente -> Unit
        u.estado == EstadoUrgencia.CUBIERTA -> EstadoVacio(stringResource(R.string.urgencia_cubierta), R.drawable.ic_check_circulo)
        u.estado != EstadoUrgencia.ACTIVA -> EstadoVacio(stringResource(R.string.urgencia_terminada), R.drawable.ic_info)
        else -> Column(verticalArrangement = Arrangement.spacedBy(Espacio.x3)) {
            BotonPrincipal(stringResource(R.string.urgencia_si_puedo), alPulsar = { responder(true) }, cargando = enviando)
            if (u.miRespuesta != false) {
                BotonSecundario(stringResource(R.string.urgencia_no_puedo), alPulsar = { responder(false) }, habilitado = !enviando)
            } else {
                Text(stringResource(R.string.urgencia_dijiste_no), style = MaterialTheme.typography.bodyMedium, color = colores.inkMuted)
            }
        }
    }
}

/** Mapa de OpenStreetMap (osmdroid): gratis y sin llave. Solo muestra el hospital, nunca al donador. */
@Composable
private fun MapaHospital(latitud: Double, longitud: Double, nombre: String) {
    val contexto = LocalContext.current
    val descripcion = stringResource(R.string.cd_mapa_hospital, nombre)
    val pin: Drawable? = remember { ContextCompat.getDrawable(contexto, R.drawable.ic_ubicacion)?.mutate() }
    val vista = remember {
        Configuration.getInstance().userAgentValue = "GotaDeVida/${BuildConfig.VERSION_NAME} (prototipo escolar)"
        MapView(contexto).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }
    DisposableEffect(Unit) {
        vista.onResume()
        onDispose { vista.onPause(); vista.onDetach() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(Radio.lg)
            .semantics { contentDescription = descripcion },
    ) {
        AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize()) { mapa ->
            val punto = GeoPoint(latitud, longitud)
            mapa.controller.setCenter(punto)
            mapa.overlays.clear()
            mapa.overlays.add(
                Marker(mapa).apply {
                    position = punto
                    title = nombre
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    pin?.let { icon = it }
                },
            )
            mapa.invalidate()
        }
        Text(
            stringResource(R.string.osm_atribucion),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        )
    }
}
