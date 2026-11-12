package mx.gotadevida.donador.service

import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.errores.validar
import mx.gotadevida.core.validacion.Validaciones
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.Donador
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.PerfilDonadorRespuesta
import mx.gotadevida.donador.entity.aRespuesta
import mx.gotadevida.donador.repository.DonadorRepository
import mx.gotadevida.reglas.service.ReglasService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

class DonadorService(
    private val repositorio: DonadorRepository,
    private val reglas: ReglasService,
    private val elegibilidad: ElegibilidadService,
    private val reloj: Clock = Clock.systemUTC(),
) {
    private fun hoy(): LocalDate = LocalDate.now(reloj.withZone(ZONA_LAGUNA))

    /** Lo llama la autenticación al registrar a un donador nuevo. */
    suspend fun crearPerfil(usuarioId: String, nombre: String, telefono: String?) {
        if (repositorio.buscarPorId(usuarioId) == null) {
            repositorio.guardar(Donador.nuevo(usuarioId, nombre, telefono, reloj.instant().toString()))
        }
    }

    suspend fun obtenerPerfil(id: String): PerfilDonadorRespuesta = buscar(id).aRespuesta()

    suspend fun actualizarPerfil(id: String, cambios: ActualizarPerfilSolicitud): PerfilDonadorRespuesta {
        val actual = buscar(id)
        val hoy = hoy()

        val nombre = cambios.nombre?.let { Validaciones.validarNombre(it) } ?: actual.nombre
        val telefono = cambios.telefono?.let { Validaciones.normalizarTelefono(it) } ?: actual.telefono
        val nacimiento = cambios.fechaNacimiento?.let { fechaPasada(it, "fecha de nacimiento", hoy) }
        if (nacimiento != null) {
            validar(!LocalDate.parse(nacimiento).isBefore(hoy.minusYears(100))) { "Revisa tu fecha de nacimiento." }
        }
        cambios.pesoKg?.let { validar(it in 30.0..250.0) { "El peso debe estar entre 30 y 250 kg." } }
        cambios.radioMaximoKm?.let { validar(it in 1..100) { "La distancia máxima debe estar entre 1 y 100 km." } }
        validar((cambios.latitud == null) == (cambios.longitud == null)) {
            "Manda latitud y longitud juntas."
        }
        val ubicacion = if (cambios.latitud != null && cambios.longitud != null) {
            PuntoGeo.de(cambios.latitud, cambios.longitud, decimales = DECIMALES_UBICACION)
        } else {
            actual.ubicacion
        }
        val ultimaDonacion = cambios.fechaUltimaDonacion?.let { fechaPasada(it, "fecha de tu última donación", hoy) }
        val fechasDonacion = if (ultimaDonacion != null && ultimaDonacion !in actual.fechasDonacion) {
            (actual.fechasDonacion + ultimaDonacion).sorted()
        } else {
            actual.fechasDonacion
        }

        val actualizado = actual.copy(
            nombre = nombre,
            telefono = telefono,
            tipoSangre = cambios.tipoSangre ?: actual.tipoSangre,
            fechaNacimiento = nacimiento ?: actual.fechaNacimiento,
            pesoKg = cambios.pesoKg ?: actual.pesoKg,
            sexoParaCriteriosMedicos = cambios.sexoParaCriteriosMedicos ?: actual.sexoParaCriteriosMedicos,
            ubicacion = ubicacion,
            zona = cambios.zona?.trim()?.take(80) ?: actual.zona,
            fechasDonacion = fechasDonacion,
            fechaUltimoTatuaje = cambios.fechaUltimoTatuaje?.let { fechaPasada(it, "fecha de tu último tatuaje", hoy) }
                ?: actual.fechaUltimoTatuaje,
            notificacionesActivas = cambios.notificacionesActivas ?: actual.notificacionesActivas,
            radioMaximoKm = cambios.radioMaximoKm ?: actual.radioMaximoKm,
            actualizadoEn = reloj.instant().toString(),
        )
        repositorio.guardar(actualizado)
        return actualizado.aRespuesta()
    }

    suspend fun evaluarElegibilidad(id: String): Elegibilidad =
        elegibilidad.evaluar(buscar(id), reglas.vigentes(), hoy())

    private suspend fun buscar(id: String): Donador =
        repositorio.buscarPorId(id) ?: throw NoEncontrado("No encontramos tu perfil de donador.")

    /** Valida una fecha ISO (`2026-11-12`) que no esté en el futuro y la devuelve igual. */
    private fun fechaPasada(texto: String, nombre: String, hoy: LocalDate): String {
        val fecha = try {
            LocalDate.parse(texto.trim())
        } catch (e: DateTimeParseException) {
            null
        }
        validar(fecha != null) { "La $nombre debe tener el formato AAAA-MM-DD." }
        validar(!fecha!!.isAfter(hoy)) { "La $nombre no puede estar en el futuro." }
        return fecha.toString()
    }

    companion object {
        /** Unos 100 metros: suficiente para calcular distancias sin guardar la dirección exacta. */
        const val DECIMALES_UBICACION = 3
        val ZONA_LAGUNA: ZoneId = ZoneId.of("America/Monterrey")
    }
}
