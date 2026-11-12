package mx.gotadevida.soporte

import mx.gotadevida.autenticacion.entity.Usuario
import mx.gotadevida.autenticacion.repository.UsuarioRepository
import mx.gotadevida.core.errores.Conflicto
import mx.gotadevida.core.seguridad.HasherContrasenas
import mx.gotadevida.donador.entity.Donador
import mx.gotadevida.donador.repository.DonadorRepository
import mx.gotadevida.hospital.entity.Hospital
import mx.gotadevida.hospital.repository.HospitalRepository
import mx.gotadevida.reglas.entity.ReglasElegibilidad
import mx.gotadevida.reglas.repository.ReglasRepository

class UsuariosEnMemoria : UsuarioRepository {
    val datos = linkedMapOf<String, Usuario>()

    override suspend fun crear(usuario: Usuario) {
        if (datos.values.any { it.email == usuario.email }) throw Conflicto("Ya existe una cuenta con ese correo.")
        datos[usuario.id] = usuario
    }

    override suspend fun buscarPorId(id: String) = datos[id]

    override suspend fun buscarPorEmail(email: String) = datos.values.firstOrNull { it.email == email }

    override suspend fun buscarPorGoogleId(googleId: String) = datos.values.firstOrNull { it.googleId == googleId }

    override suspend fun actualizar(usuario: Usuario) {
        datos[usuario.id] = usuario
    }
}

class DonadoresEnMemoria : DonadorRepository {
    val datos = linkedMapOf<String, Donador>()

    override suspend fun buscarPorId(id: String) = datos[id]

    override suspend fun guardar(donador: Donador) {
        datos[donador.id] = donador
    }
}

class HospitalesEnMemoria : HospitalRepository {
    val datos = linkedMapOf<String, Hospital>()

    override suspend fun crear(hospital: Hospital) {
        datos[hospital.id] = hospital
    }

    override suspend fun buscarPorId(id: String) = datos[id]

    override suspend fun listar() = datos.values.sortedWith(compareBy({ it.municipio }, { it.nombre }))

    override suspend fun contar() = datos.size.toLong()
}

class ReglasEnMemoria : ReglasRepository {
    var reglas: ReglasElegibilidad? = null

    override suspend fun obtenerVigentes() = reglas

    override suspend fun guardar(reglas: ReglasElegibilidad) {
        this.reglas = reglas
    }
}

/** Hasher rápido para pruebas (BCrypt real tarda a propósito). */
class HasherDePrueba : HasherContrasenas {
    override fun hashear(contrasena: String) = "hash:$contrasena"

    override fun verificar(contrasena: String, hash: String) = hash == "hash:$contrasena"
}
