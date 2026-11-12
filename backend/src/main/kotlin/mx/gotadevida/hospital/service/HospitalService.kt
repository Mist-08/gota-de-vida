package mx.gotadevida.hospital.service

import mx.gotadevida.core.dominio.DirectorioHospitales
import mx.gotadevida.core.dominio.HospitalResumen
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.errores.validar
import mx.gotadevida.hospital.entity.CrearHospitalSolicitud
import mx.gotadevida.hospital.entity.Hospital
import mx.gotadevida.hospital.entity.HospitalRespuesta
import mx.gotadevida.hospital.entity.aRespuesta
import mx.gotadevida.hospital.repository.HospitalRepository
import java.time.Clock
import java.util.UUID

class HospitalService(
    private val repositorio: HospitalRepository,
    private val reloj: Clock = Clock.systemUTC(),
) : DirectorioHospitales {
    suspend fun crear(solicitud: CrearHospitalSolicitud, esDemostracion: Boolean = false): HospitalRespuesta {
        val nombre = solicitud.nombre.trim()
        val direccion = solicitud.direccion.trim()
        val municipio = solicitud.municipio.trim()
        validar(nombre.length in 3..120) { "El nombre del hospital debe tener entre 3 y 120 caracteres." }
        validar(direccion.isNotEmpty()) { "Escribe la dirección del hospital." }
        validar(municipio.isNotEmpty()) { "Escribe el municipio del hospital." }
        val hospital = Hospital(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            direccion = direccion,
            municipio = municipio,
            // El hospital es un lugar público: su ubicación se guarda completa.
            ubicacion = PuntoGeo.de(solicitud.latitud, solicitud.longitud),
            creadoEn = reloj.instant().toString(),
            telefono = solicitud.telefono?.trim()?.takeIf { it.isNotEmpty() },
            esDemostracion = esDemostracion,
        )
        repositorio.crear(hospital)
        return hospital.aRespuesta()
    }

    suspend fun listar(): List<HospitalRespuesta> = repositorio.listar().map { it.aRespuesta() }

    suspend fun obtener(id: String): HospitalRespuesta =
        (repositorio.buscarPorId(id) ?: throw NoEncontrado("No existe ese hospital.")).aRespuesta()

    suspend fun hayHospitales(): Boolean = repositorio.contar() > 0

    suspend fun existe(id: String): Boolean = repositorio.buscarPorId(id) != null

    override suspend fun resumen(id: String): HospitalResumen? = repositorio.buscarPorId(id)?.let {
        HospitalResumen(it.id, it.nombre, it.direccion, it.ubicacion, it.telefono)
    }
}
