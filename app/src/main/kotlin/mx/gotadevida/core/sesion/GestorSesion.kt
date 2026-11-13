package mx.gotadevida.core.sesion

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.gotadevida.core.database.GotaDatabase

/** Cerrar sesión borra los tokens y la copia local de datos personales. */
@Singleton
class GestorSesion @Inject constructor(private val almacen: AlmacenSesion, private val baseDeDatos: GotaDatabase) {
    suspend fun cerrarSesion() {
        almacen.borrar()
        withContext(Dispatchers.IO) { baseDeDatos.clearAllTables() }
    }
}
