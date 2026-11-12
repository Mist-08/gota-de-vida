package mx.gotadevida.core.dominio

/**
 * Contratos que una funcionalidad ofrece a las demás sin exponer sus clases internas
 * (regla: las funcionalidades solo comparten lo que está en `core`).
 * Las implementan los services de donador, hospital y autenticación; se conectan en `core/di`.
 */

/** Lo mínimo de un donador que necesitan urgencias, citas y el chatbot. */
data class DonadorResumen(
    val id: String,
    val nombre: String,
    val telefono: String?,
    val tipoSangre: TipoSangre?,
    val ubicacion: PuntoGeo?,
    val radioMaximoKm: Int,
    val elegible: Boolean,
    val proximaFechaElegible: String?,
)

/** Donador candidato a una urgencia con su distancia al hospital. */
data class Candidato(val donador: DonadorResumen, val distanciaKm: Double)

interface DirectorioDonadores {
    suspend fun resumen(id: String): DonadorResumen?

    /**
     * Donadores con tipo de sangre en `tipos`, notificaciones activas, que ya pueden donar
     * y que están dentro de `radioKm` del hospital y de su propia distancia máxima.
     * Ordenados del más cercano al más lejano.
     */
    suspend fun candidatos(
        centro: PuntoGeo,
        radioKm: Double,
        tipos: List<TipoSangre>,
        excluir: Set<String>,
        limite: Int,
    ): List<Candidato>

    /** Registra una donación confirmada (fecha ISO) en el perfil del donador. */
    suspend fun registrarDonacion(id: String, fecha: String)
}

data class HospitalResumen(
    val id: String,
    val nombre: String,
    val direccion: String,
    val ubicacion: PuntoGeo,
    val telefono: String?,
)

interface DirectorioHospitales {
    suspend fun resumen(id: String): HospitalResumen?
}

interface DirectorioUsuarios {
    /** Tokens de Firebase Cloud Messaging de esos usuarios. */
    suspend fun tokensFcm(usuarioIds: Collection<String>): List<String>

    /** Ids de las cuentas del personal de un hospital. */
    suspend fun usuariosDeHospital(hospitalId: String): List<String>

    /** Quita un token que FCM reporta como inválido. */
    suspend fun quitarTokenFcm(token: String)
}
