package mx.gotadevida.notificaciones.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.gotadevida.BuildConfig
import mx.gotadevida.MainActivity
import mx.gotadevida.R
import mx.gotadevida.notificaciones.repository.DispositivoRepository
import javax.inject.Inject

/**
 * Firebase Cloud Messaging sin google-services.json: se inicializa con los datos de
 * local.properties (BuildConfig). Si faltan, la app funciona igual pero sin avisos push.
 */
object AvisosPush {
    const val CANAL_URGENCIAS = "urgencias"
    const val CANAL_HOSPITAL = "respuestas"
    const val EXTRA_SOLICITUD = "solicitudId"

    val configurado: Boolean
        get() = BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    fun inicializar(contexto: Context) {
        crearCanales(contexto)
        if (!configurado || FirebaseApp.getApps(contexto).isNotEmpty()) return
        val opciones = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        FirebaseApp.initializeApp(contexto, opciones)
    }

    private fun crearCanales(contexto: Context) {
        val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_URGENCIAS, contexto.getString(R.string.canal_urgencias), NotificationManager.IMPORTANCE_HIGH),
        )
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_HOSPITAL, contexto.getString(R.string.canal_respuestas), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** Arma el texto en el idioma del teléfono a partir de los datos del mensaje. */
    fun textos(contexto: Context, datos: Map<String, String>): Pair<String, String>? {
        val tipoSangre = datos["tipoSangre"].orEmpty()
        return when (datos["tipo"]) {
            "urgencia" -> contexto.getString(R.string.push_urgencia_titulo, tipoSangre) to
                contexto.getString(R.string.push_urgencia_texto, datos["hospital"].orEmpty(), datos["distanciaKm"].orEmpty())
            "respuesta" -> contexto.getString(R.string.push_respuesta_titulo) to
                contexto.getString(R.string.push_respuesta_texto, datos["comprometidas"].orEmpty(), datos["unidades"].orEmpty(), tipoSangre)
            "urgencia_cubierta" -> contexto.getString(R.string.push_cubierta_titulo) to
                contexto.getString(R.string.push_cubierta_texto, datos["unidades"].orEmpty(), tipoSangre)
            "recordatorio_cita" -> contexto.getString(R.string.push_recordatorio_titulo) to
                contexto.getString(R.string.push_recordatorio_texto, datos["hospital"].orEmpty(), datos["hora"].orEmpty())
            "donacion_confirmada" -> contexto.getString(R.string.push_gracias_titulo) to
                contexto.getString(R.string.push_gracias_texto)
            else -> null
        }
    }

    fun mostrar(contexto: Context, datos: Map<String, String>) {
        val (titulo, texto) = textos(contexto, datos) ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            datos["solicitudId"]?.let { putExtra(EXTRA_SOLICITUD, it) }
        }
        val pendiente = PendingIntent.getActivity(
            contexto, datos.hashCode(), abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canal = if (datos["tipo"] == "urgencia") CANAL_URGENCIAS else CANAL_HOSPITAL
        val notificacion = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.ic_gota)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setColor(ContextCompat.getColor(contexto, R.color.icono_fondo))
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .setPriority(if (canal == CANAL_URGENCIAS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(contexto).notify(datos.hashCode(), notificacion)
    }
}

/** Recibe los mensajes de Firebase y el token nuevo del teléfono. */
@AndroidEntryPoint
class GotaMessagingService : FirebaseMessagingService() {
    @Inject lateinit var dispositivos: DispositivoRepository

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        alcance.launch { dispositivos.registrar(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        AvisosPush.mostrar(this, mensaje.data)
    }
}
