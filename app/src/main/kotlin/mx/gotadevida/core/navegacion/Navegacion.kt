package mx.gotadevida.core.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import mx.gotadevida.ajustes.ui.AjustesScreen
import mx.gotadevida.autenticacion.ui.BienvenidaScreen
import mx.gotadevida.autenticacion.ui.InicioSesionScreen
import mx.gotadevida.autenticacion.ui.RecuperarContrasenaScreen
import mx.gotadevida.autenticacion.ui.RegistroScreen
import mx.gotadevida.chatbot.ui.ChatScreen
import mx.gotadevida.chatbot.viewmodel.Chat
import mx.gotadevida.cita.ui.AgendarScreen
import mx.gotadevida.cita.ui.MiCitaScreen
import mx.gotadevida.cita.ui.TarjetaMiCita
import mx.gotadevida.cita.viewmodel.AgendarCita
import mx.gotadevida.cita.viewmodel.MiCita
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.donador.ui.InicioScreen
import mx.gotadevida.donador.ui.PerfilScreen
import mx.gotadevida.historial.ui.HistorialScreen
import mx.gotadevida.historial.viewmodel.HistorialRuta
import mx.gotadevida.hospital.ui.CrearUrgenciaScreen
import mx.gotadevida.hospital.ui.DetalleUrgenciaHospitalScreen
import mx.gotadevida.hospital.ui.EscanearQrScreen
import mx.gotadevida.hospital.ui.PanelHospitalScreen
import mx.gotadevida.hospital.viewmodel.DetalleUrgenciaHospital
import mx.gotadevida.hospital.viewmodel.EscanearQr
import mx.gotadevida.notificaciones.ui.RegistroDeAvisos
import mx.gotadevida.solicitud.ui.DetalleUrgenciaScreen
import mx.gotadevida.solicitud.ui.SeccionUrgencias
import mx.gotadevida.solicitud.viewmodel.DetalleUrgencia

// Rutas con tipo: Navigation Compose las serializa con kotlinx.serialization.
@Serializable data object Bienvenida

@Serializable data class Registro(val aceptoAviso: Boolean)

@Serializable data object IniciarSesion

@Serializable data class RecuperarContrasena(val email: String = "")

@Serializable data class Perfil(val esRegistro: Boolean)

@Serializable data object Inicio

@Serializable data object Ajustes

@Serializable data object PanelHospital

@Serializable data object CrearUrgencia

/** Estado de arranque: todavía leyendo la sesión, sin sesión, o con sesión de donador u hospital. */
enum class EstadoArranque { LEYENDO, SIN_SESION, DONADOR, HOSPITAL }

