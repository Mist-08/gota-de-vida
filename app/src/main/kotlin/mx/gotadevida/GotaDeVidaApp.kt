package mx.gotadevida

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import mx.gotadevida.core.sync.ColaDeSalidaRoom
import mx.gotadevida.notificaciones.service.AvisosPush
import javax.inject.Inject

/**
 * Aplicación: Hilt, WorkManager con inyección (para la cola de salida) y avisos push.
 */
@HiltAndroidApp
class GotaDeVidaApp : Application(), Configuration.Provider {
    @Inject lateinit var fabricaWorkers: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaWorkers).build()

    override fun onCreate() {
        super.onCreate()
        AvisosPush.inicializar(this)
        // Si quedó algo pendiente de una sesión anterior, se intenta mandar.
        ColaDeSalidaRoom.programarEnvio(this)
    }
}
