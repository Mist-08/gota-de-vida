package mx.gotadevida.chatbot.service

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import mx.gotadevida.chatbot.entity.Autor
import mx.gotadevida.chatbot.entity.ContextoChat
import mx.gotadevida.chatbot.entity.Conversacion
import mx.gotadevida.chatbot.entity.MensajeChat
import mx.gotadevida.chatbot.entity.PreguntaFrecuente
import mx.gotadevida.chatbot.entity.RespuestaChat
import mx.gotadevida.chatbot.repository.ConversacionRepository
import mx.gotadevida.chatbot.repository.PreguntasFrecuentesRepository
import mx.gotadevida.core.dominio.DirectorioCitas
import mx.gotadevida.core.dominio.DirectorioDonadores
import mx.gotadevida.core.dominio.DirectorioUrgencias
import mx.gotadevida.core.dominio.RequisitosVigentes
import mx.gotadevida.core.errores.ErrorDeNegocio
import org.slf4j.LoggerFactory
import java.text.Normalizer
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Gota", la asistente (sección 12). El mismo cerebro atiende la app y WhatsApp.
 *
 * Orden para ahorrar cuota del nivel gratuito de Gemini:
 * 1. Botones y opciones del menú ("1", "Sí puedo", "09:30") se resuelven sin IA.
 * 2. Preguntas frecuentes (ayuno, requisitos…) se contestan desde la base.
 * 3. Lo demás va a Gemini con function calling.
 * 4. Si Gemini no está configurado o se acabó la cuota (429), se usa el menú sin IA.
 *
 * Privacidad: a Gemini solo le llega el texto y resultados anonimizados
 * (ids internos y distancias, nunca nombres, teléfonos ni ubicaciones).
 */
