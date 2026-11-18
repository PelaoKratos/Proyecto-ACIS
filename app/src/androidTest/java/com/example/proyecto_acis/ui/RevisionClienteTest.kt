package com.example.proyecto_acis.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.model.*
import com.example.proyecto_acis.ui.screens.ordenes.DetalleOrden
import com.example.proyecto_acis.ui.theme.ProyectoACISTheme
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RevisionClienteTest {
    @get:Rule val compose = createComposeRule()
    private val modelos = ViewModelStore()

    @After fun cerrar() { compose.runOnIdle { modelos.clear() } }

    @Test fun clienteObservaTecnicoRespondeYClienteApruebaDesdeLaPantalla() {
        val app = ApplicationProvider.getApplicationContext<AcisApplication>()
        val id = runBlocking {
            app.preferencias.recordar(false, Perfil.CLIENTE, "")
            val repo = app.repository
            repo.inicializar()
            val id = requireNotNull(repo.crear("Local de prueba", "Refrigerador de prueba", "Revisar luz",
                "20/11/2030 09:00", TipoOt.CORRECTIVA, PrioridadOt.MEDIA))
            check(repo.asignar(id, "Brian Bravo"))
            check(repo.aceptar(id))
            check(repo.iniciarVisita(id, null))
            check(repo.enviarInforme(id, "Falla eléctrica", "Cambio de ampolleta", ""))
            id
        }
        lateinit var vm: OrdenesViewModel
        lateinit var clima: ClimaViewModel
        compose.runOnIdle {
            app.sesion.perfil = Perfil.CLIENTE
            vm = OrdenesViewModel(app)
            clima = ClimaViewModel(app)
            modelos.put("ordenes", vm)
            modelos.put("clima", clima)
        }
        compose.waitUntil(10_000) { !vm.preferencias.value.cargando }
        compose.setContent {
            val ordenes by vm.ordenes.collectAsState()
            ProyectoACISTheme {
                // Cambiar de rol vuelve a abrir el detalle, igual que la navegación.
                key(vm.perfil) {
                    DetalleOrden(ordenes.single { it.id == id }, vm.perfil, vm, clima, {})
                }
            }
        }

        desplazarHasta("Dejar observación").performClick()
        desplazarHasta("Escribe una observación").assertIsDisplayed()
        desplazarHasta("Tu observación").performTextInput("Revisar también el interruptor")
        compose.onNodeWithText("Escribe una observación").assertDoesNotExist()
        desplazarHasta("Dejar observación").performClick()
        esperarEstado(vm, id, EstadoOt.OBSERVADA)

        compose.runOnIdle { runBlocking { vm.seleccionarTecnico("Brian Bravo") } }
        desplazarHasta("Enviar respuesta").performClick()
        desplazarHasta("Escribe una respuesta").assertIsDisplayed()
        desplazarHasta("Respuesta al cliente").performTextInput("Interruptor revisado y operativo")
        compose.onNodeWithText("Escribe una respuesta").assertDoesNotExist()
        desplazarHasta("Enviar respuesta").performClick()
        esperarEstado(vm, id, EstadoOt.PENDIENTE_CONFORMIDAD)

        compose.runOnIdle { runBlocking { vm.seleccionarPerfil(Perfil.CLIENTE) } }
        desplazarHasta("Aprobar trabajo").performClick()
        compose.onNodeWithText("Volver").performClick()
        compose.runOnIdle { assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, vm.ordenes.value.single { it.id == id }.estado) }
        desplazarHasta("Aprobar trabajo").performClick()
        compose.onNodeWithText("Sí, aprobar", useUnmergedTree = true).performClick()
        esperarEstado(vm, id, EstadoOt.CERRADA)
        compose.runOnIdle {
            val final = vm.ordenes.value.single { it.id == id }
            assertEquals("Revisar también el interruptor", final.observacionCliente)
            assertEquals("Interruptor revisado y operativo", final.respuestaTecnico)
            assertTrue(final.fechaConformidad.isNotBlank())
        }
    }

    private fun desplazarHasta(texto: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(texto))
        return compose.onNodeWithText(texto)
    }

    private fun esperarEstado(vm: OrdenesViewModel, id: String, estado: EstadoOt) {
        compose.waitUntil(10_000) { vm.ordenes.value.single { it.id == id }.estado == estado }
    }
}
