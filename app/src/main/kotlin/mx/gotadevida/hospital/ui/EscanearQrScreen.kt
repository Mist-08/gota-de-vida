package mx.gotadevida.hospital.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.Radio
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonPrincipal
import mx.gotadevida.core.designsystem.componentes.BotonSecundario
import mx.gotadevida.core.designsystem.componentes.CampoTexto
import mx.gotadevida.core.designsystem.componentes.EstadoVacio
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto
import mx.gotadevida.hospital.viewmodel.EscanearViewModel
import java.util.concurrent.Executors

/** Check-in con QR: CameraX + ML Kit (en el teléfono, gratis). También se puede escribir el código. */
@Composable
fun EscanearQrScreen(alRegresar: () -> Unit, viewModel: EscanearViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val contexto = LocalContext.current
    var conPermiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { conPermiso = it }
    LaunchedEffect(Unit) { if (!conPermiso) pedir.launch(Manifest.permission.CAMERA) }

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x6, vertical = Espacio.x2),
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        ) {
            BarraSuperior(alRegresar = alRegresar, titulo = stringResource(R.string.escanear_titulo))
            val confirmada = estado.confirmada
            if (confirmada != null) {
                EstadoVacio(stringResource(R.string.escanear_confirmada, confirmada.donadorNombre.orEmpty()), R.drawable.ic_check_circulo)
                BotonPrincipal(stringResource(R.string.escanear_otra), alPulsar = viewModel::otra)
                BotonSecundario(stringResource(R.string.accion_regresar), alPulsar = alRegresar)
                return@Column
            }
            Text(stringResource(R.string.escanear_instrucciones), style = MaterialTheme.typography.bodyLarge)
            if (conPermiso) {
                val descripcion = stringResource(R.string.cd_camara)
                Box(Modifier.fillMaxWidth().height(300.dp).clip(Radio.lg).semantics { contentDescription = descripcion }) {
                    VistaCamara(alLeer = viewModel::alLeer)
                }
            } else {
                EstadoVacio(stringResource(R.string.escanear_sin_permiso), R.drawable.ic_info)
            }
            estado.fallo?.let { MensajeError(it.texto()) }
            Text(stringResource(R.string.escanear_o_escribe), style = MaterialTheme.typography.labelLarge, color = colores.inkMuted)
            CampoTexto(valor = estado.codigoManual, alCambiar = viewModel::onCodigo, etiqueta = stringResource(R.string.escanear_codigo))
            BotonPrincipal(
                stringResource(R.string.escanear_confirmar),
                alPulsar = viewModel::confirmarManual,
                habilitado = estado.codigoManual.isNotBlank(),
                cargando = estado.enviando,
            )
        }
    }
}

@Composable
private fun VistaCamara(alLeer: (String) -> Unit) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val vista = remember { PreviewView(contexto) }
    val hilo = remember { Executors.newSingleThreadExecutor() }
    val lector = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener({
            val proveedor = futuro.get()
            val vistaPrevia = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
            val analisis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analisis.setAnalyzer(hilo, LectorQr(lector, alLeer))
            proveedor.unbindAll()
            proveedor.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, vistaPrevia, analisis)
        }, ContextCompat.getMainExecutor(contexto))
        onDispose {
            runCatching { futuro.get().unbindAll() }
            lector.close()
            hilo.shutdown()
        }
    }
    AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize())
}

/** Pasa cada cuadro de la cámara a ML Kit y avisa el contenido del primer QR que encuentre. */
private class LectorQr(
    private val lector: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val alLeer: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(cuadro: androidx.camera.core.ImageProxy) {
        val imagen = cuadro.image ?: return cuadro.close()
        lector.process(InputImage.fromMediaImage(imagen, cuadro.imageInfo.rotationDegrees))
            .addOnSuccessListener { codigos -> codigos.firstOrNull()?.rawValue?.let(alLeer) }
            .addOnCompleteListener { cuadro.close() }
    }
}
