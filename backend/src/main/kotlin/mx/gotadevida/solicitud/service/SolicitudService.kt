package mx.gotadevida.solicitud.service

import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import mx.gotadevida.core.dominio.DirectorioDonadores
import mx.gotadevida.core.dominio.DirectorioHospitales
import mx.gotadevida.core.dominio.DirectorioUrgencias
import mx.gotadevida.core.dominio.PuntoGeo
import mx.gotadevida.core.dominio.TipoSangre
import mx.gotadevida.core.dominio.UrgenciaResumen
import mx.gotadevida.core.errores.Conflicto
import mx.gotadevida.core.errores.NoEncontrado
import mx.gotadevida.core.errores.Prohibido
import mx.gotadevida.core.errores.validar
import mx.gotadevida.core.mensajeria.BusDeEventos
import mx.gotadevida.core.mensajeria.TiposEvento
import mx.gotadevida.core.seguridad.Rol
import mx.gotadevida.core.seguridad.UsuarioActual
import mx.gotadevida.solicitud.entity.CrearSolicitudPeticion
import mx.gotadevida.solicitud.entity.DonadorQueAcepto
import mx.gotadevida.solicitud.entity.EstadoSolicitud
import mx.gotadevida.solicitud.entity.Respuesta
import mx.gotadevida.solicitud.entity.Solicitud
import mx.gotadevida.solicitud.entity.SolicitudRespuesta
import mx.gotadevida.solicitud.entity.aRespuesta
import mx.gotadevida.solicitud.repository.RespuestaRepository
import mx.gotadevida.solicitud.repository.SolicitudRepository

/**
 * Urgencias: el hospital las publica, los donadores compatibles las ven y responden.
 * Publicar o responder solo guarda y lanza un evento; avisar a los donadores y
 * actualizar el contador lo hacen los consumidores de la cola ([MatchingService], [AvanceService]).
 */
