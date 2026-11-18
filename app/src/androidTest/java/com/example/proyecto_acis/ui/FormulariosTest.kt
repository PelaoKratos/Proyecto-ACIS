package com.example.proyecto_acis.ui

import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.ui.screens.catalogo.FormularioCliente
import com.example.proyecto_acis.ui.screens.ordenes.CrearOrden
import com.example.proyecto_acis.ui.theme.ProyectoACISTheme
import com.example.proyecto_acis.ui.viewmodel.CatastroViewModel
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormulariosTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var application: AcisApplication

    @Before fun preparar() {
        application = ApplicationProvider.getApplicationContext()
        application.sesion.perfil = Perfil.PLANIFICADOR
    }

    @Test fun nombreDelClienteCorrigeElErrorEnLaPantalla() {
        val viewModel = CatastroViewModel(application)
        viewModel.prepararCliente()
        compose.setContent { ProyectoACISTheme { FormularioCliente(viewModel) {} } }

        compose.onNodeWithText("Guardar", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Ingresa el nombre del cliente.").assertIsDisplayed()
        compose.onNodeWithText("Nombre del cliente").performTextInput("Cliente de prueba")
        compose.onNodeWithText("Ingresa el nombre del cliente.").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Cliente de prueba", viewModel.estado.value.cliente.datos.nombre) }
    }

    @Test fun volverAlPasoAnteriorConservaLosDatosDeLaOt() {
        val viewModel = OrdenesViewModel(application)
        val climaViewModel = ClimaViewModel(application)
        viewModel.prepararCreacion("Local de prueba", "Refrigerador de prueba")
        compose.setContent {
            ProyectoACISTheme { CrearOrden(viewModel, climaViewModel, {}, {}) }
        }

        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Siguiente").performClick()
        compose.runOnIdle { assertEquals(2, viewModel.creacion.value.paso) }
        compose.onNodeWithText("Describe el trabajo").assertIsDisplayed()
        compose.onNodeWithText("Descripción del trabajo").performTextInput("Revisar refrigeración")
        compose.onNodeWithText("Describe el trabajo").assertDoesNotExist()
        compose.onNodeWithText("Anterior").performClick()
        compose.onNodeWithText("Siguiente").performClick()
        compose.runOnIdle { assertEquals("Revisar refrigeración", viewModel.creacion.value.datos.descripcion) }
    }

    @Test fun consultarInformeNoModificaElEstadoYNuevaPreparacionConservaLoEscrito() {
        val viewModel = OrdenesViewModel(application)
        val orden = OrdenTrabajo("OT-PRUEBA", "Local", "Equipo", "Revisar", "20/11/2026 09:00")
        viewModel.formularioDetalle(orden)
        assertTrue(viewModel.detalle.value.formularios.isEmpty())
        viewModel.prepararDetalle(orden)
        viewModel.cambiarDiagnostico(orden.id, "Filtro sucio")
        viewModel.cambiarObservacion(orden.id, "Revisar de nuevo")
        viewModel.prepararDetalle(orden)
        assertEquals("Filtro sucio", viewModel.detalle.value.formularios[orden.id]?.diagnostico)
        assertEquals("Revisar de nuevo", viewModel.detalle.value.formularios[orden.id]?.observacion)
        assertTrue(viewModel.detalle.value.borradores[orden.id].orEmpty().isNotBlank())
    }

    @Test fun selectoresAbrenLaFechaYHoraDelFormularioYCancelarNoLasCambia() {
        val viewModel = OrdenesViewModel(application)
        val climaViewModel = ClimaViewModel(application)
        viewModel.prepararCreacion("Local de prueba", "Equipo de prueba")
        viewModel.cambiarDescripcion("Revisar equipo")
        viewModel.cambiarTipo(TipoOt.CORRECTIVA)
        viewModel.cambiarPrioridad(PrioridadOt.MEDIA)
        viewModel.cambiarFecha("15/08/2030")
        viewModel.cambiarHora("07:35")
        viewModel.avanzarPasoCrear()
        viewModel.avanzarPasoCrear()
        compose.setContent {
            ProyectoACISTheme { CrearOrden(viewModel, climaViewModel, {}, {}, editando = true) }
        }

        compose.onNodeWithText("15/08/2030").performClick()
        Espresso.onView(isAssignableFrom(DatePicker::class.java)).inRoot(isDialog()).check { vista, error ->
            if (error != null) throw error
            val selector = vista as DatePicker
            assertEquals(2030, selector.year)
            assertEquals(7, selector.month)
            assertEquals(15, selector.dayOfMonth)
        }
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(pressBack())
        compose.onNodeWithText("07:35").performClick()
        Espresso.onView(isAssignableFrom(TimePicker::class.java)).inRoot(isDialog()).check { vista, error ->
            if (error != null) throw error
            val selector = vista as TimePicker
            assertEquals(7, selector.hour)
            assertEquals(35, selector.minute)
        }
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(pressBack())
        compose.runOnIdle {
            assertEquals("15/08/2030", viewModel.creacion.value.datos.fecha)
            assertEquals("07:35", viewModel.creacion.value.datos.hora)
            assertEquals(3, viewModel.creacion.value.paso)
        }
    }
}
