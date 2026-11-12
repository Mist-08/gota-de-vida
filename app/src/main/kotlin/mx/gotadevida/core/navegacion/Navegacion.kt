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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import mx.gotadevida.ajustes.ui.AjustesScreen
import mx.gotadevida.autenticacion.ui.BienvenidaScreen
import mx.gotadevida.autenticacion.ui.InicioSesionScreen
import mx.gotadevida.autenticacion.ui.RegistroScreen
import mx.gotadevida.core.designsystem.GotaTema
import mx.gotadevida.core.sesion.AlmacenSesion
import mx.gotadevida.donador.ui.InicioScreen
import mx.gotadevida.donador.ui.PerfilScreen
import javax.inject.Inject

// Rutas con tipo: Navigation Compose las serializa con kotlinx.serialization.
@Serializable data object Bienvenida

@Serializable data class Registro(val aceptoAviso: Boolean)

@Serializable data object IniciarSesion

@Serializable data class Perfil(val esRegistro: Boolean)

@Serializable data object Inicio

@Serializable data object Ajustes

/** Estado de arranque: todavía leyendo la sesión, sin sesión o con sesión. */
enum class EstadoArranque { LEYENDO, SIN_SESION, CON_SESION }

@HiltViewModel
class ArranqueViewModel @Inject constructor(almacen: AlmacenSesion) : ViewModel() {
    val estado: StateFlow<EstadoArranque> = almacen.sesion
        .map { if (it == null) EstadoArranque.SIN_SESION else EstadoArranque.CON_SESION }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoArranque.LEYENDO)
}

@Composable
fun NavegacionGota(
    navController: NavHostController = rememberNavController(),
    arranque: ArranqueViewModel = hiltViewModel(),
) {
    val estado by arranque.estado.collectAsStateWithLifecycle()
    if (estado == EstadoArranque.LEYENDO) {
        // Fondo liso mientras DataStore responde (unos milisegundos).
        Box(Modifier.fillMaxSize().background(GotaTema.colores.surface))
        return
    }

    // Si la sesión se pierde (cerrar sesión o token de renovación vencido) se vuelve a la bienvenida.
    LaunchedEffect(estado) {
        if (estado == EstadoArranque.SIN_SESION) {
            val actual = navController.currentDestination
            val enZonaPublica = actual == null ||
                actual.hasRoute<Bienvenida>() || actual.hasRoute<Registro>() || actual.hasRoute<IniciarSesion>()
            if (!enZonaPublica) {
                navController.navigate(Bienvenida) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (estado == EstadoArranque.CON_SESION) Inicio else Bienvenida,
        modifier = Modifier.fillMaxSize().background(GotaTema.colores.surface),
    ) {
        composable<Bienvenida> {
            BienvenidaScreen(
                alCrearCuenta = { navController.navigate(Registro(aceptoAviso = true)) },
                alIniciarSesion = { navController.navigate(IniciarSesion) },
            )
        }
        composable<Registro> { entrada ->
            RegistroScreen(
                aceptoAviso = entrada.toRoute<Registro>().aceptoAviso,
                alRegresar = navController::popBackStack,
                alRegistrarse = {
                    navController.navigate(Perfil(esRegistro = true)) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable<IniciarSesion> {
            InicioSesionScreen(
                alRegresar = navController::popBackStack,
                alEntrar = { navController.navigate(Inicio) { popUpTo(0) { inclusive = true } } },
            )
        }
        composable<Perfil> { entrada ->
            val esRegistro = entrada.toRoute<Perfil>().esRegistro
            PerfilScreen(
                esRegistro = esRegistro,
                alRegresar = {
                    if (esRegistro) {
                        navController.navigate(Inicio) { popUpTo(0) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                alGuardar = {
                    if (esRegistro) {
                        navController.navigate(Inicio) { popUpTo(0) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<Inicio> {
            InicioScreen(
                alAbrirAjustes = { navController.navigate(Ajustes) { launchSingleTop = true } },
                alEditarPerfil = { navController.navigate(Perfil(esRegistro = false)) { launchSingleTop = true } },
            )
        }
        composable<Ajustes> {
            AjustesScreen(
                alRegresar = navController::popBackStack,
                alEditarPerfil = { navController.navigate(Perfil(esRegistro = false)) { launchSingleTop = true } },
            )
        }
    }
}
