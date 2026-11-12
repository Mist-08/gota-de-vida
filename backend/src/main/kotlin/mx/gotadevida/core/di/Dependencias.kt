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
    val bus: BusDeEventos,
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

            // Consumidores de la cola, todos en este mismo proceso.
            bus.registrar(consumidorMatching(MatchingService(repos.solicitudes, donadores, bus, esperaEntreOleadas, reloj = reloj)))
            bus.registrar(consumidorAvance(AvanceService(repos.solicitudes, repos.respuestas, bus, reloj)))
            bus.registrar(consumidorNotificaciones(autenticacion, push))

            lateinit var dependencias: Dependencias
            dependencias = Dependencias(jwt, autenticacion, donadores, hospitales, reglas, solicitudes, bus) {
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
                ),
                hasher = BCryptHasher(),
                verificadorGoogle = config.googleClientId?.let { VerificadorGoogleTokenInfo(it) },
                bus = bus,
                push = config.firebaseCredenciales?.let { EnviadorPushFcm(it) } ?: EnviadorPushRegistro(),
                esperaEntreOleadas = config.minutosEntreOleadas.minutes,
            ) { deps ->
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
