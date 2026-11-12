package mx.gotadevida.cita

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import mx.gotadevida.cita.entity.AgendarCitaPeticion
import mx.gotadevida.cita.entity.CheckinPeticion
import mx.gotadevida.cita.entity.CitaRespuesta
import mx.gotadevida.cita.entity.EstadoCita
import mx.gotadevida.cita.entity.Horario
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.donacion.entity.HistorialRespuesta
import mx.gotadevida.donacion.entity.Insignia
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.hospital.service.Estadisticas
import mx.gotadevida.solicitud.SolicitudRoutesTest
import mx.gotadevida.solicitud.entity.ResponderPeticion
import mx.gotadevida.solicitud.entity.SolicitudRespuesta
import mx.gotadevida.soporte.conToken
import mx.gotadevida.soporte.probarApi

class CitaRoutesTest {
    private val escenario = SolicitudRoutesTest()
    private val manana = LocalDate.now(ZoneId.of("America/Monterrey")).plusDays(1)

    @Test
    fun `flujo completo - acepta urgencia, agenda, check-in con QR e historial`() = probarApi { cliente, entorno ->
        val hospital = with(escenario) { cliente.crearHospitalYCuenta() }
        val (donadorId, donador) = with(escenario) {
            cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0, "token-oneg-0000000000000")
        }
        val hospitalId = entorno.hospitales.datos.values.first().id
        val urgencia = with(escenario) {
            cliente.publicar(hospital, TipoSangre.O_NEGATIVO, 1)
        }.body<SolicitudRespuesta>()
        cliente.post("/api/v1/solicitudes/${urgencia.id}/respuestas") {
            conToken(donador)
            contentType(ContentType.Application.Json)
            setBody(ResponderPeticion(true))
        }

        val horarios = cliente.get("/api/v1/hospitales/$hospitalId/horarios?fecha=$manana") {
            conToken(donador)
        }.body<List<Horario>>()
        assertEquals(12, horarios.size)
        assertTrue(horarios.all { it.disponible && it.lugares == 3 })

        val respuesta = cliente.post("/api/v1/citas") {
            conToken(donador)
            contentType(ContentType.Application.Json)
            setBody(AgendarCitaPeticion(hospitalId, "${manana}T09:30", urgencia.id))
        }
        assertEquals(HttpStatusCode.Created, respuesta.status)
        val cita = respuesta.body<CitaRespuesta>()
        assertNotNull(cita.codigoQr)
        // Se programaron los recordatorios (24 h y 2 h antes) en la cola de retraso.
        assertTrue(entorno.bus.retrasados.any { it.tipo == TiposEvento.CITA_RECORDATORIO })

        // El horario ocupado tiene un lugar menos.
        val despues = cliente.get("/api/v1/hospitales/$hospitalId/horarios?fecha=$manana") {
            conToken(donador)
        }.body<List<Horario>>()
        assertEquals(2, despues.first { it.hora == "09:30" }.lugares)

        // No puede tener dos citas a la vez.
        val otra = cliente.post("/api/v1/citas") {
            conToken(donador)
            contentType(ContentType.Application.Json)
            setBody(AgendarCitaPeticion(hospitalId, "${manana}T10:00"))
        }
        assertEquals(HttpStatusCode.Conflict, otra.status)

        // El hospital ve la cita y escanea el QR ("gota:<cita>:<código>").
        val delHospital = cliente.get("/api/v1/hospitales/me/citas?fecha=$manana") {
            conToken(hospital)
        }.body<List<CitaRespuesta>>()
        assertEquals("Donador O_NEGATIVO", delHospital.single().donadorNombre)
        val malo = cliente.post("/api/v1/citas/checkin-qr") {
            conToken(hospital)
            contentType(ContentType.Application.Json)
            setBody(CheckinPeticion("gota:${cita.id}:codigo-falso"))
        }
        assertEquals(HttpStatusCode.Conflict, malo.status)
        val confirmada = cliente.post("/api/v1/citas/checkin-qr") {
            conToken(hospital)
            contentType(ContentType.Application.Json)
            setBody(CheckinPeticion("gota:${cita.id}:${cita.codigoQr}"))
        }.body<CitaRespuesta>()
        assertEquals(EstadoCita.COMPLETADA, confirmada.estado)

        // HistorialConsumer: donación registrada, insignias y ya no es elegible (60 días).
        val historial = cliente.get("/api/v1/donaciones/me") { conToken(donador) }.body<HistorialRespuesta>()
        assertEquals(1, historial.totalDonaciones)
        assertEquals(3, historial.vidasQuePuedesAyudar)
        assertFalse(historial.elegible)
        assertNotNull(historial.proximaFechaElegible)
        assertTrue(historial.insignias.first { it.codigo == Insignia.HEROE_DE_URGENCIA }.obtenida)
        val elegibilidad = cliente.get("/api/v1/donadores/me/elegibilidad") { conToken(donador) }.body<Elegibilidad>()
        assertFalse(elegibilidad.elegible)
        assertTrue(entorno.push.enviados.any { it.second["tipo"] == "donacion_confirmada" })
        assertEquals(donadorId, entorno.donaciones.datos.values.single().donadorId)

        val stats = cliente.get("/api/v1/hospitales/me/estadisticas") { conToken(hospital) }.body<Estadisticas>()
        assertEquals(1, stats.urgenciasTotales)
        assertEquals(1, stats.urgenciasCubiertas)
        assertEquals(1, stats.donacionesConfirmadas)
        assertEquals("O−", stats.tiposMasSolicitados.single().etiquetaTipoSangre)
    }

    @Test
    fun `el recordatorio llega solo si la cita sigue agendada`() = probarApi { cliente, entorno ->
        with(escenario) { cliente.crearHospitalYCuenta() }
        val (_, donador) = with(escenario) {
            cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0, "token-oneg-0000000000000")
        }
        val hospitalId = entorno.hospitales.datos.values.first().id
        val cita = cliente.post("/api/v1/citas") {
            conToken(donador)
            contentType(ContentType.Application.Json)
            setBody(AgendarCitaPeticion(hospitalId, "${manana}T13:00"))
        }.body<CitaRespuesta>()
        val cancelada = cliente.post("/api/v1/citas/${cita.id}/cancelar") { conToken(donador) }.body<CitaRespuesta>()
        assertEquals(EstadoCita.CANCELADA, cancelada.estado)
        val mias = cliente.get("/api/v1/citas/me") { conToken(donador) }.body<List<CitaRespuesta>>()
        assertEquals(EstadoCita.CANCELADA, mias.single().estado)
        assertTrue(entorno.push.enviados.none { it.second["tipo"] == "recordatorio_cita" })
    }

    @Test
    fun `no se agenda en horarios que no existen o en el pasado`() = probarApi { cliente, entorno ->
        with(escenario) { cliente.crearHospitalYCuenta() }
        val (_, donador) = with(escenario) { cliente.donador("oneg@correo.mx", TipoSangre.O_NEGATIVO, 3.0) }
        val hospitalId = entorno.hospitales.datos.values.first().id
        for (fechaHora in listOf("${manana}T07:15", "2020-01-01T09:00", "mañana")) {
            val r = cliente.post("/api/v1/citas") {
                conToken(donador)
                contentType(ContentType.Application.Json)
                setBody(AgendarCitaPeticion(hospitalId, fechaHora))
            }
            assertEquals(HttpStatusCode.BadRequest, r.status, fechaHora)
        }
    }
}
