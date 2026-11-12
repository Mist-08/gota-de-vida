package mx.gotadevida

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mx.gotadevida.autenticacion.routes.autenticacionRoutes
import mx.gotadevida.core.config.Configuracion
import mx.gotadevida.core.di.Dependencias
import mx.gotadevida.core.errores.configurarErrores
import mx.gotadevida.core.salud.saludRoutes
import mx.gotadevida.core.seguridad.configurarSeguridad
import mx.gotadevida.donador.routes.donadorRoutes
import mx.gotadevida.hospital.routes.hospitalRoutes
import mx.gotadevida.reglas.routes.reglasRoutes
import mx.gotadevida.solicitud.routes.solicitudRoutes
import io.ktor.server.application.ApplicationStopped

/**
 * Arranca el servidor. Render asigna el puerto en la variable PORT; en local se usa 8080.
 */
fun main() {
    val config = Configuracion.cargar()
    val dependencias = Dependencias.produccion(config)
    runBlocking { dependencias.preparar() }
    embeddedServer(Netty, port = config.puerto, host = "0.0.0.0") {
        module(dependencias)
    }.start(wait = true)
}

/** JSON de la API: ignora campos desconocidos y no manda los nulos. */
val JsonApi = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Configura la API: JSON, errores, seguridad, rutas y Swagger.
 * Cada funcionalidad nueva agrega aquí sus rutas.
 */
fun Application.module(dependencias: Dependencias) {
    install(ContentNegotiation) {
        json(JsonApi)
    }
    install(CallLogging)
    configurarErrores()
    configurarSeguridad(dependencias.jwt)

    routing {
        saludRoutes()
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        autenticacionRoutes(dependencias.autenticacion, existeHospital = dependencias.hospitales::existe)
        donadorRoutes(dependencias.donadores)
        hospitalRoutes(dependencias.hospitales)
        reglasRoutes(dependencias.reglas)
        solicitudRoutes(dependencias.solicitudes)
    }
    monitor.subscribe(ApplicationStopped) { dependencias.bus.cerrar() }
}
