package mx.gotadevida.core.di

import mx.gotadevida.autenticacion.repository.MongoUsuarioRepository
import mx.gotadevida.autenticacion.repository.UsuarioRepository
import mx.gotadevida.autenticacion.service.AutenticacionService
import mx.gotadevida.autenticacion.service.VerificadorGoogle
import mx.gotadevida.autenticacion.service.VerificadorGoogleTokenInfo
import mx.gotadevida.core.config.Configuracion
import mx.gotadevida.core.db.BaseDeDatos
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.BusEnMemoria
import mx.gotadevida.core.mensajeria.BusRabbitMq
import mx.gotadevida.core.mensajeria.MongoEventosProcesados
import mx.gotadevida.core.notificaciones.EnviadorPush
import mx.gotadevida.core.notificaciones.EnviadorPushFcm
import mx.gotadevida.core.notificaciones.EnviadorPushRegistro
import mx.gotadevida.core.notificaciones.consumidorNotificaciones
import mx.gotadevida.core.seguridad.BCryptHasher
import mx.gotadevida.core.seguridad.HasherContrasenas
import mx.gotadevida.core.seguridad.JwtServicio
import mx.gotadevida.donador.repository.DonadorRepository
import mx.gotadevida.donador.repository.MongoDonadorRepository
import mx.gotadevida.donador.service.DonadorService
import mx.gotadevida.donador.service.ElegibilidadService
import mx.gotadevida.hospital.repository.HospitalRepository
import mx.gotadevida.hospital.repository.MongoHospitalRepository
import mx.gotadevida.hospital.service.HospitalService
import mx.gotadevida.hospital.service.HospitalesDemo
import mx.gotadevida.reglas.repository.MongoReglasRepository
import mx.gotadevida.reglas.repository.ReglasRepository
import mx.gotadevida.reglas.service.ReglasService
import mx.gotadevida.solicitud.consumidores.consumidorAvance
import mx.gotadevida.solicitud.consumidores.consumidorMatching
import mx.gotadevida.solicitud.repository.MongoRespuestaRepository
import mx.gotadevida.solicitud.repository.MongoSolicitudRepository
import mx.gotadevida.solicitud.repository.RespuestaRepository
import mx.gotadevida.solicitud.repository.SolicitudRepository
import mx.gotadevida.solicitud.service.AvanceService
import mx.gotadevida.solicitud.service.MatchingService
import mx.gotadevida.solicitud.service.SolicitudService
import java.time.Clock
import mx.gotadevida.cita.consumidores.RecordatorioService
import mx.gotadevida.cita.consumidores.consumidorRecordatorios
import mx.gotadevida.cita.repository.CitaRepository
import mx.gotadevida.cita.repository.MongoCitaRepository
import mx.gotadevida.cita.service.CitaService
import mx.gotadevida.donacion.repository.DonacionRepository
import mx.gotadevida.donacion.repository.MongoDonacionRepository
import mx.gotadevida.donacion.service.HistorialService
import mx.gotadevida.donacion.service.consumidorHistorial
import mx.gotadevida.hospital.service.EstadisticasService
import mx.gotadevida.chatbot.repository.ConversacionRepository
import mx.gotadevida.chatbot.repository.MongoConversacionRepository
import mx.gotadevida.chatbot.repository.MongoPreguntasFrecuentesRepository
import mx.gotadevida.chatbot.repository.PreguntasFrecuentesRepository
import mx.gotadevida.chatbot.service.AsistenteService
import mx.gotadevida.chatbot.service.ClienteGemini
import mx.gotadevida.chatbot.service.ClienteGeminiHttp
import mx.gotadevida.core.dominio.RequisitosVigentes
import mx.gotadevida.whatsapp.consumidores.consumidorChatbot
import mx.gotadevida.whatsapp.service.ClienteWhatsApp
import mx.gotadevida.whatsapp.service.ClienteWhatsAppMeta
import mx.gotadevida.whatsapp.service.WhatsAppRegistro
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Repositorios de MongoDB (o en memoria en las pruebas). */
class Repositorios(
    val usuarios: UsuarioRepository,
    val donadores: DonadorRepository,
    val hospitales: HospitalRepository,
    val reglas: ReglasRepository,
    val solicitudes: SolicitudRepository,
    val respuestas: RespuestaRepository,
    val citas: CitaRepository,
    val donaciones: DonacionRepository,
    val conversaciones: ConversacionRepository,
    val preguntas: PreguntasFrecuentesRepository,
)

