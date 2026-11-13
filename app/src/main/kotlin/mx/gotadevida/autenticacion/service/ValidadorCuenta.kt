package mx.gotadevida.autenticacion.service

import mx.gotadevida.autenticacion.entity.CampoCuenta
import mx.gotadevida.autenticacion.entity.ErrorCampo

/**
 * Mismas reglas que el backend (core/validacion/Validaciones.kt), para avisar antes de enviar.
 * El servidor vuelve a validar: esto solo mejora la experiencia.
 */
object ValidadorCuenta {
    private val PATRON_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun nombre(valor: String): ErrorCampo? = when {
        valor.isBlank() -> ErrorCampo.VACIO
        valor.trim().length < 2 -> ErrorCampo.NOMBRE_CORTO
        else -> null
    }

    fun email(valor: String): ErrorCampo? = when {
        valor.isBlank() -> ErrorCampo.VACIO
        !PATRON_EMAIL.matches(valor.trim().lowercase()) -> ErrorCampo.EMAIL_INVALIDO
        else -> null
    }

    fun password(valor: String): ErrorCampo? = when {
        valor.isEmpty() -> ErrorCampo.VACIO
        valor.length < 8 -> ErrorCampo.PASSWORD_CORTA
        else -> null
    }

    /** Celular de México: 10 dígitos, acepta espacios, guiones y +52. */
    fun telefono(valor: String): ErrorCampo? {
        if (valor.isBlank()) return ErrorCampo.VACIO
        val digitos = valor.filter { it.isDigit() }
        val nacional = if (digitos.length == 12 && digitos.startsWith("52")) digitos.drop(2) else digitos
        return if (nacional.length == 10) null else ErrorCampo.TELEFONO_INVALIDO
    }

    fun registro(nombre: String, email: String, password: String, telefono: String): Map<CampoCuenta, ErrorCampo> = buildMap {
        nombre(nombre)?.let { put(CampoCuenta.NOMBRE, it) }
        email(email)?.let { put(CampoCuenta.EMAIL, it) }
        password(password)?.let { put(CampoCuenta.PASSWORD, it) }
        telefono(telefono)?.let { put(CampoCuenta.TELEFONO, it) }
    }

    fun inicioSesion(email: String, password: String): Map<CampoCuenta, ErrorCampo> = buildMap {
        email(email)?.let { put(CampoCuenta.EMAIL, it) }
        if (password.isEmpty()) put(CampoCuenta.PASSWORD, ErrorCampo.VACIO)
    }
}
