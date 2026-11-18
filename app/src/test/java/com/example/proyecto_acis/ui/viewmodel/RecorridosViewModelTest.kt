package com.example.proyecto_acis.ui.viewmodel

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.model.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = AcisApplication::class)
class RecorridosViewModelTest {
    private lateinit var app: AcisApplication
    private lateinit var vm: OrdenesViewModel
    private val modelos = ViewModelStore()

    @Before fun preparar() {
        app = RuntimeEnvironment.getApplication() as AcisApplication
        runBlocking { app.repository.inicializar() }
        vm = OrdenesViewModel(app)
        modelos.put("ordenes", vm)
        val limite = System.nanoTime() + 5_000_000_000L
        while (vm.preferencias.value.cargando && System.nanoTime() < limite) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertFalse("La carga de preferencias debe terminar", vm.preferencias.value.cargando)
    }

    @After fun cerrar() {
        modelos.clear()
        app.tareas.cancel()
    }

    @Test fun recorreCreacionEdicionVisitaObservacionYConformidad() = runBlocking {
        val id = crear()
        assertTrue(vm.prepararEdicion(id))
        vm.cambiarDescripcion("Revisar refrigeración y luz")
        assertTrue(vm.guardarEdicion(id))
        assertTrue(vm.asignar(id, "Brian Bravo", "10:30"))

        vm.seleccionarTecnico("Brian Bravo")
        assertTrue(vm.aceptar(id))
        vm.iniciarVisita(id, null)
        assertEquals(EstadoOt.EN_VISITA, orden(id).estado)
        assertEquals("", orden(id).ubicacionCheckIn)
        vm.prepararDetalle(orden(id))
        vm.cambiarDiagnostico(id, "Filtro sucio")
        vm.cambiarActividad(id, "Limpieza de filtros")
        assertTrue(vm.autoguardar(id, vm.detalle.value.formularios.getValue(id)))
        vm.finalizarVisita(id, "Filtro sucio", "Limpieza de filtros", "")
        assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, orden(id).estado)

