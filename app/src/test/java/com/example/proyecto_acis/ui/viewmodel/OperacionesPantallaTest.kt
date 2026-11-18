package com.example.proyecto_acis.ui.viewmodel

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class OperacionesPantallaTest {
    @Test fun noRepiteUnaAccionMientrasLaAnteriorSigueGuardando() = runBlocking {
        val trabajo = SupervisorJob()
        try {
            val operaciones = OperacionesPantalla(CoroutineScope(trabajo + Dispatchers.Unconfined))
            val terminar = CompletableDeferred<Unit>()
            var guardados = 0
            operaciones.ejecutar { guardados++; terminar.await() }
            assertTrue(operaciones.estado.value.enCurso)
            operaciones.ejecutar { guardados++ }
            assertEquals(1, guardados)
            terminar.complete(Unit)
            assertEquals(EstadoOperacion(), operaciones.estado.value)
            operaciones.ejecutar { guardados++ }
            assertEquals(2, guardados)
        } finally { trabajo.cancelAndJoin() }
    }

    @Test fun unErrorPermiteInformarYReintentar() = runBlocking {
        val trabajo = SupervisorJob()
        try {
            val operaciones = OperacionesPantalla(CoroutineScope(trabajo + Dispatchers.Unconfined))
            operaciones.ejecutar { throw java.io.IOException("No se pudo guardar") }
            assertFalse(operaciones.estado.value.enCurso)
            assertTrue(operaciones.estado.value.error.isNotBlank())
            operaciones.limpiarError()
            assertEquals(EstadoOperacion(), operaciones.estado.value)
            var guardado = false
            operaciones.ejecutar { guardado = true }
            assertTrue(guardado)
            assertEquals(EstadoOperacion(), operaciones.estado.value)
        } finally { trabajo.cancelAndJoin() }
    }

    @Test fun cancelarLiberaElIndicadorSinMostrarUnErrorDeGuardado() = runBlocking {
        val trabajo = SupervisorJob()
        val operaciones = OperacionesPantalla(CoroutineScope(trabajo + Dispatchers.Unconfined))
        operaciones.ejecutar { awaitCancellation() }
        assertTrue(operaciones.estado.value.enCurso)
        trabajo.cancelAndJoin()
        assertEquals(EstadoOperacion(), operaciones.estado.value)
    }
}