class SolicitudService(
    private val solicitudes: SolicitudRepository,
    private val respuestas: RespuestaRepository,
    private val donadores: DirectorioDonadores,
    private val hospitales: DirectorioHospitales,
    private val hospitalDeUsuario: suspend (usuarioId: String) -> String?,
    private val bus: BusDeEventos,
    private val reloj: Clock = Clock.systemUTC()
) : DirectorioUrgencias {
    suspend fun crear(usuario: UsuarioActual, peticion: CrearSolicitudPeticion): SolicitudRespuesta {
        val hospitalId = hospitalDeUsuario(usuario.id) ?: throw Prohibido("Tu cuenta no pertenece a un hospital.")
        val hospital = hospitales.resumen(hospitalId) ?: throw NoEncontrado("No existe tu hospital.")
        validar(peticion.unidades in 1..UNIDADES_MAXIMAS) { "Las unidades deben estar entre 1 y $UNIDADES_MAXIMAS." }
        val nota = peticion.nota?.trim()?.takeIf { it.isNotEmpty() }
        validar((nota?.length ?: 0) <= 200) { "La nota debe tener máximo 200 caracteres." }
        val ahora = reloj.instant().toString()
        val solicitud = Solicitud(
            id = UUID.randomUUID().toString(),
            hospitalId = hospital.id,
            hospitalNombre = hospital.nombre,
            hospitalDireccion = hospital.direccion,
            ubicacion = hospital.ubicacion,
            tipoSangre = peticion.tipoSangre,
            unidades = peticion.unidades,
            unidadesComprometidas = 0,
            estado = EstadoSolicitud.ACTIVA,
            creadaEn = ahora,
            actualizadaEn = ahora,
            nota = nota
        )
        solicitudes.guardar(solicitud)
        bus.publicar(TiposEvento.SOLICITUD_CREADA, buildJsonObject { put("solicitudId", solicitud.id) })
        return solicitud.aRespuesta(aceptaciones = emptyList())
    }

    /** Urgencias activas que el donador puede cubrir, de la más cercana a la más lejana. */
    suspend fun cercanas(donadorId: String, latitud: Double?, longitud: Double?, radioKm: Double?): List<SolicitudRespuesta> {
        val donador = donadores.resumen(donadorId) ?: throw NoEncontrado("No encontramos tu perfil de donador.")
        val tipo = donador.tipoSangre ?: return emptyList()
        validar((latitud == null) == (longitud == null)) { "Manda lat y lng juntas." }
        val centro = if (latitud != null && longitud != null) PuntoGeo.de(latitud, longitud) else donador.ubicacion
        centro ?: return emptyList()
        val radio = (radioKm ?: donador.radioMaximoKm.toDouble())
        validar(radio in 1.0..100.0) { "El radio debe estar entre 1 y 100 km." }
        val receptores = TipoSangre.entries.filter { tipo.puedeDonarA(it) }
        val mias = respuestas.deDonador(donadorId).associateBy { it.solicitudId }
        return solicitudes.activasCerca(centro, radio, receptores)
            .map { it to centro.distanciaKmA(it.ubicacion) }
            .sortedBy { it.second }
            .map { (s, d) -> s.aRespuesta(distanciaKm = d, miRespuesta = mias[s.id]?.acepta) }
    }

    suspend fun detalle(usuario: UsuarioActual, id: String): SolicitudRespuesta {
        val solicitud = buscar(id)
        return when (usuario.rol) {
            Rol.DONADOR -> {
                val donador = donadores.resumen(usuario.id)
                solicitud.aRespuesta(
                    distanciaKm = donador?.ubicacion?.distanciaKmA(solicitud.ubicacion),
                    miRespuesta = respuestas.buscar(id, usuario.id)?.acepta
                )
            }

            Rol.HOSPITAL, Rol.ADMIN -> {
                exigirDueno(usuario, solicitud)
                solicitud.aRespuesta(aceptaciones = aceptaciones(solicitud))
            }
        }
    }

    suspend fun delHospital(usuario: UsuarioActual): List<SolicitudRespuesta> {
        val hospitalId = hospitalDeUsuario(usuario.id) ?: throw Prohibido("Tu cuenta no pertenece a un hospital.")
        return solicitudes.delHospital(hospitalId).map { it.aRespuesta(aceptaciones = aceptaciones(it)) }
    }

    suspend fun cambiarEstado(usuario: UsuarioActual, id: String, estado: EstadoSolicitud): SolicitudRespuesta {
        val solicitud = buscar(id)
        exigirDueno(usuario, solicitud)
        validar(estado == EstadoSolicitud.CERRADA || estado == EstadoSolicitud.CANCELADA) {
            "Solo puedes cerrar o cancelar una urgencia."
        }
        if (solicitud.estado == EstadoSolicitud.CERRADA || solicitud.estado == EstadoSolicitud.CANCELADA) {
            throw Conflicto("Esta urgencia ya terminó.")
        }
        val actualizada = solicitud.copy(estado = estado, actualizadaEn = reloj.instant().toString())
        solicitudes.guardar(actualizada)
        return actualizada.aRespuesta(aceptaciones = aceptaciones(actualizada))
    }

    suspend fun responder(donadorId: String, id: String, acepta: Boolean): SolicitudRespuesta {
        val solicitud = buscar(id)
        val donador = donadores.resumen(donadorId) ?: throw NoEncontrado("No encontramos tu perfil de donador.")
        val anterior = respuestas.buscar(id, donadorId)
        if (anterior != null && anterior.acepta == acepta) {
            return solicitud.aRespuesta(donador.ubicacion?.distanciaKmA(solicitud.ubicacion), acepta)
        }
        if (anterior?.acepta == true) throw Conflicto("Ya aceptaste esta urgencia. Si no puedes ir, cancela tu cita.")
        if (acepta) {
            if (solicitud.estado == EstadoSolicitud.CUBIERTA) throw Conflicto("¡Gracias! Esta urgencia ya se cubrió.")
            if (solicitud.estado != EstadoSolicitud.ACTIVA) throw Conflicto("Esta urgencia ya terminó.")
            val tipo = donador.tipoSangre ?: throw Conflicto("Completa tu tipo de sangre para responder urgencias.")
            if (!tipo.puedeDonarA(
                    solicitud.tipoSangre
                )
            ) {
                throw Conflicto("Tu tipo de sangre no es compatible con esta urgencia.")
            }
            if (!donador.elegible) throw Conflicto("Por ahora no puedes donar. Revisa tu elegibilidad en el inicio.")
        }
        val distancia = donador.ubicacion?.distanciaKmA(solicitud.ubicacion)
        respuestas.guardar(Respuesta("$id:$donadorId", id, donadorId, acepta, distancia, reloj.instant().toString()))
        bus.publicar(
            TiposEvento.RESPUESTA_REGISTRADA,
            buildJsonObject {
                put("solicitudId", id)
                put("donadorId", donadorId)
                put("acepta", acepta)
            }
        )
        val comprometidas = respuestas.deSolicitud(id).count { it.acepta }
        return solicitud.copy(unidadesComprometidas = comprometidas).aRespuesta(distancia, acepta)
    }

    override suspend fun deHospital(hospitalId: String): List<UrgenciaResumen> =
        solicitudes.delHospital(hospitalId).map { it.resumen(null, null) }

    override suspend fun paraDonador(donadorId: String): List<UrgenciaResumen> =
        cercanas(donadorId, null, null, null).map { r -> buscar(r.id).resumen(r.distanciaKm, r.miRespuesta) }

    override suspend fun responderUrgencia(donadorId: String, solicitudId: String, acepta: Boolean): UrgenciaResumen {
        val r = responder(donadorId, solicitudId, acepta)
        return buscar(
            solicitudId
        ).copy(unidadesComprometidas = r.unidadesComprometidas).resumen(r.distanciaKm, r.miRespuesta)
    }

    private fun Solicitud.resumen(distanciaKm: Double?, miRespuesta: Boolean?) = UrgenciaResumen(
        id, hospitalId, hospitalNombre, tipoSangre.etiqueta, tipoSangre, estado.name, unidades, unidadesComprometidas,
        creadaEn, cubiertaEn, distanciaKm, miRespuesta
    )

    private suspend fun aceptaciones(solicitud: Solicitud): List<DonadorQueAcepto> =
        respuestas.deSolicitud(solicitud.id).filter { it.acepta }.map { r ->
            val d = donadores.resumen(r.donadorId)
            DonadorQueAcepto(
                donadorId = r.donadorId,
                nombre = d?.nombre ?: "Donador",
                telefono = d?.telefono,
                distanciaKm = r.distanciaKm?.let { Math.round(it * 10) / 10.0 },
                respondioEn = r.creadaEn
            )
        }

    private suspend fun exigirDueno(usuario: UsuarioActual, solicitud: Solicitud) {
        if (usuario.rol == Rol.ADMIN) return
        if (usuario.rol != Rol.HOSPITAL || hospitalDeUsuario(usuario.id) != solicitud.hospitalId) throw Prohibido()
    }

    private suspend fun buscar(id: String) = solicitudes.buscarPorId(id) ?: throw NoEncontrado("No existe esa urgencia.")

    companion object {
        const val UNIDADES_MAXIMAS = 20
    }
}

