package mx.gotadevida.autenticacion.service

import mx.gotadevida.autenticacion.entity.GoogleSolicitud
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RefrescoSolicitud
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.autenticacion.entity.Usuario
import mx.gotadevida.autenticacion.entity.UsuarioHospitalSolicitud
import mx.gotadevida.autenticacion.entity.UsuarioPublico
import mx.gotadevida.autenticacion.entity.aPublico
import mx.gotadevida.autenticacion.repository.UsuarioRepository
import mx.gotadevida.core.errores.Conflicto
import mx.gotadevida.core.errores.DatosInvalidos
import mx.gotadevida.core.errores.NoAutorizado
import mx.gotadevida.core.errores.validar
import mx.gotadevida.core.seguridad.HasherContrasenas
import mx.gotadevida.core.seguridad.JwtServicio
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.validacion.Validaciones.normalizarEmail
import mx.gotadevida.core.validacion.Validaciones.normalizarTelefono
import mx.gotadevida.core.validacion.Validaciones.validarContrasena
import mx.gotadevida.core.validacion.Validaciones.validarNombre
import java.time.Clock
import java.util.UUID

/**
 * Registro e inicio de sesión. Al registrar un donador también se crea su perfil
 * vacío por medio de [crearPerfilDonador] (lo provee la funcionalidad donador).
 */
class AutenticacionService(
    private val usuarios: UsuarioRepository,
    private val jwt: JwtServicio,
    private val hasher: HasherContrasenas,
    private val verificadorGoogle: VerificadorGoogle?,
    private val crearPerfilDonador: suspend (usuarioId: String, nombre: String, telefono: String?) -> Unit,
    private val reloj: Clock = Clock.systemUTC(),
) {

    suspend fun registrarDonador(solicitud: RegistroSolicitud): SesionRespuesta {
        validar(solicitud.aceptaAvisoPrivacidad) { "Debes aceptar el aviso de privacidad para crear tu cuenta." }
        val nombre = validarNombre(solicitud.nombre)
        val email = normalizarEmail(solicitud.email)
        validarContrasena(solicitud.password)
        val telefono = normalizarTelefono(solicitud.telefono)

        if (usuarios.buscarPorEmail(email) != null) throw Conflicto("Ya existe una cuenta con ese correo.")
        val usuario = Usuario(
            id = UUID.randomUUID().toString(),
            email = email,
            nombre = nombre,
            rol = Rol.DONADOR,
            creadoEn = reloj.instant().toString(),
            passwordHash = hasher.hashear(solicitud.password),
        )
        usuarios.crear(usuario)
        crearPerfilDonador(usuario.id, nombre, telefono)
        return sesion(usuario)
    }

    suspend fun iniciarSesion(solicitud: LoginSolicitud): SesionRespuesta {
        val usuario = usuarios.buscarPorEmail(solicitud.email.trim().lowercase()) ?: throw NoAutorizado()
        val hash = usuario.passwordHash ?: throw NoAutorizado("Esta cuenta inicia sesión con Google.")
        if (!hasher.verificar(solicitud.password, hash)) throw NoAutorizado()
        return sesion(usuario)
    }

    suspend fun iniciarSesionConGoogle(solicitud: GoogleSolicitud): SesionRespuesta {
        val verificador = verificadorGoogle
            ?: throw DatosInvalidos("El inicio de sesión con Google no está configurado en el servidor.")
        val cuenta = verificador.verificar(solicitud.idToken)
            ?: throw NoAutorizado("No pudimos verificar tu cuenta de Google.")

        usuarios.buscarPorGoogleId(cuenta.googleId)?.let { return sesion(it) }

        val email = cuenta.email.lowercase()
        val existente = usuarios.buscarPorEmail(email)
        if (existente != null) {
            // Vincula la cuenta de Google a la cuenta que ya tenía ese correo.
            val vinculado = existente.copy(googleId = cuenta.googleId)
            usuarios.actualizar(vinculado)
            return sesion(vinculado)
        }

        val nuevo = Usuario(
            id = UUID.randomUUID().toString(),
            email = email,
            nombre = cuenta.nombre,
            rol = Rol.DONADOR,
            creadoEn = reloj.instant().toString(),
            googleId = cuenta.googleId,
        )
        usuarios.crear(nuevo)
        crearPerfilDonador(nuevo.id, nuevo.nombre, null)
        return sesion(nuevo)
    }

    suspend fun refrescar(solicitud: RefrescoSolicitud): SesionRespuesta {
        val usuarioId = jwt.verificarRefresco(solicitud.tokenRefresco)
        val usuario = usuarios.buscarPorId(usuarioId) ?: throw NoAutorizado("Tu sesión expiró. Inicia sesión de nuevo.")
        return sesion(usuario)
    }

    /** Crea la cuenta con la que el personal de un banco de sangre entra a la app. */
    suspend fun crearUsuarioHospital(hospitalId: String, solicitud: UsuarioHospitalSolicitud): UsuarioPublico {
        val email = normalizarEmail(solicitud.email)
        validarContrasena(solicitud.password)
        val usuario = Usuario(
            id = UUID.randomUUID().toString(),
            email = email,
            nombre = validarNombre(solicitud.nombre),
            rol = Rol.HOSPITAL,
            creadoEn = reloj.instant().toString(),
            passwordHash = hasher.hashear(solicitud.password),
            hospitalId = hospitalId,
        )
        usuarios.crear(usuario)
        return usuario.aPublico()
    }

    /** Crea el administrador inicial si no existe (lo usa la carga de datos al arrancar). */
    suspend fun asegurarAdministrador(email: String, password: String) {
        val correo = normalizarEmail(email)
        if (usuarios.buscarPorEmail(correo) != null) return
        usuarios.crear(
            Usuario(
                id = UUID.randomUUID().toString(),
                email = correo,
                nombre = "Administración Gota de Vida",
                rol = Rol.ADMIN,
                creadoEn = reloj.instant().toString(),
                passwordHash = hasher.hashear(password),
            ),
        )
    }

    private fun sesion(usuario: Usuario) = SesionRespuesta(
        usuario = usuario.aPublico(),
        tokens = jwt.crearTokens(usuario.id, usuario.rol),
    )
}
