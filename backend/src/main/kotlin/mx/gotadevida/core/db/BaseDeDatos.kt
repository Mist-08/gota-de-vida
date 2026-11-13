package mx.gotadevida.core.db

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document

/** Conexión a MongoDB Atlas (o a un MongoDB local en desarrollo y en el CI). */
class BaseDeDatos(uri: String, nombre: String) {
    private val cliente = MongoClient.create(uri)
    val db: MongoDatabase = cliente.getDatabase(nombre)

    /** Crea los índices si no existen. Es seguro llamarlo en cada arranque. */
    suspend fun crearIndices() {
        db.getCollection<Document>(Colecciones.USUARIOS)
            .createIndex(Indexes.ascending("email"), IndexOptions().unique(true))
        db.getCollection<Document>(Colecciones.USUARIOS)
            .createIndex(Indexes.ascending("googleId"))
        db.getCollection<Document>(Colecciones.DONADORES)
            .createIndex(Indexes.geo2dsphere("ubicacion"))
        db.getCollection<Document>(Colecciones.HOSPITALES)
            .createIndex(Indexes.geo2dsphere("ubicacion"))
        db.getCollection<Document>(Colecciones.SOLICITUDES)
            .createIndex(Indexes.geo2dsphere("ubicacion"))
        db.getCollection<Document>(Colecciones.SOLICITUDES)
            .createIndex(Indexes.compoundIndex(Indexes.ascending("hospitalId"), Indexes.descending("creadaEn")))
        db.getCollection<Document>(Colecciones.RESPUESTAS)
            .createIndex(Indexes.ascending("solicitudId"))
        db.getCollection<Document>(Colecciones.RESPUESTAS)
            .createIndex(Indexes.ascending("donadorId"))
        db.getCollection<Document>(Colecciones.USUARIOS)
            .createIndex(Indexes.ascending("hospitalId"))
        db.getCollection<Document>(Colecciones.CITAS)
            .createIndex(Indexes.compoundIndex(Indexes.ascending("hospitalId"), Indexes.ascending("fechaHora")))
        db.getCollection<Document>(Colecciones.CITAS)
            .createIndex(Indexes.ascending("donadorId"))
        db.getCollection<Document>(Colecciones.DONACIONES)
            .createIndex(Indexes.ascending("donadorId"))
    }

    fun cerrar() = cliente.close()
}

object Colecciones {
    const val USUARIOS = "usuarios"
    const val DONADORES = "donadores"
    const val HOSPITALES = "hospitales"
    const val REGLAS_ELEGIBILIDAD = "reglas_elegibilidad"
    const val SOLICITUDES = "solicitudes"
    const val RESPUESTAS = "respuestas"
    const val CITAS = "citas"
    const val DONACIONES = "donaciones"
    const val CONVERSACIONES = "conversaciones"
    const val PREGUNTAS_FRECUENTES = "preguntas_frecuentes"
    const val EVENTOS_PROCESADOS = "eventos_procesados"
}
