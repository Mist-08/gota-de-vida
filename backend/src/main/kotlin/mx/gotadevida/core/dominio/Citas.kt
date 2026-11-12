package mx.gotadevida.core.dominio

/** Resumen de una urgencia para las estadísticas del hospital y el chatbot. */
data class UrgenciaResumen(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val etiquetaTipoSangre: String,
    val tipoSangre: TipoSangre,
    val estado: String,
    val unidades: Int,
    val unidadesComprometidas: Int,
    val creadaEn: String,
    val cubiertaEn: String?,
    val distanciaKm: Double?,
    val miRespuesta: Boolean?,
)

/** Lo que otras funcionalidades (estadísticas, chatbot) pueden consultar de las urgencias. */
interface DirectorioUrgencias {
    suspend fun deHospital(hospitalId: String): List<UrgenciaResumen>

    /** Urgencias activas que ese donador puede cubrir, de la más cercana a la más lejana. */
    suspend fun paraDonador(donadorId: String): List<UrgenciaResumen>

    /** Registra que el donador acepta o no; devuelve un mensaje para mostrar si no se pudo. */
    suspend fun responderUrgencia(donadorId: String, solicitudId: String, acepta: Boolean): UrgenciaResumen
}
