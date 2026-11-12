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
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.solicitud.entity.EstadoSolicitud
import mx.gotadevida.solicitud.entity.Respuesta
import mx.gotadevida.solicitud.entity.Solicitud
import mx.gotadevida.solicitud.repository.RespuestaRepository
import mx.gotadevida.solicitud.repository.SolicitudRepository
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

    override suspend fun buscarPorIds(ids: Collection<String>) = ids.mapNotNull { datos[it] }

    override suspend fun deHospital(hospitalId: String) = datos.values.filter { it.hospitalId == hospitalId }

    override suspend fun conTokenFcm(token: String) = datos.values.filter { token in it.tokensFcm }
}

class DonadoresEnMemoria : DonadorRepository {
    val datos = linkedMapOf<String, Donador>()

    override suspend fun buscarPorId(id: String) = datos[id]

    override suspend fun guardar(donador: Donador) {
        datos[donador.id] = donador
    }

    /** Igual que `$geoNear`: filtra por tipo, notificaciones y radio, y ordena por distancia. */
    override suspend fun cercanos(centro: PuntoGeo, radioKm: Double, tipos: List<TipoSangre>, excluir: Set<String>, limite: Int) =
        datos.values
            .filter { it.tipoSangre in tipos && it.notificacionesActivas && it.id !in excluir && it.ubicacion != null }
            .map { it to it.ubicacion!!.distanciaKmA(centro) }
            .filter { it.second <= radioKm }
            .sortedBy { it.second }
            .take(limite)
            .map { it.first }
}

class SolicitudesEnMemoria : SolicitudRepository {
    val datos = linkedMapOf<String, Solicitud>()

    override suspend fun guardar(solicitud: Solicitud) {
        datos[solicitud.id] = solicitud
    }

    override suspend fun buscarPorId(id: String) = datos[id]

    override suspend fun activasCerca(centro: PuntoGeo, radioKm: Double, tipos: List<TipoSangre>) =
        datos.values.filter { it.estado == EstadoSolicitud.ACTIVA && it.tipoSangre in tipos && it.ubicacion.distanciaKmA(centro) <= radioKm }

    override suspend fun delHospital(hospitalId: String) = datos.values.filter { it.hospitalId == hospitalId }.sortedByDescending { it.creadaEn }
}

class RespuestasEnMemoria : RespuestaRepository {
    val datos = linkedMapOf<String, Respuesta>()

    override suspend fun guardar(respuesta: Respuesta) {
        datos[respuesta.id] = respuesta
    }

    override suspend fun buscar(solicitudId: String, donadorId: String) = datos["$solicitudId:$donadorId"]

    override suspend fun deSolicitud(solicitudId: String) = datos.values.filter { it.solicitudId == solicitudId }

    override suspend fun deDonador(donadorId: String) = datos.values.filter { it.donadorId == donadorId }
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
