package mx.gotadevida.prueba

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioPublico
import mx.gotadevida.autenticacion.repository.AutenticacionRepository
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.donador.entity.ActualizarPerfilSolicitud
import mx.gotadevida.donador.entity.Elegibilidad
import mx.gotadevida.donador.entity.PerfilDonador
import mx.gotadevida.donador.repository.DonadorRepository

val usuarioDePrueba = UsuarioPublico(id = "u1", nombre = "Ana López", email = "ana@correo.com", rol = "DONADOR")

class AutenticacionFalsa(var respuesta: Resultado<UsuarioPublico> = Resultado.Exito(usuarioDePrueba)) : AutenticacionRepository {
    val registros = mutableListOf<RegistroSolicitud>()
    val inicios = mutableListOf<Pair<String, String>>()

    override suspend fun registrar(solicitud: RegistroSolicitud): Resultado<UsuarioPublico> {
        registros += solicitud
        return respuesta
    }

    override suspend fun iniciarSesion(email: String, password: String): Resultado<UsuarioPublico> {
        inicios += email to password
        return respuesta
    }

    override suspend fun iniciarConGoogle(idToken: String): Resultado<UsuarioPublico> = respuesta
}

fun perfilDePrueba(completo: Boolean = true, ultimaDonacion: String? = null) = PerfilDonador(
    id = "u1",
    nombre = "Ana López",
    notificacionesActivas = true,
    radioMaximoKm = 15,
    totalDonaciones = if (ultimaDonacion != null) 1 else 0,
    perfilCompleto = completo,
    tipoSangre = if (completo) mx.gotadevida.donador.entity.TipoSangre.O_NEGATIVO else null,
    etiquetaTipoSangre = if (completo) "O−" else null,
    fechaNacimiento = if (completo) "1998-05-10" else null,
    pesoKg = if (completo) 62.5 else null,
    sexoParaCriteriosMedicos = if (completo) mx.gotadevida.donador.entity.SexoParaCriteriosMedicos.FEMENINO else null,
    latitud = if (completo) 25.54 else null,
    longitud = if (completo) -103.406 else null,
    zona = if (completo) "Torreón" else null,
    ultimaDonacion = ultimaDonacion
)

class DonadorFalso(
    perfil: PerfilDonador? = null,
    elegibilidad: Elegibilidad? = null,
    var respuestaRefresco: Resultado<Unit> = Resultado.Exito(Unit),
    var respuestaActualizar: Resultado<PerfilDonador>? = null
) : DonadorRepository {
    val perfil = MutableStateFlow(perfil)
    val elegibilidad = MutableStateFlow(elegibilidad)
    val actualizaciones = mutableListOf<ActualizarPerfilSolicitud>()

    override fun observarPerfil(): Flow<PerfilDonador?> = perfil

    override fun observarElegibilidad(): Flow<Elegibilidad?> = elegibilidad

    override suspend fun refrescar(): Resultado<Unit> = respuestaRefresco

    override suspend fun actualizarPerfil(cambios: ActualizarPerfilSolicitud): Resultado<PerfilDonador> {
        actualizaciones += cambios
        return respuestaActualizar ?: Resultado.Exito(perfilDePrueba())
    }
}