/**
 * Inyección de dependencias por constructor, sin librerías: aquí se arma todo una sola vez.
 * Las pruebas usan [crear] con repositorios en memoria y un bus en memoria.
 */
class Dependencias(
    val jwt: JwtServicio,
    val autenticacion: AutenticacionService,
    val donadores: DonadorService,
    val hospitales: HospitalService,
    val reglas: ReglasService,
    val solicitudes: SolicitudService,
    val citas: CitaService,
    val historial: HistorialService,
    val estadisticas: EstadisticasService,
    val asistente: AsistenteService,
    val bus: BusDeEventos,
    val whatsappVerificacion: String?,
    val whatsappSecreto: String?,
    private val alPreparar: suspend () -> Unit,
) {
    /** Índices, datos iniciales y conexión a la cola. Se llama una vez antes de levantar el servidor. */
    suspend fun preparar() {
        alPreparar()
        bus.iniciar()
    }

    companion object {
        fun crear(
            jwt: JwtServicio,
            repos: Repositorios,
            hasher: HasherContrasenas,
            verificadorGoogle: VerificadorGoogle?,
            bus: BusDeEventos,
            push: EnviadorPush,
            gemini: ClienteGemini? = null,
            whatsapp: ClienteWhatsApp = WhatsAppRegistro(),
            whatsappVerificacion: String? = null,
            whatsappSecreto: String? = null,
            esperaEntreOleadas: Duration = 15.minutes,
            reloj: Clock = Clock.systemUTC(),
            alPreparar: suspend (Dependencias) -> Unit = {},
        ): Dependencias {
            val reglas = ReglasService(repos.reglas, reloj)
            val donadores = DonadorService(repos.donadores, reglas, ElegibilidadService(), reloj)
            val hospitales = HospitalService(repos.hospitales, reloj)
            val autenticacion = AutenticacionService(
                usuarios = repos.usuarios,
                jwt = jwt,
                hasher = hasher,
                verificadorGoogle = verificadorGoogle,
                crearPerfilDonador = donadores::crearPerfil,
                reloj = reloj,
            )
            val solicitudes = SolicitudService(
                solicitudes = repos.solicitudes,
                respuestas = repos.respuestas,
                donadores = donadores,
                hospitales = hospitales,
                hospitalDeUsuario = autenticacion::hospitalDe,
                bus = bus,
                reloj = reloj,
            )

            val citas = CitaService(repos.citas, donadores, hospitales, autenticacion::hospitalDe, bus, reloj)
            val historial = HistorialService(repos.donaciones, donadores, bus, reloj)
            val estadisticas = EstadisticasService(
                urgencias = solicitudes,
                hospitalDeUsuario = autenticacion::hospitalDe,
                donacionesDe = historial::contarDeHospital,
                citasHoyDe = { id -> citas.horariosOcupadosHoy(id) },
            )

            val asistente = AsistenteService(
                conversaciones = repos.conversaciones,
                preguntas = repos.preguntas,
                gemini = gemini,
                donadores = donadores,
                urgencias = solicitudes,
                citas = citas,
                requisitos = {
                    reglas.vigentes().let {
                        RequisitosVigentes(
                            it.edadMinima, it.edadMaxima, it.pesoMinimoKg, it.diasEntreDonaciones,
                            it.horasAyunoMinimo, it.horasAyunoMaximo, it.mesesEsperaTatuaje, it.requisitos,
                        )
                    }
                },
                reloj = reloj,
            )

            // Consumidores de la cola, todos en este mismo proceso.
            bus.registrar(consumidorMatching(MatchingService(repos.solicitudes, donadores, bus, esperaEntreOleadas, reloj = reloj)))
            bus.registrar(consumidorAvance(AvanceService(repos.solicitudes, repos.respuestas, bus, reloj)))
            bus.registrar(
                consumidorNotificaciones(autenticacion, push) { usuarioIds, datos ->
                    // Además del push, la urgencia llega por WhatsApp con la plantilla aprobada.
                    if (datos["tipo"] == "urgencia") {
                        usuarioIds.forEach { id ->
                            val d = donadores.resumen(id) ?: return@forEach
                            val tel = d.telefono ?: return@forEach
                            whatsapp.enviarUrgencia(tel, datos["tipoSangre"].orEmpty(), datos["hospital"].orEmpty(), datos["distanciaKm"].orEmpty())
                            asistente.recordarUrgencia(id, datos["solicitudId"].orEmpty(), null)
                        }
                    }
                },
            )
            bus.registrar(consumidorChatbot(asistente, donadores, whatsapp))
            bus.registrar(consumidorRecordatorios(RecordatorioService(citas, bus, reloj)))
            bus.registrar(consumidorHistorial(historial))

            lateinit var dependencias: Dependencias
            dependencias = Dependencias(jwt, autenticacion, donadores, hospitales, reglas, solicitudes, citas, historial, estadisticas, asistente, bus, whatsappVerificacion, whatsappSecreto) {
                alPreparar(dependencias)
            }
            return dependencias
        }

        fun produccion(config: Configuracion): Dependencias {
            val base = BaseDeDatos(config.mongoUri, config.mongoBaseDatos)
            val idempotencia = MongoEventosProcesados(base.db)
            val bus = config.rabbitMqUrl?.let { BusRabbitMq(it, idempotencia) } ?: BusEnMemoria(idempotencia)
            return crear(
                jwt = JwtServicio(config.jwtSecreto, config.jwtEmisor),
                repos = Repositorios(
                    usuarios = MongoUsuarioRepository(base.db),
                    donadores = MongoDonadorRepository(base.db),
                    hospitales = MongoHospitalRepository(base.db),
                    reglas = MongoReglasRepository(base.db),
                    solicitudes = MongoSolicitudRepository(base.db),
                    respuestas = MongoRespuestaRepository(base.db),
                    citas = MongoCitaRepository(base.db),
                    donaciones = MongoDonacionRepository(base.db),
                    conversaciones = MongoConversacionRepository(base.db),
                    preguntas = MongoPreguntasFrecuentesRepository(base.db),
                ),
                hasher = BCryptHasher(),
                verificadorGoogle = config.googleClientId?.let { VerificadorGoogleTokenInfo(it) },
                bus = bus,
                push = config.firebaseCredenciales?.let { EnviadorPushFcm(it) } ?: EnviadorPushRegistro(),
                esperaEntreOleadas = config.minutosEntreOleadas.minutes,
                gemini = config.geminiLlave?.let { ClienteGeminiHttp(it, config.geminiModelo) },
                whatsapp = if (config.whatsappToken != null && config.whatsappIdNumero != null) {
                    ClienteWhatsAppMeta(config.whatsappToken, config.whatsappIdNumero)
                } else {
                    WhatsAppRegistro()
                },
                whatsappVerificacion = config.whatsappTokenVerificacion,
                whatsappSecreto = config.whatsappSecretoApp,
            ) { deps ->
                deps.asistente.asegurarPreguntasFrecuentes()
                base.crearIndices()
                deps.reglas.asegurarReglas()
                if (config.adminEmail != null && config.adminPassword != null) {
                    deps.autenticacion.asegurarAdministrador(config.adminEmail, config.adminPassword)
                }
                if (config.cargarDatosDemo) HospitalesDemo.cargarSiFaltan(deps.hospitales)
            }
        }
    }
}