/**
 * Notificación por oleadas: primero los más cercanos; si no se cubre,
 * se amplía el radio. Lo ejecuta el consumidor de `solicitud.creada` y `solicitud.oleada`.
 */
class MatchingService(
    private val solicitudes: SolicitudRepository,
    private val donadores: DirectorioDonadores,
    private val bus: BusDeEventos,
    private val esperaEntreOleadas: Duration = 15.minutes,
    private val radiosKm: List<Double> = listOf(10.0, 25.0, 50.0),
    private val porOleada: Int = 20,
    private val reloj: Clock = Clock.systemUTC()
) {
    suspend fun lanzarOleada(solicitudId: String, oleada: Int) {
        val solicitud = solicitudes.buscarPorId(solicitudId) ?: return
        // Si ya se cubrió, se cerró o se canceló, las oleadas pendientes no hacen nada.
        if (solicitud.estado != EstadoSolicitud.ACTIVA || oleada <= solicitud.oleada || oleada > radiosKm.size) return
        val radio = radiosKm[oleada - 1]
        val candidatos = donadores.candidatos(
            centro = solicitud.ubicacion,
            radioKm = radio,
            tipos = TipoSangre.donadoresCompatiblesCon(solicitud.tipoSangre),
            excluir = solicitud.notificados.toSet(),
            limite = porOleada
        )
        solicitudes.guardar(
            solicitud.copy(
                oleada = oleada,
                radioKm = radio,
                notificados = solicitud.notificados + candidatos.map { it.donador.id },
                actualizadaEn = reloj.instant().toString()
            )
        )
        candidatos.forEach { c ->
            bus.publicar(
                TiposEvento.NOTIFICACION_ENVIAR,
                buildJsonObject {
                    putJsonArray("usuarioIds") { add(c.donador.id) }
                    putJsonObject("datos") {
                        put("tipo", "urgencia")
                        put("solicitudId", solicitud.id)
                        put("tipoSangre", solicitud.tipoSangre.etiqueta)
                        put("hospital", solicitud.hospitalNombre)
                        put("distanciaKm", JsonPrimitive((Math.round(c.distanciaKm * 10) / 10.0).toString()))
                    }
                }
            )
        }
        if (oleada < radiosKm.size) {
            bus.publicarConRetraso(
                TiposEvento.SOLICITUD_OLEADA,
                buildJsonObject {
                    put("solicitudId", solicitud.id)
                    put("oleada", oleada + 1)
                },
                esperaEntreOleadas
            )
        }
    }
}

