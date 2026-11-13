package mx.gotadevida.donador.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import mx.gotadevida.MainActivity
import mx.gotadevida.R
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.donador.repository.DonadorDao
import mx.gotadevida.donador.service.ActualizadorWidget

/** Lo que el widget necesita de Hilt (los widgets no tienen inyección directa). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DependenciasWidget {
    fun almacenSesion(): AlmacenSesion

    fun donadorDao(): DonadorDao
}

/**
 * Widget de la pantalla de inicio (Jetpack Glance): "Puedes donar" o
 * "Podrás volver a donar en 23 días". Lee la copia de Room, así funciona sin señal.
 */
class WidgetElegibilidad : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, DependenciasWidget::class.java)
        val sesion = deps.almacenSesion().actual()
        val elegibilidad = sesion?.let { deps.donadorDao().observarElegibilidad(it.usuarioId).first() }
        val (titulo, detalle, fondo) = when {
            sesion == null || elegibilidad == null -> Triple(
                context.getString(R.string.app_name),
                context.getString(R.string.widget_abre_la_app),
                SANGRE
            )

            elegibilidad.elegible -> Triple(
                context.getString(R.string.inicio_puedes_donar),
                context.getString(R.string.widget_gracias),
                VIDA
            )

            elegibilidad.diasParaVolverADonar != null -> {
                val dias = elegibilidad.diasParaVolverADonar.toInt()
                Triple(
                    context.getString(R.string.inicio_podras_donar),
                    context.resources.getQuantityString(R.plurals.inicio_dias, dias, dias),
                    AVISO
                )
            }

            else -> Triple(context.getString(R.string.inicio_completa_perfil), context.getString(R.string.widget_abre_la_app), SANGRE)
        }
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(fondo, fondo))
                    .cornerRadius(20.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titulo, style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = 14.sp))
                Text(
                    detalle,
                    style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    companion object {
        // Colores del sistema de diseño (sangre, vida, aviso): blanco encima pasa contraste AA.
        private val SANGRE = Color(0xFFB4161F)
        private val VIDA = Color(0xFF1C7547)
        private val AVISO = Color(0xFF8A5300)
    }
}

class ReceptorWidgetElegibilidad : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetElegibilidad()
}

class ActualizadorWidgetGlance @Inject constructor(@param:ApplicationContext private val contexto: Context) : ActualizadorWidget {
    override suspend fun actualizar() {
        if (GlanceAppWidgetManager(contexto).getGlanceIds(WidgetElegibilidad::class.java).isNotEmpty()) {
            WidgetElegibilidad().updateAll(contexto)
        }
    }
}
