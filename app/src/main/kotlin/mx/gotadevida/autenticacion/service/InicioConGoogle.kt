package mx.gotadevida.autenticacion.service

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import mx.gotadevida.BuildConfig

/** Resultado de pedir la cuenta de Google al teléfono. */
sealed interface CuentaGoogleResultado {
    data class Token(val idToken: String) : CuentaGoogleResultado

    data object Cancelado : CuentaGoogleResultado

    data object NoDisponible : CuentaGoogleResultado
}

/**
 * "Continuar con Google" con Credential Manager (gratis).
 * Requiere `GOOGLE_WEB_CLIENT_ID` en local.properties: el ID de cliente WEB de OAuth,
 * el mismo que el backend tiene en `GOOGLE_CLIENT_ID`.
 */
object InicioConGoogle {
    val estaConfigurado: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Necesita el contexto de la Activity para mostrar la hoja de selección de cuenta. */
    suspend fun pedirToken(contextoActividad: Context): CuentaGoogleResultado {
        if (!estaConfigurado) return CuentaGoogleResultado.NoDisponible
        val opcion = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val solicitud = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        return try {
            val credencial = CredentialManager.create(contextoActividad)
                .getCredential(contextoActividad, solicitud)
                .credential
            if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                CuentaGoogleResultado.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
            } else {
                CuentaGoogleResultado.NoDisponible
            }
        } catch (e: GetCredentialCancellationException) {
            CuentaGoogleResultado.Cancelado
        } catch (e: NoCredentialException) {
            // El teléfono no tiene ninguna cuenta de Google: se sigue con correo y contraseña.
            CuentaGoogleResultado.NoDisponible
        } catch (e: GetCredentialException) {
            CuentaGoogleResultado.NoDisponible
        }
    }
}
