package mx.gotadevida.core.dominio

import kotlinx.serialization.Serializable

/**
 * Tipos de sangre (sistema ABO y factor Rh).
 * La etiqueta usa el signo de menos real (U+2212) para mostrarse en la app.
 */
@Serializable
enum class TipoSangre(val etiqueta: String) {
    O_NEGATIVO("O−"),
    O_POSITIVO("O+"),
    A_NEGATIVO("A−"),
    A_POSITIVO("A+"),
    B_NEGATIVO("B−"),
    B_POSITIVO("B+"),
    AB_NEGATIVO("AB−"),
    AB_POSITIVO("AB+")
    ;

    private val grupo: String get() = name.substringBefore('_')
    private val esPositivo: Boolean get() = name.endsWith("POSITIVO")

    /**
     * Compatibilidad de glóbulos rojos: el receptor debe tener los antígenos del donador.
     * O− dona a todos; AB+ recibe de todos.
     */
    fun puedeDonarA(receptor: TipoSangre): Boolean {
        val aboCompatible = when (grupo) {
            "O" -> true
            "A" -> receptor.grupo == "A" || receptor.grupo == "AB"
            "B" -> receptor.grupo == "B" || receptor.grupo == "AB"
            else -> receptor.grupo == "AB"
        }
        val rhCompatible = !esPositivo || receptor.esPositivo
        return aboCompatible && rhCompatible
    }

    companion object {
        /** Tipos de donador que sirven para un paciente con el tipo indicado. */
        fun donadoresCompatiblesCon(receptor: TipoSangre): List<TipoSangre> = entries.filter { it.puedeDonarA(receptor) }
    }
}
