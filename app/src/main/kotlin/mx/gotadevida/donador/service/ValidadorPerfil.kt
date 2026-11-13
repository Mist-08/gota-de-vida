package mx.gotadevida.donador.service

import java.time.LocalDate
import mx.gotadevida.donador.entity.SexoParaCriteriosMedicos
import mx.gotadevida.donador.entity.TipoSangre

enum class CampoPerfil { TIPO_SANGRE, NACIMIENTO, PESO, SEXO, UBICACION, ULTIMA_DONACION }

enum class ErrorPerfil { REQUERIDO, PESO_INVALIDO, FECHA_FUTURA, EDAD_INVALIDA }

/** Datos que el donador llena en la pantalla de perfil. */
data class DatosPerfil(
    val tipoSangre: TipoSangre? = null,
    val noSeTipo: Boolean = false,
    val fechaNacimiento: LocalDate? = null,
    val peso: String = "",
    val sexo: SexoParaCriteriosMedicos? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val haDonado: Boolean? = null,
    val fechaUltimaDonacion: LocalDate? = null
)

/** Revisa el perfil antes de enviarlo. Mismos rangos que el backend. */
object ValidadorPerfil {

    /** Convierte "68", "68.5" o "68,5" en número. */
    fun pesoComoNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    fun validar(datos: DatosPerfil, hoy: LocalDate): Map<CampoPerfil, ErrorPerfil> = buildMap {
        if (datos.tipoSangre == null && !datos.noSeTipo) put(CampoPerfil.TIPO_SANGRE, ErrorPerfil.REQUERIDO)

        val nacimiento = datos.fechaNacimiento
        when {
            nacimiento == null -> put(CampoPerfil.NACIMIENTO, ErrorPerfil.REQUERIDO)
            nacimiento.isAfter(hoy) -> put(CampoPerfil.NACIMIENTO, ErrorPerfil.FECHA_FUTURA)
            nacimiento.isBefore(hoy.minusYears(100)) -> put(CampoPerfil.NACIMIENTO, ErrorPerfil.EDAD_INVALIDA)
        }

        val peso = pesoComoNumero(datos.peso)
        when {
            datos.peso.isBlank() -> put(CampoPerfil.PESO, ErrorPerfil.REQUERIDO)
            peso == null || peso !in 30.0..250.0 -> put(CampoPerfil.PESO, ErrorPerfil.PESO_INVALIDO)
        }

        if (datos.sexo == null) put(CampoPerfil.SEXO, ErrorPerfil.REQUERIDO)
        if (datos.latitud == null || datos.longitud == null) put(CampoPerfil.UBICACION, ErrorPerfil.REQUERIDO)

        if (datos.haDonado == null) {
            put(CampoPerfil.ULTIMA_DONACION, ErrorPerfil.REQUERIDO)
        } else if (datos.haDonado) {
            val ultima = datos.fechaUltimaDonacion
            when {
                ultima == null -> put(CampoPerfil.ULTIMA_DONACION, ErrorPerfil.REQUERIDO)
                ultima.isAfter(hoy) -> put(CampoPerfil.ULTIMA_DONACION, ErrorPerfil.FECHA_FUTURA)
            }
        }
    }
}
