package mx.gotadevida.core.designsystem.componentes

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mx.gotadevida.R
import mx.gotadevida.core.designsystem.Espacio
import mx.gotadevida.core.designsystem.GotaDeVidaTema
import mx.gotadevida.core.designsystem.GotaTema

/**
 * Vistas previas de los componentes del sistema de diseño en claro y oscuro,
 * para compararlos con el diseño desde Android Studio.
 */
@Composable
private fun Muestrario() {
    Column(
        Modifier.background(GotaTema.colores.surface).padding(Espacio.x4),
        verticalArrangement = Arrangement.spacedBy(Espacio.x3)
    ) {
        BotonPrincipal("Sí puedo donar", alPulsar = {})
        BotonSecundario("Abrir ruta al hospital", alPulsar = {})
        BotonTexto("Cancelar urgencia", alPulsar = {}, peligroso = true)
        CampoTexto(valor = "Mariana", alCambiar = {}, etiqueta = "Nombre")
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x2)) {
            ChipTipoSangre("O−", destacado = true)
            ChipTipoSangre("A+")
        }
        TarjetaUrgencia("O−", "Hospital Universitario", "a 3.2 km · Centro, Torreón", 2, 3, alPulsar = {})
        BannerAviso("Sin conexión. Te mostramos lo último guardado.", R.drawable.ic_nube_tachada)
        EstadoVacio("Cuando un hospital cercano necesite tu tipo de sangre, aparecerá aquí.", R.drawable.ic_campana)
        MensajeError("No pudimos conectarnos. Revisa tu internet e intenta de nuevo.")
    }
}

@Preview(name = "Claro", showBackground = true, widthDp = 360)
@Composable
private fun MuestrarioClaro() = GotaDeVidaTema(oscuro = false) { Muestrario() }

@Preview(name = "Oscuro", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MuestrarioOscuro() = GotaDeVidaTema(oscuro = true) { Muestrario() }
