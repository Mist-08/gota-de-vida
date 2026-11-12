package mx.gotadevida.autenticacion.repository

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
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

    suspend fun buscarPorIds(ids: Collection<String>): List<Usuario>

    suspend fun deHospital(hospitalId: String): List<Usuario>

    suspend fun conTokenFcm(token: String): List<Usuario>
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

    override suspend fun buscarPorIds(ids: Collection<String>): List<Usuario> =
        coleccion.find(Filters.`in`("_id", ids.toList())).toList()

    override suspend fun deHospital(hospitalId: String): List<Usuario> =
        coleccion.find(Filters.eq("hospitalId", hospitalId)).toList()

    override suspend fun conTokenFcm(token: String): List<Usuario> =
        coleccion.find(Filters.eq("tokensFcm", token)).toList()
}
