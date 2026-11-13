package mx.gotadevida.hospital.service

import mx.gotadevida.hospital.entity.CrearHospitalSolicitud

/**
 * Hospitales para la demo. Las coordenadas son aproximadas (centro de cada zona) y quedan
 * marcados como `esDemostracion`. Antes de usarlo en serio, confirmen nombres, direcciones
 * y ubicaciones con cada banco de sangre.
 */
object HospitalesDemo {
    val lista = listOf(
        CrearHospitalSolicitud(
            nombre = "Hospital Universitario de Torreón",
            direccion = "Centro, Torreón",
            municipio = "Torreón",
            latitud = 25.5428,
            longitud = -103.4068
        ),
        CrearHospitalSolicitud(
            nombre = "Hospital General de Gómez Palacio",
            direccion = "Centro, Gómez Palacio",
            municipio = "Gómez Palacio",
            latitud = 25.5617,
            longitud = -103.4980
        ),
        CrearHospitalSolicitud(
            nombre = "Cruz Roja Torreón",
            direccion = "Torreón",
            municipio = "Torreón",
            latitud = 25.5560,
            longitud = -103.4300
        )
    )

    suspend fun cargarSiFaltan(servicio: HospitalService) {
        if (servicio.hayHospitales()) return
        lista.forEach { servicio.crear(it, esDemostracion = true) }
    }
}
