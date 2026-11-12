package mx.gotadevida.core.sesion

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Sesión iniciada: quién es y sus tokens. */
data class Sesion(
    val usuarioId: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val tokenAcceso: String,
    val tokenRefresco: String,
)

/** Guarda la sesión en el teléfono. Es una interfaz para poder usar una versión en memoria en las pruebas. */
interface AlmacenSesion {
    val sesion: Flow<Sesion?>

    suspend fun actual(): Sesion? = sesion.first()

    suspend fun guardar(sesion: Sesion)

    suspend fun actualizarTokens(tokenAcceso: String, tokenRefresco: String)

    suspend fun borrar()
}

private val Context.almacenSesion by preferencesDataStore(name = "sesion")

@Singleton
class AlmacenSesionDataStore @Inject constructor(
    @param:ApplicationContext private val contexto: Context,
) : AlmacenSesion {

    override val sesion: Flow<Sesion?> = contexto.almacenSesion.data.map { p ->
        val id = p[USUARIO_ID] ?: return@map null
        val acceso = p[TOKEN_ACCESO] ?: return@map null
        val refresco = p[TOKEN_REFRESCO] ?: return@map null
        Sesion(
            usuarioId = id,
            nombre = p[NOMBRE].orEmpty(),
            email = p[EMAIL].orEmpty(),
            rol = p[ROL].orEmpty(),
            tokenAcceso = acceso,
            tokenRefresco = refresco,
        )
    }

    override suspend fun guardar(sesion: Sesion) {
        contexto.almacenSesion.edit { p ->
            p[USUARIO_ID] = sesion.usuarioId
            p[NOMBRE] = sesion.nombre
            p[EMAIL] = sesion.email
            p[ROL] = sesion.rol
            p[TOKEN_ACCESO] = sesion.tokenAcceso
            p[TOKEN_REFRESCO] = sesion.tokenRefresco
        }
    }

    override suspend fun actualizarTokens(tokenAcceso: String, tokenRefresco: String) {
        contexto.almacenSesion.edit { p ->
            p[TOKEN_ACCESO] = tokenAcceso
            p[TOKEN_REFRESCO] = tokenRefresco
        }
    }

    override suspend fun borrar() {
        contexto.almacenSesion.edit { it.clear() }
    }

    private companion object {
        val USUARIO_ID = stringPreferencesKey("usuario_id")
        val NOMBRE = stringPreferencesKey("nombre")
        val EMAIL = stringPreferencesKey("email")
        val ROL = stringPreferencesKey("rol")
        val TOKEN_ACCESO = stringPreferencesKey("token_acceso")
        val TOKEN_REFRESCO = stringPreferencesKey("token_refresco")
    }
}