        vm.seleccionarPerfil(Perfil.CLIENTE)
        vm.observar(id, "Falta revisar la luz")
        assertEquals(EstadoOt.OBSERVADA, orden(id).estado)
        vm.seleccionarTecnico("Brian Bravo")
        vm.responderObservacion(id, "Se revisó y se cambió la ampolleta")
        vm.seleccionarPerfil(Perfil.CLIENTE)
        assertTrue(vm.aprobar(id))
        assertFalse(vm.aprobar(id))
        val final = orden(id)
        assertEquals(EstadoOt.CERRADA, final.estado)
        assertEquals("Revisar refrigeración y luz", final.descripcion)
        assertEquals("Falta revisar la luz", final.observacionCliente)
        assertTrue(final.checkIn.isNotBlank() && final.checkOut.isNotBlank() && final.fechaConformidad.isNotBlank())
        assertEquals(listOf(TipoEvento.CREACION, TipoEvento.EDICION, TipoEvento.ASIGNACION,
            TipoEvento.ACEPTACION, TipoEvento.LLEGADA, TipoEvento.INFORME, TipoEvento.OBSERVACION,
            TipoEvento.RESPUESTA, TipoEvento.CONFORMIDAD), vm.eventos.value.map { it.tipo })
        assertEquals("Brian Bravo", vm.eventos.value.first { it.tipo == TipoEvento.INFORME }.actor)
    }

    @Test fun cadaRolSoloPuedeRealizarSusAcciones() = runBlocking {
        val id = crear()
        assertFalse(vm.aceptar(id))
        assertFalse(vm.aprobar(id))
        assertTrue(vm.asignar(id, "Brian Bravo"))
        vm.seleccionarTecnico(vm.tecnicos.first { it != "Brian Bravo" })
        assertTrue(vm.ordenesParaPerfil(vm.ordenes.value, Perfil.TECNICO).isEmpty())
        assertFalse(vm.aceptar(id))
        assertFalse(vm.rechazar(id))
        assertFalse(vm.eliminar(id))
        vm.seleccionarPerfil(Perfil.CLIENTE)
        assertNull(vm.crear())
        assertFalse(vm.asignar(id))
        assertTrue(vm.ordenesParaPerfil(vm.ordenes.value, Perfil.CLIENTE).isEmpty())
        assertEquals(EstadoOt.ASIGNADA, orden(id).estado)
        assertEquals(2, vm.eventos.value.size)
    }

    @Test fun losErroresNoAvanzanElFlujoYSePuedenCorregir() = runBlocking {
        vm.seleccionarPerfil(Perfil.PLANIFICADOR)
        vm.prepararCreacion()
        assertNull(vm.crear())
        assertTrue(vm.creacion.value.errores.hayErrores)
        assertTrue(vm.ordenes.value.isEmpty())
        val id = crear()
        assertTrue(vm.asignar(id, "Brian Bravo"))
        vm.seleccionarTecnico("Brian Bravo")
        assertTrue(vm.aceptar(id))
        vm.iniciarVisita(id, null)
        vm.prepararDetalle(orden(id))
        vm.finalizarVisita(id, "", "", "")
        assertEquals(EstadoOt.EN_VISITA, orden(id).estado)
        assertEquals(id, vm.detalle.value.informeConErrores)
        vm.finalizarVisita(id, "Filtro sucio", "Limpieza", "")
        assertNull(vm.detalle.value.informeConErrores)
        vm.seleccionarPerfil(Perfil.CLIENTE)
        vm.observar(id, " ")
        assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, orden(id).estado)
        assertEquals(id, vm.detalle.value.observacionConError)
        vm.observar(id, "Revisar nuevamente")
        assertNull(vm.detalle.value.observacionConError)
        vm.seleccionarTecnico("Brian Bravo")
        vm.responderObservacion(id, "")
        assertEquals(EstadoOt.OBSERVADA, orden(id).estado)
        assertEquals(id, vm.detalle.value.respuestaConError)
        vm.responderObservacion(id, "Revisión completada")
        assertNull(vm.detalle.value.respuestaConError)
        assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, orden(id).estado)
    }

    @Test fun rechazarPermiteReasignarYPostergarSinPerderLaOrden() = runBlocking {
        val id = crear()
        assertTrue(vm.asignar(id, "Brian Bravo"))
        vm.seleccionarTecnico("Brian Bravo")
        assertTrue(vm.rechazar(id))
        assertEquals(EstadoOt.CREADA, orden(id).estado)
        assertEquals("", orden(id).tecnico)
        vm.seleccionarPerfil(Perfil.PLANIFICADOR)
        val otro = vm.tecnicos.first { it != "Brian Bravo" }
        assertTrue(vm.asignar(id, otro))
        vm.seleccionarTecnico(otro)
        assertFalse(vm.postergar(id, "01/01/2000 09:00"))
        assertTrue(vm.postergar(id, "21/11/2030 11:00"))
        assertEquals("21/11/2030 11:00", orden(id).fechaProgramada)
        assertTrue(vm.aceptar(id))
        vm.iniciarVisita(id, "-36.82, -73.05")
        assertEquals("-36.82, -73.05", orden(id).ubicacionCheckIn)
        assertFalse(vm.rechazar(id))
        assertEquals(EstadoOt.EN_VISITA, orden(id).estado)
    }

    private fun orden(id: String) = vm.ordenes.value.single { it.id == id }

    private suspend fun crear(): String {
        vm.seleccionarPerfil(Perfil.PLANIFICADOR)
        vm.prepararCreacion("Local de prueba", "Refrigerador")
        vm.cambiarDescripcion("Revisar refrigeración")
        vm.cambiarTipo(TipoOt.CORRECTIVA)
        vm.cambiarPrioridad(PrioridadOt.MEDIA)
        vm.cambiarFecha("20/11/2030")
        vm.cambiarHora("09:00")
        return requireNotNull(vm.crear())
    }
}