/** Recalcula las unidades comprometidas y avisa al hospital (consumidor de `respuesta.registrada`). */
class AvanceService(
    private val solicitudes: SolicitudRepository,
    private val respuestas: RespuestaRepository,
    private val bus: BusDeEventos,
    private val reloj: Clock = Clock.systemUTC()
) {
    suspend fun actualizar(solicitudId: String, acepta: Boolean) {
        val solicitud = solicitudes.buscarPorId(solicitudId) ?: return
        // Se cuenta desde cero: así repetir el evento no suma de más (idempotente).
        val comprometidas = respuestas.deSolicitud(solicitudId).count { it.acepta }
        val ahora = reloj.instant().toString()
        val cubierta = solicitud.estado == EstadoSolicitud.ACTIVA && comprometidas >= solicitud.unidades
        solicitudes.guardar(
            solicitud.copy(
                unidadesComprometidas = comprometidas,
                estado = if (cubierta) EstadoSolicitud.CUBIERTA else solicitud.estado,
                cubiertaEn = if (cubierta) ahora else solicitud.cubiertaEn,
                actualizadaEn = ahora
            )
        )
        if (acepta) {
            bus.publicar(
                TiposEvento.NOTIFICACION_ENVIAR,
                buildJsonObject {
                    put("hospitalId", solicitud.hospitalId)
                    putJsonObject("datos") {
                        put("tipo", if (cubierta) "urgencia_cubierta" else "respuesta")
                        put("solicitudId", solicitud.id)
                        put("tipoSangre", solicitud.tipoSangre.etiqueta)
                        put("comprometidas", comprometidas.toString())
                        put("unidades", solicitud.unidades.toString())
                    }
                }
            )
        }
    }
}
