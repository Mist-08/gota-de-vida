package mx.gotadevida.cita.service

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.gotadevida.cita.entity.AgendarCitaPeticion
import mx.gotadevida.cita.entity.Cita
import mx.gotadevida.cita.entity.CitaRespuesta
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.cita.entity.Horario
import mx.gotadevida.cita.entity.paraDonador
import mx.gotadevida.cita.entity.paraHospital
import mx.gotadevida.cita.repository.CitaRepository
import mx.gotadevida.core.dominio.DirectorioDonadores
import mx.gotadevida.core.dominio.DirectorioHospitales
import mx.gotadevida.core.errores.Conflicto
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.errores.Prohibido
import mx.gotadevida.core.errores.validar
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.TiposEvento
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

/**
 * Citas para donar: horarios con cupo, agenda, cancelación y check-in con QR.
 * Al agendar publica `cita.agendada` (recordatorios); al confirmar, `donacion.confirmada` (historial).
 */
class CitaService(
    private val citas: CitaRepository,
    private val donadores: DirectorioDonadores,
    private val hospitales: DirectorioHospitales,
    private val hospitalDeUsuario: suspend (usuarioId: String) -> String?,
    private val bus: BusDeEventos,
    private val reloj: Clock = Clock.systemUTC(),
) {
    private fun ahora(): LocalDateTime = LocalDateTime.now(reloj.withZone(ZONA))

    /** Horarios de 8:00 a 13:30 cada media hora, con [CUPO_POR_HORARIO] lugares cada uno. */
    suspend fun horarios(hospitalId: String, fechaTexto: String?): List<Horario> {
        hospitales.resumen(hospitalId) ?: throw NoEncontrado("No existe ese hospital.")
        val fecha = fechaTexto?.let { parsearFecha(it) } ?: ahora().toLocalDate()
        val ocupadas = citas.agendadasDeHospital(hospitalId, fecha.toString()).groupingBy { it.fechaHora.substring(11, 16) }.eachCount()
        val momento = ahora()
        return HORAS.map { hora ->
            val lugares = CUPO_POR_HORARIO - (ocupadas[hora.toString()] ?: 0)
            val enElFuturo = LocalDateTime.of(fecha, hora).isAfter(momento.plusMinutes(30))
            Horario(hora.toString(), lugares.coerceAtLeast(0), lugares > 0 && enElFuturo)
        }
    }

    suspend fun agendar(donadorId: String, peticion: AgendarCitaPeticion): CitaRespuesta {
        val donador = donadores.resumen(donadorId) ?: throw NoEncontrado("No encontramos tu perfil de donador.")
        val hospital = hospitales.resumen(peticion.hospitalId) ?: throw NoEncontrado("No existe ese hospital.")
        val fechaHora = try {
            LocalDateTime.parse(peticion.fechaHora.trim()).truncatedTo(ChronoUnit.MINUTES)
        } catch (e: DateTimeParseException) {
            throw DatosInvalidos("La fecha y hora deben tener el formato AAAA-MM-DDTHH:MM.")
        }
        validar(fechaHora.toLocalTime() in HORAS) { "Elige uno de los horarios disponibles." }
        validar(fechaHora.isAfter(ahora().plusMinutes(30))) { "Elige un horario a partir de dentro de media hora." }
        validar(!fechaHora.toLocalDate().isAfter(ahora().toLocalDate().plusDays(DIAS_MAXIMOS))) {
            "Solo puedes agendar en los próximos $DIAS_MAXIMOS días."
        }
        val proxima = donador.proximaFechaElegible?.let(LocalDate::parse)
        if (!donador.elegible && (proxima == null || proxima.isAfter(fechaHora.toLocalDate()))) {
            throw Conflicto("Para esa fecha todavía no podrías donar. Revisa tu elegibilidad en el inicio.")
        }
        if (citas.deDonador(donadorId).any { it.estado == EstadoCita.AGENDADA && LocalDateTime.parse(it.fechaHora).isAfter(ahora()) }) {
            throw Conflicto("Ya tienes una cita agendada. Cancélala si quieres cambiarla.")
        }
        val clave = fechaHora.toString()
        if (citas.agendadasDeHospital(hospital.id, clave).size >= CUPO_POR_HORARIO) throw Conflicto("Ese horario ya se llenó. Elige otro.")

        val cita = Cita(
            id = UUID.randomUUID().toString(),
            donadorId = donadorId,
            donadorNombre = donador.nombre,
            hospitalId = hospital.id,
            hospitalNombre = hospital.nombre,
            hospitalDireccion = hospital.direccion,
            latitud = hospital.ubicacion.latitud,
            longitud = hospital.ubicacion.longitud,
            fechaHora = clave,
            estado = EstadoCita.AGENDADA,
            codigoQr = nuevoCodigo(),
            creadaEn = reloj.instant().toString(),
            solicitudId = peticion.solicitudId,
        )
        citas.guardar(cita)
        bus.publicar(TiposEvento.CITA_AGENDADA, buildJsonObject { put("citaId", cita.id) })
        return cita.paraDonador()
    }

    suspend fun misCitas(donadorId: String): List<CitaRespuesta> = citas.deDonador(donadorId).map { it.paraDonador() }

    suspend fun cancelar(donadorId: String, id: String): CitaRespuesta {
        val cita = citas.buscarPorId(id) ?: throw NoEncontrado("No existe esa cita.")
        if (cita.donadorId != donadorId) throw Prohibido()
        if (cita.estado != EstadoCita.AGENDADA) throw Conflicto("Esta cita ya no se puede cancelar.")
        val cancelada = cita.copy(estado = EstadoCita.CANCELADA)
        citas.guardar(cancelada)
        return cancelada.paraDonador()
    }

    /** Citas de hoy (u otro día) del hospital de quien llama. */
    suspend fun delHospital(usuarioId: String, fechaTexto: String?): List<CitaRespuesta> {
        val hospitalId = hospitalDeUsuario(usuarioId) ?: throw Prohibido("Tu cuenta no pertenece a un hospital.")
        val fecha = fechaTexto?.let { parsearFecha(it) } ?: ahora().toLocalDate()
        return citas.agendadasDeHospital(hospitalId, fecha.toString()).map { it.paraHospital() }
    }

    /** El hospital escanea el QR: confirma la donación y dispara `donacion.confirmada`. */
    suspend fun checkin(usuarioId: String, id: String, codigo: String): CitaRespuesta {
        val hospitalId = hospitalDeUsuario(usuarioId) ?: throw Prohibido("Tu cuenta no pertenece a un hospital.")
        val cita = citas.buscarPorId(id) ?: throw NoEncontrado("No existe esa cita.")
        if (cita.hospitalId != hospitalId) throw Prohibido("Esta cita es de otro hospital.")
        if (cita.codigoQr != codigo.trim()) throw Conflicto("El código no coincide con la cita.")
        if (cita.estado != EstadoCita.AGENDADA) throw Conflicto("Esta cita ya fue confirmada o cancelada.")
        val completada = cita.copy(estado = EstadoCita.COMPLETADA, completadaEn = reloj.instant().toString())
        citas.guardar(completada)
        bus.publicar(
            TiposEvento.DONACION_CONFIRMADA,
            buildJsonObject {
                put("citaId", cita.id)
                put("donadorId", cita.donadorId)
                put("hospitalId", cita.hospitalId)
                put("hospitalNombre", cita.hospitalNombre)
                put("fecha", ahora().toLocalDate().toString())
                cita.solicitudId?.let { put("solicitudId", it) }
            },
        )
        return completada.paraHospital()
    }

    /** El QR contiene `gota:<citaId>:<codigo>`; esto lo separa. */
    suspend fun checkinConQr(usuarioId: String, contenidoQr: String): CitaRespuesta {
        val partes = contenidoQr.trim().split(':')
        validar(partes.size == 3 && partes[0] == "gota") { "Ese código QR no es de Gota de Vida." }
        return checkin(usuarioId, partes[1], partes[2])
    }

    suspend fun buscar(id: String): Cita? = citas.buscarPorId(id)

    suspend fun horariosOcupadosHoy(hospitalId: String): Int = citas.agendadasDeHospital(hospitalId, ahora().toLocalDate().toString()).size

    private fun parsearFecha(texto: String): LocalDate = try {
        LocalDate.parse(texto.trim())
    } catch (e: DateTimeParseException) {
        throw DatosInvalidos("La fecha debe tener el formato AAAA-MM-DD.")
    }

    private fun nuevoCodigo(): String {
        val bytes = ByteArray(12).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        val ZONA: ZoneId = ZoneId.of("America/Monterrey")
        const val CUPO_POR_HORARIO = 3
        const val DIAS_MAXIMOS = 14L
        val HORAS: List<LocalTime> = (0 until 12).map { LocalTime.of(8, 0).plusMinutes(30L * it) }
    }
}
