package mx.gotadevida.core.dominio

import kotlinx.serialization.Serializable
import mx.gotadevida.core.errores.validar
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Punto GeoJSON tal como lo espera MongoDB para los índices `2dsphere`.
 * Ojo: GeoJSON guarda primero la longitud y luego la latitud.
 */
@Serializable
data class PuntoGeo(
    val type: String,
    val coordinates: List<Double>,
) {
    val longitud: Double get() = coordinates[0]
    val latitud: Double get() = coordinates[1]

    /** Distancia en kilómetros (fórmula de haversine). */
    fun distanciaKmA(otro: PuntoGeo): Double {
        val radioTierraKm = 6371.0
        val dLat = Math.toRadians(otro.latitud - latitud)
        val dLon = Math.toRadians(otro.longitud - longitud)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitud)) * cos(Math.toRadians(otro.latitud)) * sin(dLon / 2).pow(2)
        return 2 * radioTierraKm * asin(sqrt(a))
    }

    companion object {
        /**
         * Crea un punto. Con `decimales = 3` la precisión es de unos 100 metros:
         * así nunca se guarda la dirección exacta del donador.
         */
        fun de(latitud: Double, longitud: Double, decimales: Int? = null): PuntoGeo {
            validar(latitud in -90.0..90.0) { "La latitud debe estar entre -90 y 90." }
            validar(longitud in -180.0..180.0) { "La longitud debe estar entre -180 y 180." }
            fun redondear(valor: Double): Double {
                if (decimales == null) return valor
                val factor = 10.0.pow(decimales)
                return (valor * factor).roundToLong() / factor
            }
            return PuntoGeo(type = "Point", coordinates = listOf(redondear(longitud), redondear(latitud)))
        }
    }
}
