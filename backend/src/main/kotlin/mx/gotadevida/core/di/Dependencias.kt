package mx.gotadevida.core.di

import mx.gotadevida.autenticacion.repository.MongoUsuarioRepository
import mx.gotadevida.autenticacion.repository.UsuarioRepository
import mx.gotadevida.autenticacion.service.AutenticacionService
import mx.gotadevida.autenticacion.service.VerificadorGoogle
import mx.gotadevida.autenticacion.service.VerificadorGoogleTokenInfo
import mx.gotadevida.core.config.Configuracion
import mx.gotadevida.core.db.BaseDeDatos
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
import java.time.Clock

/**
 * Inyección de dependencias por constructor, sin librerías: aquí se arma todo una sola vez.
 * Las pruebas usan [crear] con repositorios en memoria en lugar de MongoDB.
 */
class Dependencias(
    val jwt: JwtServicio,
    val autenticacion: AutenticacionService,
    val donadores: DonadorService,
    val hospitales: HospitalService,
    val reglas: ReglasService,
    private val alPreparar: suspend () -> Unit,
) {
    /** Índices y datos iniciales. Se llama una vez antes de levantar el servidor. */
    suspend fun preparar() = alPreparar()

    companion object {
        fun crear(
            jwt: JwtServicio,
            usuarios: UsuarioRepository,
            donadoresRepo: DonadorRepository,
            hospitalesRepo: HospitalRepository,
            reglasRepo: ReglasRepository,
            hasher: HasherContrasenas,
            verificadorGoogle: VerificadorGoogle?,
            reloj: Clock = Clock.systemUTC(),
            alPreparar: suspend (Dependencias) -> Unit = {},
        ): Dependencias {
            val reglas = ReglasService(reglasRepo, reloj)
            val donadores = DonadorService(donadoresRepo, reglas, ElegibilidadService(), reloj)
            val hospitales = HospitalService(hospitalesRepo, reloj)
            val autenticacion = AutenticacionService(
                usuarios = usuarios,
                jwt = jwt,
                hasher = hasher,
                verificadorGoogle = verificadorGoogle,
                crearPerfilDonador = donadores::crearPerfil,
                reloj = reloj,
            )
            lateinit var dependencias: Dependencias
            dependencias = Dependencias(jwt, autenticacion, donadores, hospitales, reglas) { alPreparar(dependencias) }
            return dependencias
        }

        fun produccion(config: Configuracion): Dependencias {
            val base = BaseDeDatos(config.mongoUri, config.mongoBaseDatos)
            return crear(
                jwt = JwtServicio(config.jwtSecreto, config.jwtEmisor),
                usuarios = MongoUsuarioRepository(base.db),
                donadoresRepo = MongoDonadorRepository(base.db),
                hospitalesRepo = MongoHospitalRepository(base.db),
                reglasRepo = MongoReglasRepository(base.db),
                hasher = BCryptHasher(),
                verificadorGoogle = config.googleClientId?.let { VerificadorGoogleTokenInfo(it) },
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
