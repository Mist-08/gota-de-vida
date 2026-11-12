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

data class CitaResumen(
    val id: String,
    val hospitalId: String,
    val hospitalNombre: String,
    val fechaHora: String,
    val estado: String,
)

/** Lo que el chatbot puede hacer con las citas (las mismas reglas que la app). */
interface DirectorioCitas {
    /** Horas libres de ese día, por ejemplo `["08:00", "09:30"]`. */
    suspend fun horasLibres(hospitalId: String, fecha: String): List<String>

    suspend fun agendar(donadorId: String, hospitalId: String, fechaHora: String, solicitudId: String?): CitaResumen

    suspend fun cancelarCita(donadorId: String, citaId: String): CitaResumen

    suspend fun proximas(donadorId: String): List<CitaResumen>
}

/** Requisitos vigentes (de `reglas_elegibilidad`) para que el chatbot no invente datos. */
data class RequisitosVigentes(
    val edadMinima: Int,
    val edadMaxima: Int,
    val pesoMinimoKg: Double,
    val diasEntreDonaciones: Int,
    val horasAyunoMinimo: Int,
    val horasAyunoMaximo: Int,
    val mesesEsperaTatuaje: Int,
    val requisitos: List<String>,
)
