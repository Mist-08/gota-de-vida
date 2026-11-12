package mx.gotadevida.autenticacion.repository

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import mx.gotadevida.autenticacion.entity.Usuario
import mx.gotadevida.core.db.Colecciones
import mx.gotadevida.core.errores.Conflicto

interface UsuarioRepository {
    /** Lanza [Conflicto] si el correo ya existe. */
    suspend fun crear(usuario: Usuario)

    suspend fun buscarPorId(id: String): Usuario?

    suspend fun buscarPorEmail(email: String): Usuario?

    suspend fun buscarPorGoogleId(googleId: String): Usuario?

    suspend fun actualizar(usuario: Usuario)
}

class MongoUsuarioRepository(db: MongoDatabase) : UsuarioRepository {
    private val coleccion = db.getCollection<Usuario>(Colecciones.USUARIOS)

    override suspend fun crear(usuario: Usuario) {
        try {
            coleccion.insertOne(usuario)
        } catch (e: MongoWriteException) {
            if (ErrorCategory.fromErrorCode(e.code) == ErrorCategory.DUPLICATE_KEY) {
                throw Conflicto("Ya existe una cuenta con ese correo.")
            }
            throw e
        }
    }

    override suspend fun buscarPorId(id: String): Usuario? =
        coleccion.find(Filters.eq("_id", id)).firstOrNull()

    override suspend fun buscarPorEmail(email: String): Usuario? =
        coleccion.find(Filters.eq("email", email)).firstOrNull()

    override suspend fun buscarPorGoogleId(googleId: String): Usuario? =
        coleccion.find(Filters.eq("googleId", googleId)).firstOrNull()

    override suspend fun actualizar(usuario: Usuario) {
        coleccion.replaceOne(Filters.eq("_id", usuario.id), usuario)
    }
}
