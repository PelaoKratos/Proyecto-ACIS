package com.example.proyecto_acis.data.repository

import android.app.Application
import com.example.proyecto_acis.data.local.OrdenesDb
import com.example.proyecto_acis.domain.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RecorridosRepositoryTest {
    @Test fun unInformeFallidoConservaElBorradorYPermiteReintentar() = runBlocking(Dispatchers.IO) {
        conBase { db, repo, _ ->
            val id = crear(repo)
            assertTrue(repo.asignar(id, "Brian Bravo"))
            assertTrue(repo.aceptar(id))
            assertTrue(repo.iniciarVisita(id, null))
            assertTrue(repo.guardarBorrador(id, "Filtro sucio", "Limpieza", ""))
            val orden = db.leer().single()
            val eventos = db.leerEventos()
            val revisiones = db.leerSync()
            db.writableDatabase.execSQL("CREATE TRIGGER fallo_informe BEFORE INSERT ON eventos_ot WHEN NEW.tipo = 'INFORME' BEGIN SELECT RAISE(ABORT, 'fallo'); END")
            assertFalse(repo.enviarInforme(id, "Diagnóstico nuevo", "Actividad nueva", ""))
            assertEquals(orden, db.leer().single())
            assertEquals(eventos, db.leerEventos())
            assertEquals(revisiones, db.leerSync())
            assertEquals(orden, repo.ordenes.value.single())
            db.writableDatabase.execSQL("DROP TRIGGER fallo_informe")
            assertTrue(repo.enviarInforme(id, "Diagnóstico nuevo", "Actividad nueva", ""))
            assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, repo.ordenes.value.single().estado)
            assertEquals(1, db.leerEventos().count { it.tipo == TipoEvento.INFORME })
        }
    }

    @Test fun aceptarDosVecesAlMismoTiempoNoDuplicaLaLlegadaNiLosEventos() = runBlocking(Dispatchers.IO) {
        conBase { db, repo, _ ->
            val id = crear(repo)
            assertTrue(repo.asignar(id, "Brian Bravo"))
            val aceptadas = coroutineScope { List(2) { async { repo.aceptar(id) } }.awaitAll() }
            assertEquals(1, aceptadas.count { it })
            val llegadas = coroutineScope { List(2) { async { repo.iniciarVisita(id, null) } }.awaitAll() }
            assertEquals(1, llegadas.count { it })
            assertEquals(1, db.leerEventos().count { it.tipo == TipoEvento.ACEPTACION })
            assertEquals(1, db.leerEventos().count { it.tipo == TipoEvento.LLEGADA })
            assertEquals(EstadoOt.EN_VISITA, db.leer().single().estado)
        }
    }

    @Test fun elRecorridoCerradoSeConservaAlCerrarYReabrirLaBase() = runBlocking(Dispatchers.IO) {
        conBase { db, repo, nombre ->
            val id = crear(repo)
            assertTrue(repo.asignar(id, "Brian Bravo"))
            assertTrue(repo.aceptar(id))
            assertTrue(repo.iniciarVisita(id, "-36.82, -73.05"))
            assertTrue(repo.enviarInforme(id, "Falla eléctrica", "Cambio de ampolleta", "Ampolleta · 1"))
            assertTrue(repo.observar(id, "Revisar interruptor"))
            assertTrue(repo.responderObservacion(id, "Interruptor operativo"))
            assertTrue(repo.aprobar(id))
            val orden = db.leer().single()
            val eventos = db.leerEventos()
            val revisiones = db.leerSync()
            db.close()
            OrdenesDb(RuntimeEnvironment.getApplication(), nombre).use { nueva ->
                assertEquals(orden, nueva.leer().single())
                assertEquals(eventos, nueva.leerEventos())
                assertEquals(revisiones, nueva.leerSync())
                val nuevoRepo = OrdenesRepository(RuntimeEnvironment.getApplication(), nueva, "${nombre}_prefs")
                nuevoRepo.inicializar()
                assertEquals(EstadoOt.CERRADA, nuevoRepo.ordenes.value.single().estado)
                assertFalse(nuevoRepo.eliminar(id))
                assertFalse(nuevoRepo.enviarInforme(id, "Cambio", "Cambio", ""))
                assertEquals(eventos, nueva.leerEventos())
            }
        }
    }

    private suspend fun crear(repo: OrdenesRepository) = requireNotNull(repo.crear("Local", "Equipo", "Revisar luz",
        "20/11/2030 09:00", TipoOt.CORRECTIVA, PrioridadOt.MEDIA))

    private suspend fun conBase(accion: suspend (OrdenesDb, OrdenesRepository, String) -> Unit) {
        val context = RuntimeEnvironment.getApplication()
        val nombre = "recorrido_${UUID.randomUUID()}.db"
        val db = OrdenesDb(context, nombre)
        try {
            val repo = OrdenesRepository(context, db, "${nombre}_prefs")
            repo.inicializar()
            accion(db, repo, nombre)
        } finally {
            db.close()
            context.deleteDatabase(nombre)
        }
    }
}