class AsistenteService(
    private val conversaciones: ConversacionRepository,
    private val preguntas: PreguntasFrecuentesRepository,
    private val gemini: ClienteGemini?,
    private val donadores: DirectorioDonadores,
    private val urgencias: DirectorioUrgencias,
    private val citas: DirectorioCitas,
    private val requisitos: suspend () -> RequisitosVigentes,
    private val reloj: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(AsistenteService::class.java)
    private fun hoy(): LocalDate = LocalDate.now(reloj.withZone(ZONA))

    /** Carga las preguntas frecuentes iniciales que falten (se puede llamar en cada arranque). */
    suspend fun asegurarPreguntasFrecuentes() {
        val existentes = preguntas.todas().map { it.id }.toSet()
        PREGUNTAS_INICIALES.filter { it.id !in existentes }.forEach { preguntas.guardar(it) }
    }

    suspend fun historial(donadorId: String): List<MensajeChat> = conversaciones.buscar(donadorId)?.mensajes.orEmpty()

    /** Lo usa el aviso de WhatsApp: así "Sí puedo" sabe de qué urgencia se habla. */
    suspend fun recordarUrgencia(donadorId: String, solicitudId: String, hospitalId: String?) {
        val c = cargar(donadorId)
        conversaciones.guardar(c.copy(contexto = ContextoChat(solicitudId, hospitalId, hoy().plusDays(1).toString())))
    }

    suspend fun responder(donadorId: String, texto: String): RespuestaChat {
        val limpio = texto.trim().take(500)
        var conversacion = cargar(donadorId)
        val ahora = reloj.instant().toString()
        conversacion = conversacion.copy(mensajes = conversacion.mensajes + MensajeChat(Autor.DONADOR, limpio, ahora))

        val (respuesta, contexto) = opcionDelMenu(donadorId, limpio, conversacion.contexto, estricto = gemini != null)
            ?: preguntaFrecuente(limpio)?.let { it to conversacion.contexto }
            ?: conGemini(donadorId, conversacion)
            ?: (menu(sinIa = true) to conversacion.contexto)

        conversaciones.guardar(
            conversacion.copy(
                mensajes = (conversacion.mensajes + MensajeChat(Autor.GOTA, respuesta.respuesta, reloj.instant().toString())).takeLast(MAXIMO_MENSAJES),
                contexto = contexto,
                actualizadaEn = ahora,
            ),
        )
        return respuesta
    }

    private suspend fun cargar(donadorId: String) =
        conversaciones.buscar(donadorId) ?: Conversacion(donadorId, emptyList(), ContextoChat(), reloj.instant().toString())

    // ---------- 1. Menú y botones (sin IA) ----------

    private suspend fun opcionDelMenu(donadorId: String, texto: String, ctx: ContextoChat, estricto: Boolean): Pair<RespuestaChat, ContextoChat>? {
        val t = normalizar(texto)
        val hora = Regex("^(\\d{1,2}):(\\d{2})$").find(t)?.let { "%02d:%s".format(it.groupValues[1].toInt(), it.groupValues[2]) }
        fun dice(vararg palabras: String) = if (estricto) palabras.any { t == it } else palabras.any { t.contains(it) }
        return when {
            hora != null && ctx.hospitalId != null -> agendarDesdeMenu(donadorId, ctx, hora)
            t == "1" || t == "si puedo" || dice("acepto", "aceptar urgencia") -> aceptarDesdeMenu(donadorId, ctx)
            t == "no puedo" -> rechazarDesdeMenu(donadorId, ctx)
            t == "2" || dice("requisitos") -> RespuestaChat(textoRequisitos(), MENU, ORIGEN_MENU) to ctx
            t == "3" || dice("agendar", "agendar cita") -> ofrecerHorarios(donadorId, ctx)
            t == "4" || dice("puedo donar", "elegibilidad") -> RespuestaChat(textoElegibilidad(donadorId), MENU, ORIGEN_MENU) to ctx
            t == "menu" || t == "hola" && !estricto -> menu(sinIa = false) to ctx
            else -> null
        }
    }

    private suspend fun aceptarDesdeMenu(donadorId: String, ctx: ContextoChat): Pair<RespuestaChat, ContextoChat> {
        val urgencia = ctx.solicitudId?.let { id -> urgencias.paraDonador(donadorId).firstOrNull { it.id == id } }
            ?: urgencias.paraDonador(donadorId).firstOrNull { it.miRespuesta != true }
            ?: return RespuestaChat("Por ahora no hay urgencias activas cerca de ti que puedas cubrir. Te aviso en cuanto haya una. 🩸", MENU, ORIGEN_MENU) to ctx
        return try {
            urgencias.responderUrgencia(donadorId, urgencia.id, true)
            val nuevo = ContextoChat(urgencia.id, urgencia.hospitalId, hoy().plusDays(1).toString())
            val horas = citas.horasLibres(urgencia.hospitalId, nuevo.fecha!!).take(3)
            val texto = if (horas.isEmpty()) {
                "¡Gracias! ${urgencia.hospitalNombre} ya sabe que vas. Agenda tu cita desde la app."
            } else {
                "¡Gracias! Hay lugar mañana en ${urgencia.hospitalNombre} a las ${horas.joinToString(", ")}. ¿Cuál prefieres?"
            }
            RespuestaChat(texto, horas, ORIGEN_MENU) to nuevo
        } catch (e: ErrorDeNegocio) {
            RespuestaChat(e.message.orEmpty(), MENU, ORIGEN_MENU) to ctx
        }
    }

    private suspend fun rechazarDesdeMenu(donadorId: String, ctx: ContextoChat): Pair<RespuestaChat, ContextoChat> {
        ctx.solicitudId?.let { runCatching { urgencias.responderUrgencia(donadorId, it, false) } }
        return RespuestaChat("Gracias por avisar. Te escribiré cuando haya otra urgencia compatible. 💙", emptyList(), ORIGEN_MENU) to ContextoChat()
    }

    private suspend fun ofrecerHorarios(donadorId: String, ctx: ContextoChat): Pair<RespuestaChat, ContextoChat> {
        citas.proximas(donadorId).firstOrNull()?.let {
            return RespuestaChat("Ya tienes cita el ${it.fechaHora.replace('T', ' ')} en ${it.hospitalNombre}. Tu QR está en la app, en \"Mi cita\".", emptyList(), ORIGEN_MENU) to ctx
        }
        val hospitalId = ctx.hospitalId
            ?: return RespuestaChat("Primero acepta una urgencia (opción 1) y te ofrezco horarios en ese hospital.", MENU, ORIGEN_MENU) to ctx
        val fecha = ctx.fecha ?: hoy().plusDays(1).toString()
        val horas = citas.horasLibres(hospitalId, fecha).take(3)
        val texto = if (horas.isEmpty()) "Ese día ya no hay lugares. Agenda otro día desde la app." else "Hay lugar el $fecha a las ${horas.joinToString(", ")}. ¿Cuál prefieres?"
        return RespuestaChat(texto, horas, ORIGEN_MENU) to ctx.copy(fecha = fecha)
    }

    private suspend fun agendarDesdeMenu(donadorId: String, ctx: ContextoChat, hora: String): Pair<RespuestaChat, ContextoChat> {
        val fecha = ctx.fecha ?: hoy().plusDays(1).toString()
        return try {
            val cita = citas.agendar(donadorId, ctx.hospitalId!!, "${fecha}T$hora", ctx.solicitudId)
            val r = requisitos()
            RespuestaChat(
                "✅ Listo, cita confirmada el $fecha a las $hora en ${cita.hospitalNombre}. Tu código QR está en la app (\"Mi cita\"). " +
                    "Llega con ${r.horasAyunoMinimo} a ${r.horasAyunoMaximo} horas de ayuno y lleva tu identificación.",
                emptyList(),
                ORIGEN_MENU,
            ) to ContextoChat()
        } catch (e: ErrorDeNegocio) {
            RespuestaChat(e.message.orEmpty(), emptyList(), ORIGEN_MENU) to ctx
        }
    }

    private fun menu(sinIa: Boolean) = RespuestaChat(
        (if (sinIa) "Ahora mismo te atiendo con opciones rápidas:\n" else "Soy Gota 🩸. ¿En qué te ayudo?\n") +
            "1. Aceptar urgencia\n2. Ver requisitos\n3. Agendar cita\n4. ¿Puedo donar?",
        MENU,
        ORIGEN_MENU,
    )

    // ---------- 2. Preguntas frecuentes ----------

    private suspend fun preguntaFrecuente(texto: String): RespuestaChat? {
        val t = normalizar(texto)
        val pregunta = preguntas.todas().firstOrNull { p -> p.palabrasClave.any { t.contains(normalizar(it)) } } ?: return null
        return RespuestaChat(rellenar(pregunta.respuesta, requisitos()), emptyList(), ORIGEN_FAQ)
    }

    // ---------- 3. Gemini con function calling ----------

    private suspend fun conGemini(donadorId: String, conversacion: Conversacion): Pair<RespuestaChat, ContextoChat>? {
        val cliente = gemini ?: return null
        var ctx = conversacion.contexto
        val turnos = conversacion.mensajes.takeLast(10).map { m ->
            TurnoGemini(if (m.autor == Autor.DONADOR) "user" else "model", buildJsonArray { addJsonObject { put("text", m.texto) } })
        }.toMutableList()
        return try {
            repeat(MAXIMO_LLAMADAS) {
                when (val r = cliente.generar(sistema(), turnos, HERRAMIENTAS)) {
                    is RespuestaGemini.Texto -> return RespuestaChat(r.texto.ifBlank { "¿Me lo repites, por favor?" }, emptyList(), ORIGEN_GEMINI) to ctx
                    is RespuestaGemini.Funciones -> {
                        turnos += TurnoGemini("model", r.parteDelModelo)
                        val resultados = r.llamadas.map { (nombre, args) ->
                            val (resultado, nuevoCtx) = ejecutar(donadorId, nombre, args, ctx)
                            ctx = nuevoCtx
                            buildJsonObject { putJsonObject("functionResponse") { put("name", nombre); put("response", resultado) } }
                        }
                        turnos += TurnoGemini("user", JsonArray(resultados))
                    }
                }
            }
            null
        } catch (e: CuotaAgotada) {
            log.warn("Sin cuota de Gemini: se usa el menú sin IA")
            null
        } catch (e: Exception) {
            log.warn("Gemini falló; se usa el menú sin IA", e)
            null
        }
    }

    /** Ejecuta una herramienta. Cada una llama a un service existente; el modelo no toca la base. */
    private suspend fun ejecutar(donadorId: String, nombre: String, args: JsonObject, ctx: ContextoChat): Pair<JsonObject, ContextoChat> = try {
        fun texto(clave: String) = args[clave]?.jsonPrimitive?.content
        when (nombre) {
            "consultar_elegibilidad" -> {
                val d = donadores.resumen(donadorId)
                buildJsonObject {
                    put("puedeDonar", d?.elegible ?: false)
                    d?.proximaFechaElegible?.let { put("proximaFechaElegible", it) }
                    put("nota", "La decisión final es del personal médico del banco de sangre.")
                } to ctx
            }
            "buscar_urgencias_activas" -> {
                val lista = urgencias.paraDonador(donadorId).take(5)
                buildJsonObject {
                putJsonArray("urgencias") {
                    lista.forEach { u ->
                        addJsonObject {
                            put("solicitudId", u.id)
                            put("hospitalId", u.hospitalId)
                            put("hospital", u.hospitalNombre)
                            put("tipoSangreNecesario", u.etiquetaTipoSangre)
                            put("unidadesFaltantes", (u.unidades - u.unidadesComprometidas).coerceAtLeast(0))
                            u.distanciaKm?.let { put("distanciaKm", Math.round(it * 10) / 10.0) }
                            u.miRespuesta?.let { put("yaRespondio", it) }
                        }
                    }
                }
                } to ctx
            }
            "responder_urgencia" -> {
                val id = texto("solicitudId").orEmpty()
                val acepta = args["acepta"]?.jsonPrimitive?.booleanOrNull ?: true
                val u = urgencias.responderUrgencia(donadorId, id, acepta)
                buildJsonObject {
                    put("ok", true)
                    put("unidadesComprometidas", u.unidadesComprometidas)
                    put("unidades", u.unidades)
                } to if (acepta) ContextoChat(id, u.hospitalId, hoy().plusDays(1).toString()) else ContextoChat()
            }
            "horarios_disponibles" -> {
                val hospitalId = texto("hospitalId") ?: ctx.hospitalId.orEmpty()
                val fecha = texto("fecha") ?: hoy().plusDays(1).toString()
                val horas = citas.horasLibres(hospitalId, fecha)
                buildJsonObject {
                    put("fecha", fecha)
                    putJsonArray("horas") { horas.forEach { add(JsonPrimitive(it)) } }
                } to ctx.copy(hospitalId = hospitalId, fecha = fecha)
            }
            "agendar_cita" -> {
                val solicitudId = texto("solicitudId") ?: ctx.solicitudId
                val hospitalId = texto("hospitalId")
                    ?: urgencias.paraDonador(donadorId).firstOrNull { it.id == solicitudId }?.hospitalId
                    ?: ctx.hospitalId
                    ?: return buildJsonObject { put("error", "Falta saber en qué hospital. Pregunta por la urgencia primero.") } to ctx
                val cita = citas.agendar(donadorId, hospitalId, texto("fechaHora").orEmpty(), solicitudId)
                buildJsonObject {
                    put("ok", true)
                    put("citaId", cita.id)
                    put("fechaHora", cita.fechaHora)
                    put("hospital", cita.hospitalNombre)
                    put("nota", "El código QR está en la app, en Mi cita.")
                } to ContextoChat()
            }
            "mis_citas" -> {
                val proximas = citas.proximas(donadorId)
                buildJsonObject {
                    putJsonArray("citas") {
                        proximas.forEach { c ->
                            addJsonObject { put("citaId", c.id); put("fechaHora", c.fechaHora); put("hospital", c.hospitalNombre) }
                        }
                    }
                } to ctx
            }
            "cancelar_cita" -> {
                citas.cancelarCita(donadorId, texto("citaId").orEmpty())
                buildJsonObject { put("ok", true) } to ctx
            }
            "requisitos_donacion" -> {
                val r = requisitos()
                buildJsonObject {
                    put("edad", "${r.edadMinima} a ${r.edadMaxima} años")
                    put("pesoMinimoKg", r.pesoMinimoKg)
                    put("diasEntreDonaciones", r.diasEntreDonaciones)
                    put("ayuno", "${r.horasAyunoMinimo} a ${r.horasAyunoMaximo} horas")
                    put("tatuajesOPerforaciones", "esperar ${r.mesesEsperaTatuaje} meses")
                    putJsonArray("otros") { r.requisitos.forEach { add(JsonPrimitive(it)) } }
                } to ctx
            }
            else -> buildJsonObject { put("error", "Herramienta desconocida.") } to ctx
        }
    } catch (e: ErrorDeNegocio) {
        buildJsonObject { put("error", e.message.orEmpty()) } to ctx
    }

    private fun sistema() = INSTRUCCIONES + "\nHoy es ${hoy()} (zona America/Monterrey). Las fechas y horas de cita van en formato AAAA-MM-DDTHH:MM."

    // ---------- Textos sin IA ----------

    private suspend fun textoRequisitos(): String {
        val r = requisitos()
        return "Para donar: tener de ${r.edadMinima} a ${r.edadMaxima} años, pesar al menos ${r.pesoMinimoKg.toInt()} kg, " +
            "llegar con ${r.horasAyunoMinimo} a ${r.horasAyunoMaximo} horas de ayuno y llevar tu identificación oficial. " +
            "La decisión final la toma el personal médico del banco de sangre."
    }

    private suspend fun textoElegibilidad(donadorId: String): String {
        val d = donadores.resumen(donadorId) ?: return "No encontré tu perfil. Complétalo en la app."
        return when {
            d.elegible -> "Según tu perfil, ya puedes donar. 🙌 La decisión final es del personal médico."
            d.proximaFechaElegible != null -> "Podrás volver a donar a partir del ${d.proximaFechaElegible}."
            else -> "Te faltan datos en tu perfil. Complétalo en la app para saber si puedes donar."
        }
    }

    companion object {
        val ZONA: ZoneId = ZoneId.of("America/Monterrey")
        const val MAXIMO_MENSAJES = 30
        const val MAXIMO_LLAMADAS = 5
        const val ORIGEN_MENU = "MENU"
        const val ORIGEN_FAQ = "PREGUNTA_FRECUENTE"
        const val ORIGEN_GEMINI = "GEMINI"
        val MENU = listOf("1", "2", "3", "4")

        /** Instrucciones del sistema (sección 12 del prompt maestro). */
        val INSTRUCCIONES = """
            Eres "Gota", la asistente de Gota de Vida en la Comarca Lagunera.
            Hablas en el idioma del usuario (español por defecto), con un tono cálido, breve y claro.
            En WhatsApp usa mensajes cortos (máximo 3 líneas) y emojis con moderación.
            Tu objetivo: ayudar al donador a responder urgencias, resolver dudas sobre requisitos y agendar su cita.
            Reglas:
            - Responde sobre requisitos SOLO con los datos de la herramienta requisitos_donacion. Si no está ahí, di que lo confirme con el banco de sangre.
            - Nunca diagnostiques ni asegures que alguien puede donar: la decisión final es del personal médico del banco de sangre.
            - Antes de ejecutar una acción (aceptar urgencia, agendar o cancelar cita), confirma con el usuario.
            - Si el usuario menciona síntomas graves o una emergencia, indícale llamar al 911.
            - No hables de temas ajenos a la donación de sangre; redirige con amabilidad.
            - Nunca reveles datos de otros usuarios.
        """.trimIndent()

        private fun funcion(nombre: String, descripcion: String, propiedades: Map<String, Pair<String, String>> = emptyMap(), requeridos: List<String> = emptyList()) =
            buildJsonObject {
                put("name", nombre)
                put("description", descripcion)
                if (propiedades.isNotEmpty()) {
                    putJsonObject("parameters") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            propiedades.forEach { (k, v) -> putJsonObject(k) { put("type", v.first); put("description", v.second) } }
                        }
                        putJsonArray("required") { requeridos.forEach { add(JsonPrimitive(it)) } }
                    }
                }
            }

        val HERRAMIENTAS: JsonArray = JsonArray(
            listOf<JsonElement>(
                funcion("consultar_elegibilidad", "Dice si el donador puede donar hoy según su perfil y desde cuándo podrá."),
                funcion("buscar_urgencias_activas", "Urgencias activas cercanas que el donador puede cubrir, con distancia aproximada."),
                funcion(
                    "responder_urgencia", "Acepta o rechaza una urgencia. Confirma con el usuario antes de llamarla.",
                    mapOf("solicitudId" to ("STRING" to "Id de la urgencia"), "acepta" to ("BOOLEAN" to "true si puede donar")),
                    listOf("solicitudId", "acepta"),
                ),
                funcion(
                    "horarios_disponibles", "Horas libres para donar en un hospital un día.",
                    mapOf("hospitalId" to ("STRING" to "Id del hospital"), "fecha" to ("STRING" to "AAAA-MM-DD")),
                    listOf("hospitalId", "fecha"),
                ),
                funcion(
                    "agendar_cita", "Agenda la cita para donar. Confirma con el usuario antes de llamarla.",
                    mapOf("solicitudId" to ("STRING" to "Id de la urgencia aceptada"), "fechaHora" to ("STRING" to "AAAA-MM-DDTHH:MM")),
                    listOf("solicitudId", "fechaHora"),
                ),
                funcion("mis_citas", "Citas próximas del donador con su id."),
                funcion(
                    "cancelar_cita", "Cancela una cita. Confirma con el usuario antes de llamarla.",
                    mapOf("citaId" to ("STRING" to "Id de la cita")),
                    listOf("citaId"),
                ),
                funcion("requisitos_donacion", "Requisitos vigentes para donar (edad, peso, ayuno, tatuajes, qué llevar)."),
            ),
        )

        fun normalizar(texto: String): String =
            Normalizer.normalize(texto.lowercase().trim(), Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")
                .replace("[¿?¡!.,]".toRegex(), "").trim()

        fun rellenar(plantilla: String, r: RequisitosVigentes): String = plantilla
            .replace("{edadMinima}", r.edadMinima.toString())
            .replace("{edadMaxima}", r.edadMaxima.toString())
            .replace("{pesoMinimoKg}", r.pesoMinimoKg.toInt().toString())
            .replace("{diasEntreDonaciones}", r.diasEntreDonaciones.toString())
            .replace("{horasAyunoMinimo}", r.horasAyunoMinimo.toString())
            .replace("{horasAyunoMaximo}", r.horasAyunoMaximo.toString())
            .replace("{mesesEsperaTatuaje}", r.mesesEsperaTatuaje.toString())

        /** Preguntas frecuentes iniciales. Se guardan en `preguntas_frecuentes` y se pueden editar ahí. */
        val PREGUNTAS_INICIALES = listOf(
            PreguntaFrecuente("ayuno", listOf("ayuno", "ayunas", "comer antes", "desayunar"),
                "Llega con {horasAyunoMinimo} a {horasAyunoMaximo} horas de ayuno y evita grasas, huevo y lácteos desde un día antes. Puedes tomar agua. 💧"),
            PreguntaFrecuente("que_llevar", listOf("que llevo", "que necesito llevar", "identificacion", "ine"),
                "Lleva tu identificación oficial vigente con fotografía. En la app, en \"Mi cita\", está tu código QR."),
            PreguntaFrecuente("cada_cuanto", listOf("cada cuanto", "cuantas veces", "cada cuando"),
                "Entre una donación de sangre total y otra deben pasar al menos {diasEntreDonaciones} días. En el inicio de la app ves tu próxima fecha."),
            PreguntaFrecuente("tatuaje", listOf("tatuaje", "perforacion", "piercing", "acupuntura"),
                "Si te hiciste un tatuaje, perforación o acupuntura, espera {mesesEsperaTatuaje} meses antes de donar. Confírmalo con el banco de sangre."),
            PreguntaFrecuente("edad_peso", listOf("edad", "cuanto debo pesar", "peso minimo", "cuantos años"),
                "Para donar debes tener de {edadMinima} a {edadMaxima} años y pesar al menos {pesoMinimoKg} kg. La decisión final es del personal médico."),
            PreguntaFrecuente("pago", listOf("pagan", "cobran", "dinero", "cuanto cuesta"),
                "Donar sangre es voluntario y altruista: no cuesta nada y nadie puede pagarte por hacerlo. 💙"),
        )
    }
}
