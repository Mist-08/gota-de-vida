package mx.gotadevida.chatbot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.gotadevida.R
import mx.gotadevida.chatbot.entity.Autor
import mx.gotadevida.chatbot.entity.MensajeChat
import mx.gotadevida.chatbot.viewmodel.ChatViewModel
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.designsystem.componentes.BarraSuperior
import mx.gotadevida.core.designsystem.componentes.BotonTexto
import mx.gotadevida.core.designsystem.componentes.CampoTexto
import mx.gotadevida.core.designsystem.componentes.Icono
import mx.gotadevida.core.designsystem.componentes.MensajeError
import mx.gotadevida.core.designsystem.componentes.texto

/** Chat con Gota: burbujas del sistema de diseño, respuestas rápidas y campo de texto. */
@Composable
fun ChatScreen(alRegresar: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val colores = GotaTema.colores
    val lista = rememberLazyListState()
    LaunchedEffect(estado.mensajes.size) { if (estado.mensajes.isNotEmpty()) lista.animateScrollToItem(estado.mensajes.size) }

    Scaffold(containerColor = colores.surface) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).imePadding()) {
            BarraSuperior(
                modifier = Modifier.padding(horizontal = Espacio.x4),
                alRegresar = alRegresar,
                titulo = stringResource(R.string.chat_titulo),
            )
            LazyColumn(
                state = lista,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Espacio.x2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Espacio.x4),
            ) {
                item { Burbuja(MensajeChat(Autor.GOTA, stringResource(R.string.chat_bienvenida), "")) }
                items(estado.mensajes) { Burbuja(it) }
                if (estado.enviando) item { CircularProgressIndicator(Modifier.padding(Espacio.x2), color = colores.sangre) }
            }
            estado.fallo?.let { f ->
                Column(Modifier.padding(horizontal = Espacio.x4)) {
                    MensajeError(f.texto())
                    estado.sinEnviar?.let { BotonTexto(stringResource(R.string.accion_reintentar_envio), alPulsar = { viewModel.enviar(it) }) }
                }
            }
            if (estado.opciones.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Espacio.x4),
                ) {
                    items(estado.opciones) { o ->
                        AssistChip(onClick = { viewModel.enviar(o) }, label = { Text(textoOpcion(o)) }, enabled = !estado.enviando)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(Espacio.x4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.x2),
            ) {
                Box(Modifier.weight(1f)) {
                    CampoTexto(
                        valor = estado.borrador,
                        alCambiar = viewModel::onBorrador,
                        etiqueta = stringResource(R.string.chat_escribe),
                        accionTeclado = ImeAction.Send,
                        alConfirmar = { viewModel.enviar() },
                    )
                }
                IconButton(onClick = { viewModel.enviar() }, enabled = estado.borrador.isNotBlank() && !estado.enviando) {
                    Icono(R.drawable.ic_enviar, stringResource(R.string.chat_enviar), color = colores.sangre)
                }
            }
            Text(
                stringResource(R.string.aviso_medico),
                style = MaterialTheme.typography.labelSmall,
                color = colores.inkMuted,
                modifier = Modifier.padding(start = Espacio.x4, end = Espacio.x4, bottom = Espacio.x2),
            )
        }
    }
}

@Composable
private fun textoOpcion(opcion: String): String = when (opcion) {
    "1" -> stringResource(R.string.chat_opcion_1)
    "2" -> stringResource(R.string.chat_opcion_2)
    "3" -> stringResource(R.string.chat_opcion_3)
    "4" -> stringResource(R.string.chat_opcion_4)
    else -> opcion
}

@Composable
private fun Burbuja(mensaje: MensajeChat) {
    val colores = GotaTema.colores
    val mio = mensaje.autor == Autor.DONADOR
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mio) Arrangement.End else Arrangement.Start) {
        Text(
            mensaje.texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (mio) colores.onSangre else colores.ink,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (mio) colores.sangre else colores.surfaceSunken,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mio) 16.dp else 4.dp, bottomEnd = if (mio) 4.dp else 16.dp),
                )
                .padding(horizontal = Espacio.x3, vertical = 10.dp)
                .semantics { if (!mio) liveRegion = LiveRegionMode.Polite },
        )
    }
}
