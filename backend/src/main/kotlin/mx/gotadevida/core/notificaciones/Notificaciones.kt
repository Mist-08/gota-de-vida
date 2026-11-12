package mx.gotadevida.core.notificaciones

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mx.gotadevida.core.dominio.DirectorioUsuarios
import mx.gotadevida.core.mensajeria.Consumidor
import mx.gotadevida.core.mensajeria.TiposEvento
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

/**
 * Envía notificaciones push. Se mandan mensajes "de datos": la app arma el texto
 * con sus `strings.xml`, así la notificación sale en el idioma del teléfono.
 */
interface EnviadorPush {
    /** Devuelve los tokens que FCM reportó como inválidos. */
    suspend fun enviar(tokens: List<String>, datos: Map<String, String>): List<String>
}

/** Sin Firebase configurado (desarrollo, CI): solo deja constancia en el registro. */
class EnviadorPushRegistro : EnviadorPush {
    private val log = LoggerFactory.getLogger(EnviadorPushRegistro::class.java)
    val enviados = mutableListOf<Pair<List<String>, Map<String, String>>>()

    override suspend fun enviar(tokens: List<String>, datos: Map<String, String>): List<String> {
        enviados += tokens to datos
        log.info("Push simulado a ${tokens.size} dispositivo(s): $datos")
        return emptyList()
    }
}

/**
 * Firebase Cloud Messaging HTTP v1 (plan Spark, gratis).
 * `credencialesJson` es la cuenta de servicio de Firebase (variable FIREBASE_CREDENCIALES_JSON,
 * el JSON completo o en base64). Con ella se firma un JWT y se cambia por un token OAuth.
 */
class EnviadorPushFcm(credencialesJson: String) : EnviadorPush {
    private val log = LoggerFactory.getLogger(EnviadorPushFcm::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val cliente = HttpClient(CIO)
    private val credenciales: JsonObject = json.parseToJsonElement(
        credencialesJson.trim().let { if (it.startsWith("{")) it else String(Base64.getDecoder().decode(it)) },
    ).jsonObject
    private val proyecto = credenciales.getValue("project_id").jsonPrimitive.content
    private val correo = credenciales.getValue("client_email").jsonPrimitive.content
    private val llave: RSAPrivateKey = credenciales.getValue("private_key").jsonPrimitive.content
        .replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
        .let { KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(it))) as RSAPrivateKey }
    private var tokenAcceso: String? = null
    private var venceEn: Instant = Instant.EPOCH

    private suspend fun token(): String {
        val ahora = Instant.now()
        tokenAcceso?.takeIf { ahora.isBefore(venceEn.minusSeconds(60)) }?.let { return it }
        val afirmacion = JWT.create()
            .withIssuer(correo)
            .withAudience(URL_TOKEN)
            .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
            .withIssuedAt(ahora)
            .withExpiresAt(ahora.plusSeconds(3600))
            .sign(Algorithm.RSA256(null, llave))
        val respuesta = cliente.submitForm(
            URL_TOKEN,
            parameters {
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("assertion", afirmacion)
            },
        )
        val cuerpo = json.parseToJsonElement(respuesta.bodyAsText()).jsonObject
        val nuevo = cuerpo["access_token"]?.jsonPrimitive?.content ?: error("Firebase no dio token: $cuerpo")
        tokenAcceso = nuevo
        venceEn = ahora.plusSeconds(cuerpo["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600)
        return nuevo
    }

    override suspend fun enviar(tokens: List<String>, datos: Map<String, String>): List<String> {
        val invalidos = mutableListOf<String>()
        val acceso = token()
        tokens.forEach { destino ->
            val mensaje = buildJsonObject {
                putJsonObject("message") {
                    put("token", destino)
                    putJsonObject("data") { datos.forEach { (k, v) -> put(k, v) } }
                    putJsonObject("android") { put("priority", "high") }
                }
            }
            val r = cliente.post("https://fcm.googleapis.com/v1/projects/$proyecto/messages:send") {
                bearerAuth(acceso)
                contentType(ContentType.Application.Json)
                setBody(mensaje.toString())
            }
            when {
                r.status == HttpStatusCode.NotFound -> invalidos += destino
                r.status.value >= 400 -> log.warn("FCM respondió ${r.status}: ${r.bodyAsText()}")
            }
        }
        return invalidos
    }

    companion object {
        private const val URL_TOKEN = "https://oauth2.googleapis.com/token"
    }
}

/**
 * Consumidor de `notificacion.enviar`: busca los tokens de los destinatarios y manda el push.
 * Datos del evento: `usuarioIds` (lista) o `hospitalId`, y `datos` (lo que recibe la app).
 */
fun consumidorNotificaciones(
    usuarios: DirectorioUsuarios,
    push: EnviadorPush,
    alEnviar: suspend (usuarioIds: List<String>, datos: Map<String, String>) -> Unit = { _, _ -> },
) = Consumidor("notificador", listOf(TiposEvento.NOTIFICACION_ENVIAR)) { evento ->
    val d = evento.datos
    val destinatarios = buildList {
        d["usuarioIds"]?.jsonArray?.forEach { add(it.jsonPrimitive.content) }
        d["hospitalId"]?.jsonPrimitive?.content?.let { addAll(usuarios.usuariosDeHospital(it)) }
    }.distinct()
    val datos = d["datos"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
    val tokens = usuarios.tokensFcm(destinatarios)
    if (tokens.isNotEmpty()) {
        push.enviar(tokens, datos).forEach { usuarios.quitarTokenFcm(it) }
    }
    alEnviar(destinatarios, datos)
}
