package mx.gotadevida.core.tiempo

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Fecha de hoy en La Laguna. Las pruebas cambian el reloj para fijar el día. */
class Calendario @Inject constructor() {
    var reloj: Clock = Clock.system(ZoneId.of("America/Monterrey"))

    fun hoy(): LocalDate = LocalDate.now(reloj)
}
