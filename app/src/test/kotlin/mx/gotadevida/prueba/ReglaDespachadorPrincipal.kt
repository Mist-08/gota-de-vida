package mx.gotadevida.prueba

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Cambia Dispatchers.Main por uno de prueba para poder probar ViewModels. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDespachadorPrincipal(val despachador: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(despachador)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
