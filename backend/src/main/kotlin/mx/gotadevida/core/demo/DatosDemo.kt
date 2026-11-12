package mx.gotadevida.core.demo

import java.time.LocalDate
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioHospitalSolicitud
import mx.gotadevida.autenticacion.service.AutenticacionService
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.core.errores.ErrorDeNegocio
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.service.DonadorService
import mx.gotadevida.hospital.service.HospitalService
import org.slf4j.LoggerFactory

/**
 * Datos de la demo: una cuenta de banco de sangre por hospital de demostración
 * y donadores ficticios en Torreón, Gómez Palacio y Lerdo con distintos tipos de sangre.
 *
 * Solo se cargan si existen `CARGAR_DATOS_DEMO=true` y `DEMO_PASSWORD` (la contraseña de todas
 * las cuentas de demo). Las personas son inventadas; los celulares usan el prefijo 871-000.
 * Se puede llamar en cada arranque: si una cuenta ya existe, la salta.
 */
object DatosDemo {
    private val log = LoggerFactory.getLogger(DatosDemo::class.java)

    private data class DonadorDemo(
        val nombre: String,
        val tipo: TipoSangre,
        val sexo: SexoParaCriteriosMedicos,
        val latitud: Double,
        val longitud: Double,
        val zona: String,
        /** Días desde su última donación (null = nunca ha donado). */
        val diasDesdeDonacion: Long?
    )

    private val donadores = listOf(
        DonadorDemo("Ana Demo", TipoSangre.O_NEGATIVO, SexoParaCriteriosMedicos.FEMENINO, 25.541, -103.418, "Centro, Torreón", null),
        DonadorDemo("Beto Demo", TipoSangre.O_NEGATIVO, SexoParaCriteriosMedicos.MASCULINO, 25.560, -103.390, "Torreón Jardín", 120),
        DonadorDemo("Carla Demo", TipoSangre.O_POSITIVO, SexoParaCriteriosMedicos.FEMENINO, 25.530, -103.440, "Torreón", 30),
        DonadorDemo("Diego Demo", TipoSangre.A_POSITIVO, SexoParaCriteriosMedicos.MASCULINO, 25.548, -103.405, "Torreón", null),
        DonadorDemo("Elena Demo", TipoSangre.A_NEGATIVO, SexoParaCriteriosMedicos.FEMENINO, 25.570, -103.500, "Centro, Gómez Palacio", 90),
        DonadorDemo("Fer Demo", TipoSangre.B_POSITIVO, SexoParaCriteriosMedicos.MASCULINO, 25.580, -103.480, "Gómez Palacio", null),
        DonadorDemo("Gaby Demo", TipoSangre.O_POSITIVO, SexoParaCriteriosMedicos.FEMENINO, 25.555, -103.520, "Gómez Palacio", 200),
        DonadorDemo("Hugo Demo", TipoSangre.AB_POSITIVO, SexoParaCriteriosMedicos.MASCULINO, 25.590, -103.510, "Gómez Palacio", null),
        DonadorDemo("Irma Demo", TipoSangre.O_NEGATIVO, SexoParaCriteriosMedicos.FEMENINO, 25.540, -103.520, "Centro, Lerdo", null),
        DonadorDemo("Javier Demo", TipoSangre.B_NEGATIVO, SexoParaCriteriosMedicos.MASCULINO, 25.535, -103.530, "Lerdo", 70),
        DonadorDemo("Karla Demo", TipoSangre.A_POSITIVO, SexoParaCriteriosMedicos.FEMENINO, 25.530, -103.515, "Lerdo", 10),
        DonadorDemo("Luis Demo", TipoSangre.O_POSITIVO, SexoParaCriteriosMedicos.MASCULINO, 25.600, -103.450, "Torreón Oriente", null)
    )

    suspend fun cargar(
        password: String,
        autenticacion: AutenticacionService,
        donadorService: DonadorService,
        hospitales: HospitalService,
        hoy: LocalDate
    ) {
        hospitales.listar().filter { it.esDemostracion }.forEachIndexed { i, h ->
            intentar {
                autenticacion.crearUsuarioHospital(
                    h.id,
                    UsuarioHospitalSolicitud("Banco de sangre ${h.nombre}", "banco${i + 1}@demo.gotadevida.mx", password)
                )
            }
        }
        donadores.forEachIndexed { i, d ->
            intentar {
                val numero = i + 1
                val sesion = autenticacion.registrarDonador(
                    RegistroSolicitud(
                        d.nombre,
                        "donador$numero@demo.gotadevida.mx",
                        password,
                        "871000%04d".format(numero),
                        aceptaAvisoPrivacidad = true
                    )
                )
                donadorService.actualizarPerfil(
                    sesion.usuario.id,
                    ActualizarPerfilSolicitud(
                        tipoSangre = d.tipo,
                        fechaNacimiento = hoy.minusYears(20L + numero).toString(),
                        pesoKg = 58.0 + numero * 2,
                        sexoParaCriteriosMedicos = d.sexo,
                        latitud = d.latitud,
                        longitud = d.longitud,
                        zona = d.zona,
                        fechaUltimaDonacion = d.diasDesdeDonacion?.let { hoy.minusDays(it).toString() }
                    )
                )
            }
        }
        log.info("Datos de demo listos: cuentas banco#@demo.gotadevida.mx y donador#@demo.gotadevida.mx")
    }

    /** Si la cuenta ya existe (409), no pasa nada: el seed se puede repetir. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: ErrorDeNegocio) {
            // Ya existía.
        }
    }
}
