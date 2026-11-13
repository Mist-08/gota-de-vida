package mx.gotadevida.donador.service

/** Avisa al widget de la pantalla de inicio que cambió la elegibilidad guardada. */
fun interface ActualizadorWidget {
    suspend fun actualizar()
}
