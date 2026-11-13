package mx.gotadevida.donador.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Municipios de la Comarca Lagunera con su centro aproximado, para quien no quiere usar el GPS. */
data class Zona(val nombre: String, val latitud: Double, val longitud: Double)

object ZonasLaguna {
    val lista = listOf(
        Zona("Torreón", 25.5428, -103.4068),
        Zona("Gómez Palacio", 25.5611, -103.4983),
        Zona("Lerdo", 25.5383, -103.5244),
        Zona("Matamoros", 25.5281, -103.2280),
        Zona("Francisco I. Madero", 25.7756, -103.2731),
        Zona("San Pedro", 25.7583, -102.9833),
        Zona("Viesca", 25.3417, -102.8050)
    )
}

/**
 * Ubicación aproximada con el permiso de ubicación "aproximada" (ACCESS_COARSE_LOCATION)
 * y el LocationManager de Android: sin Google Play Services ni costo. El backend además la redondea.
 */
object UbicacionAproximada {

    fun tienePermiso(contexto: Context): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun obtener(contexto: Context): Location? {
        if (!tienePermiso(contexto)) return null
        val gestor = contexto.getSystemService(LocationManager::class.java) ?: return null
        val proveedor = when {
            gestor.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            gestor.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        val actual = if (proveedor != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Location?> { continuacion ->
                    gestor.getCurrentLocation(proveedor, null, contexto.mainExecutor) { continuacion.resume(it) }
                }
            }
        } else {
            null
        }
        return actual ?: gestor.getProviders(true)
            .mapNotNull { runCatching { gestor.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
