package mx.gotadevida.core.validacion

import mx.gotadevida.core.errores.validar

/** Validaciones de datos personales que usan varias funcionalidades. */
object Validaciones {
    private val PATRON_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizarEmail(email: String): String {
        val limpio = email.trim().lowercase()
        validar(PATRON_EMAIL.matches(limpio)) { "Escribe un correo válido." }
        return limpio
    }

    fun validarNombre(nombre: String): String {
        val limpio = nombre.trim()
        validar(limpio.length in 2..80) { "El nombre debe tener entre 2 y 80 caracteres." }
        return limpio
    }

    fun validarContrasena(password: String) {
        validar(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }
    }

    /** Celular de México: 10 dígitos. Acepta espacios, guiones y el prefijo +52. */
    fun normalizarTelefono(telefono: String): String {
        val todos = telefono.filter { it.isDigit() }
        val digitos = if (todos.length == 12 && todos.startsWith("52")) todos.drop(2) else todos
        validar(digitos.length == 10) { "El celular debe tener 10 dígitos." }
        return digitos
    }
}
