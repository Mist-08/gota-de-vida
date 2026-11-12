package mx.gotadevida.core.seguridad

import at.favre.lib.crypto.bcrypt.BCrypt

/** Convierte contraseñas en hash y las verifica. Nunca se guardan en texto plano. */
interface HasherContrasenas {
    fun hashear(contrasena: String): String

    fun verificar(contrasena: String, hash: String): Boolean
}

class BCryptHasher(private val costo: Int = 12) : HasherContrasenas {
    override fun hashear(contrasena: String): String =
        BCrypt.withDefaults().hashToString(costo, contrasena.toCharArray())

    override fun verificar(contrasena: String, hash: String): Boolean =
        BCrypt.verifyer().verify(contrasena.toCharArray(), hash).verified
}
