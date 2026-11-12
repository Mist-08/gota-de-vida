package mx.gotadevida.soporte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import mx.gotadevida.JsonApi
import mx.gotadevida.autenticacion.entity.LoginSolicitud
import mx.gotadevida.autenticacion.entity.RegistroSolicitud
import mx.gotadevida.autenticacion.entity.SesionRespuesta
import mx.gotadevida.autenticacion.service.CuentaGoogle
import mx.gotadevida.autenticacion.service.VerificadorGoogle
import mx.gotadevida.core.di.Dependencias
import mx.gotadevida.core.di.Repositorios
import mx.gotadevida.core.mensajeria.BusEnMemoria
import mx.gotadevida.core.mensajeria.EventosProcesadosEnMemoria
import mx.gotadevida.core.notificaciones.EnviadorPushRegistro
import mx.gotadevida.core.seguridad.JwtServicio
import mx.gotadevida.module

const val SECRETO_PRUEBAS = "secreto-de-pruebas-con-mas-de-32-caracteres"
const val ADMIN_EMAIL = "admin@gotadevida.mx"
const val ADMIN_PASSWORD = "admin-segura-123"

/** Acepta el token "token-valido" como la cuenta de Google de Laura. */
class GoogleDePrueba : VerificadorGoogle {
    override suspend fun verificar(idToken: String) =
        if (idToken == "token-valido") CuentaGoogle("google-123", "laura@gmail.com", "Laura Pérez") else null
}

class EntornoDePrueba {
    val usuarios = UsuariosEnMemoria()
    val donadores = DonadoresEnMemoria()
    val hospitales = HospitalesEnMemoria()
    val reglas = ReglasEnMemoria()
    val solicitudes = SolicitudesEnMemoria()
    val respuestas = RespuestasEnMemoria()
    val citas = CitasEnMemoria()
    val donaciones = DonacionesEnMemoria()

    /** Bus síncrono: cada evento se procesa antes de que termine la petición. */
    val bus = BusEnMemoria(EventosProcesadosEnMemoria(), sincrono = true)
    val push = EnviadorPushRegistro()
    val dependencias: Dependencias = Dependencias.crear(
        jwt = JwtServicio(SECRETO_PRUEBAS, "gota-de-vida-pruebas"),
        repos = Repositorios(usuarios, donadores, hospitales, reglas, solicitudes, respuestas, citas, donaciones),
        hasher = HasherDePrueba(),
        verificadorGoogle = GoogleDePrueba(),
        bus = bus,
        push = push,
    ) { deps ->
        deps.reglas.asegurarReglas()
        deps.autenticacion.asegurarAdministrador(ADMIN_EMAIL, ADMIN_PASSWORD)
    }
}

/** Levanta la API completa con repositorios en memoria y un cliente que habla JSON. */
fun probarApi(bloque: suspend ApplicationTestBuilder.(cliente: HttpClient, entorno: EntornoDePrueba) -> Unit) =
    testApplication {
        val entorno = EntornoDePrueba()
        entorno.dependencias.preparar()
        application { module(entorno.dependencias) }
        val cliente = createClient {
            install(ContentNegotiation) { json(JsonApi) }
        }
        bloque(cliente, entorno)
    }

suspend fun HttpClient.registrar(
    email: String = "mariana@correo.mx",
    nombre: String = "Mariana Rodríguez",
): SesionRespuesta = post("/api/v1/auth/registro") {
    contentType(ContentType.Application.Json)
    setBody(
        RegistroSolicitud(
            nombre = nombre,
            email = email,
            password = "contrasena-segura",
            telefono = "871 123 4567",
            aceptaAvisoPrivacidad = true,
        ),
    )
}.body()

suspend fun HttpClient.iniciarComoAdmin(): String = post("/api/v1/auth/login") {
    contentType(ContentType.Application.Json)
    setBody(LoginSolicitud(ADMIN_EMAIL, ADMIN_PASSWORD))
}.body<SesionRespuesta>().tokens.tokenAcceso

fun io.ktor.client.request.HttpRequestBuilder.conToken(token: String) {
    bearerAuth(token)
}
