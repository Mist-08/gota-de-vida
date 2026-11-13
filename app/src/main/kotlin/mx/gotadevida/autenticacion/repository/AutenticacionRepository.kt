package mx.gotadevida.autenticacion.repository

import javax.inject.Inject
import mx.gotadevida.autenticacion.entity.GoogleSolicitud
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RecuperarSolicitud
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.RestablecerSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.autenticacion.entity.UsuarioPublico
import mx.gotadevida.core.network.Resultado
import mx.gotadevida.core.network.llamarApi
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.core.sesion.Sesion
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoints de autenticación (docs: /swagger del backend). */
interface AutenticacionApi {
    @POST("api/v1/auth/registro")
    suspend fun registrar(@Body solicitud: RegistroSolicitud): SesionRespuesta

    @POST("api/v1/auth/login")
    suspend fun iniciarSesion(@Body solicitud: LoginSolicitud): SesionRespuesta

    @POST("api/v1/auth/google")
    suspend fun iniciarConGoogle(@Body solicitud: GoogleSolicitud): SesionRespuesta

    @POST("api/v1/auth/recuperar")
    suspend fun recuperar(@Body solicitud: RecuperarSolicitud)

    @POST("api/v1/auth/restablecer")
    suspend fun restablecer(@Body solicitud: RestablecerSolicitud): SesionRespuesta
}

interface AutenticacionRepository {
    suspend fun registrar(solicitud: RegistroSolicitud): Resultado<UsuarioPublico>

    suspend fun iniciarSesion(email: String, password: String): Resultado<UsuarioPublico>

    suspend fun iniciarConGoogle(idToken: String): Resultado<UsuarioPublico>

    /** Pide que llegue al correo un código para cambiar la contraseña. */
    suspend fun solicitarRecuperacion(email: String): Resultado<Unit>

    /** Cambia la contraseña con el código del correo y deja la sesión iniciada. */
    suspend fun restablecerContrasena(email: String, codigo: String, password: String): Resultado<UsuarioPublico>
}

class AutenticacionRepositoryRed @Inject constructor(private val api: AutenticacionApi, private val almacen: AlmacenSesion) :
    AutenticacionRepository {

    override suspend fun registrar(solicitud: RegistroSolicitud) = entrar { api.registrar(solicitud) }

    override suspend fun iniciarSesion(email: String, password: String) =
        entrar { api.iniciarSesion(LoginSolicitud(email.trim(), password)) }

    override suspend fun iniciarConGoogle(idToken: String) = entrar { api.iniciarConGoogle(GoogleSolicitud(idToken)) }

    override suspend fun solicitarRecuperacion(email: String) = llamarApi { api.recuperar(RecuperarSolicitud(email.trim())) }

    override suspend fun restablecerContrasena(email: String, codigo: String, password: String) =
        entrar { api.restablecer(RestablecerSolicitud(email.trim(), codigo, password)) }

    /** Llama a la API y, si sale bien, guarda la sesión en el teléfono. */
    private suspend fun entrar(llamada: suspend () -> SesionRespuesta): Resultado<UsuarioPublico> = when (val r = llamarApi { llamada() }) {
        is Resultado.Fallo -> r

        is Resultado.Exito -> {
            val (usuario, tokens) = r.valor
            almacen.guardar(
                Sesion(
                    usuarioId = usuario.id,
                    nombre = usuario.nombre,
                    email = usuario.email,
                    rol = usuario.rol,
                    tokenAcceso = tokens.tokenAcceso,
                    tokenRefresco = tokens.tokenRefresco
                )
            )
            Resultado.Exito(usuario)
        }
    }
}