@HiltViewModel
class ArranqueViewModel @Inject constructor(almacen: AlmacenSesion) : ViewModel() {
    val estado: StateFlow<EstadoArranque> = almacen.sesion
        .map {
            when {
                it == null -> EstadoArranque.SIN_SESION
                it.rol == "HOSPITAL" || it.rol == "ADMIN" -> EstadoArranque.HOSPITAL
                else -> EstadoArranque.DONADOR
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoArranque.LEYENDO)
}

@Composable
fun NavegacionGota(
    solicitudPorAbrir: String? = null,
    alAbrirSolicitud: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    arranque: ArranqueViewModel = hiltViewModel()
) {
    val estado by arranque.estado.collectAsStateWithLifecycle()
    if (estado == EstadoArranque.LEYENDO) {
        // Fondo liso mientras DataStore responde (unos milisegundos).
        Box(Modifier.fillMaxSize().background(GotaTema.colores.surface))
        return
    }
    val inicio: Any = if (estado == EstadoArranque.HOSPITAL) PanelHospital else Inicio

    LaunchedEffect(estado) {
        val actual = navController.currentDestination
        val enZonaPublica = actual == null ||
            actual.hasRoute<Bienvenida>() || actual.hasRoute<Registro>() || actual.hasRoute<IniciarSesion>() ||
            actual.hasRoute<RecuperarContrasena>()
        // Si la sesión se pierde (cerrar sesión o token de renovación vencido) se vuelve a la bienvenida.
        if (estado == EstadoArranque.SIN_SESION && !enZonaPublica) {
            navController.navigate(Bienvenida) { popUpTo(0) { inclusive = true } }
        }
        // Al entrar como hospital se va a su panel (el donador sigue su propio flujo de registro).
        if (estado == EstadoArranque.HOSPITAL && enZonaPublica && actual != null) {
            navController.navigate(PanelHospital) { popUpTo(0) { inclusive = true } }
        }
    }

    // Abrir la urgencia de una notificación.
    LaunchedEffect(solicitudPorAbrir, estado) {
        val id = solicitudPorAbrir ?: return@LaunchedEffect
        when (estado) {
            EstadoArranque.DONADOR -> navController.navigate(DetalleUrgencia(id))
            EstadoArranque.HOSPITAL -> navController.navigate(DetalleUrgenciaHospital(id))
            else -> return@LaunchedEffect
        }
        alAbrirSolicitud()
    }

    if (estado != EstadoArranque.SIN_SESION) RegistroDeAvisos()

    NavHost(
        navController = navController,
        startDestination = if (estado == EstadoArranque.SIN_SESION) Bienvenida else inicio,
        modifier = Modifier.fillMaxSize().background(GotaTema.colores.surface)
    ) {
        composable<Bienvenida> {
            BienvenidaScreen(
                alCrearCuenta = { navController.navigate(Registro(aceptoAviso = true)) },
                alIniciarSesion = { navController.navigate(IniciarSesion) }
            )
        }
        composable<Registro> { entrada ->
            RegistroScreen(
                aceptoAviso = entrada.toRoute<Registro>().aceptoAviso,
                alRegresar = navController::popBackStack,
                alRegistrarse = {
                    navController.navigate(Perfil(esRegistro = true)) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable<IniciarSesion> {
            InicioSesionScreen(
                alRegresar = navController::popBackStack,
                // El rol decide a dónde ir: el LaunchedEffect de arriba manda al hospital a su panel.
                alEntrar = {
                    if (estado !=
                        EstadoArranque.HOSPITAL
                    ) {
                        navController.navigate(Inicio) { popUpTo(0) { inclusive = true } }
                    }
                },
                alOlvideContrasena = { email -> navController.navigate(RecuperarContrasena(email)) }
            )
        }
        composable<RecuperarContrasena> { entrada ->
            RecuperarContrasenaScreen(
                emailInicial = entrada.toRoute<RecuperarContrasena>().email,
                alRegresar = navController::popBackStack,
                alEntrar = {
                    if (estado != EstadoArranque.HOSPITAL) {
                        navController.navigate(Inicio) { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
        composable<Perfil> { entrada ->
            val esRegistro = entrada.toRoute<Perfil>().esRegistro
            val salir = {
                if (esRegistro) {
                    navController.navigate(Inicio) { popUpTo(0) { inclusive = true } }
                } else {
                    navController.popBackStack()
                }
            }
            PerfilScreen(esRegistro = esRegistro, alRegresar = { salir() }, alGuardar = { salir() })
        }
        composable<Inicio> {
            InicioScreen(
                alAbrirAjustes = { navController.navigate(Ajustes) { launchSingleTop = true } },
                alEditarPerfil = { navController.navigate(Perfil(esRegistro = false)) { launchSingleTop = true } },
                urgencias = { SeccionUrgencias(alAbrir = { navController.navigate(DetalleUrgencia(it)) }) },
                cita = { TarjetaMiCita(alAbrir = { navController.navigate(MiCita) }) },
                alHistorial = { navController.navigate(HistorialRuta) },
                alChat = { navController.navigate(Chat) }
            )
        }
        composable<DetalleUrgencia> {
            DetalleUrgenciaScreen(
                alRegresar = navController::popBackStack,
                alAgendar = { hospitalId, nombre, solicitudId ->
                    navController.navigate(AgendarCita(hospitalId, nombre, solicitudId))
                }
            )
        }
        composable<AgendarCita> {
            AgendarScreen(
                alRegresar = navController::popBackStack,
                alAgendar = { navController.navigate(MiCita) { popUpTo(Inicio) } }
            )
        }
        composable<MiCita> { MiCitaScreen(alRegresar = navController::popBackStack) }
        composable<HistorialRuta> { HistorialScreen(alRegresar = navController::popBackStack) }
        composable<Chat> { ChatScreen(alRegresar = navController::popBackStack) }
        composable<EscanearQr> { EscanearQrScreen(alRegresar = navController::popBackStack) }
        composable<PanelHospital> {
            PanelHospitalScreen(
                alNuevaUrgencia = { navController.navigate(CrearUrgencia) },
                alAbrir = { navController.navigate(DetalleUrgenciaHospital(it)) },
                alAjustes = { navController.navigate(Ajustes) { launchSingleTop = true } },
                alEscanear = { navController.navigate(EscanearQr) }
            )
        }
        composable<CrearUrgencia> {
            CrearUrgenciaScreen(
                alRegresar = navController::popBackStack,
                alPublicar = { id -> navController.navigate(DetalleUrgenciaHospital(id)) { popUpTo(PanelHospital) } }
            )
        }
        composable<DetalleUrgenciaHospital> {
            DetalleUrgenciaHospitalScreen(alRegresar = navController::popBackStack)
        }
        composable<Ajustes> {
            AjustesScreen(
                alRegresar = navController::popBackStack,
                alEditarPerfil = if (estado == EstadoArranque.DONADOR) {
                    { navController.navigate(Perfil(esRegistro = false)) { launchSingleTop = true } }
                } else {
                    null
                }
            )
        }
    }
}
