package mx.gotadevida.core.config

import java.io.File

/**
 * Configuración leída de variables de entorno. En local también se lee el archivo `.env`
 * de la carpeta `backend/` (las variables del sistema tienen prioridad).
 */
data class Configuracion(
    val puerto: Int,
    val mongoUri: String,
    val mongoBaseDatos: String,
    val jwtSecreto: String,
    val jwtEmisor: String,
    val googleClientId: String?,
    val adminEmail: String?,
    val adminPassword: String?,
    val cargarDatosDemo: Boolean,
    val rabbitMqUrl: String? = null,
    val firebaseCredenciales: String? = null,
    val minutosEntreOleadas: Long = 15,
    val demoPassword: String? = null,
    val geminiLlave: String? = null,
    val geminiModelo: String = "gemini-3.5-flash-lite",
    val whatsappToken: String? = null,
    val whatsappIdNumero: String? = null,
    val whatsappTokenVerificacion: String? = null,
    val whatsappSecretoApp: String? = null
) {
    companion object {
        fun cargar(): Configuracion {
            val archivo = leerArchivoEnv(File(".env"))
            return desde { nombre -> System.getenv(nombre) ?: archivo[nombre] }
        }

        fun desde(variable: (String) -> String?): Configuracion {
            fun texto(nombre: String): String? = variable(nombre)?.trim()?.takeIf { it.isNotEmpty() }
            val secreto = texto("JWT_SECRET")
                ?: error("Falta la variable JWT_SECRET. Copia backend/.env.example como backend/.env y llénala.")
            require(secreto.length >= 32) { "JWT_SECRET debe tener al menos 32 caracteres." }
            return Configuracion(
                puerto = texto("PORT")?.toIntOrNull() ?: 8080,
                mongoUri = texto("MONGODB_URI") ?: "mongodb://localhost:27017",
                mongoBaseDatos = texto("MONGODB_DB") ?: "gotadevida",
                jwtSecreto = secreto,
                jwtEmisor = texto("JWT_ISSUER") ?: "gota-de-vida",
                googleClientId = texto("GOOGLE_CLIENT_ID"),
                adminEmail = texto("ADMIN_EMAIL"),
                adminPassword = texto("ADMIN_PASSWORD"),
                cargarDatosDemo = texto("CARGAR_DATOS_DEMO")?.lowercase() != "false",
                rabbitMqUrl = texto("RABBITMQ_URL"),
                firebaseCredenciales = texto("FIREBASE_CREDENCIALES_JSON"),
                minutosEntreOleadas = texto("MINUTOS_ENTRE_OLEADAS")?.toLongOrNull() ?: 15,
                demoPassword = texto("DEMO_PASSWORD"),
                geminiLlave = texto("GEMINI_API_KEY"),
                geminiModelo = texto("GEMINI_MODEL") ?: "gemini-3.5-flash-lite",
                whatsappToken = texto("WHATSAPP_TOKEN"),
                whatsappIdNumero = texto("WHATSAPP_PHONE_NUMBER_ID"),
                whatsappTokenVerificacion = texto("WHATSAPP_VERIFY_TOKEN"),
                whatsappSecretoApp = texto("WHATSAPP_APP_SECRET")
            )
        }

        /** Lee líneas `CLAVE=valor`, ignorando vacías y comentarios. */
        fun leerArchivoEnv(archivo: File): Map<String, String> {
            if (!archivo.isFile) return emptyMap()
            return archivo.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                .associate { linea ->
                    val clave = linea.substringBefore('=').trim()
                    val valor = linea.substringAfter('=').trim().removeSurrounding("\"")
                    clave to valor
                }
        }
    }
}
