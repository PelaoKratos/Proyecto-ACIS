package com.example.proyecto_acis.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HistorialDbTest {
    private lateinit var context: Context
    private lateinit var nombre: String
    private var helper: OrdenesDb? = null

    @Before fun preparar() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        nombre = "acis_prueba_historial_${UUID.randomUUID()}.db"
    }

    @After fun limpiar() {
        helper?.close()
        context.deleteDatabase(nombre)
        context.deleteSharedPreferences("${nombre}_preferencias")
    }

    private fun base(): OrdenesDb = OrdenesDb(context, nombre).also { helper = it }

    private fun orden(id: String = "OT-1") = OrdenTrabajo(
        id = id, local = "Local de prueba", equipo = "Refrigerador",
        descripcion = "Revisar temperatura", fechaProgramada = "29/10/2026 10:00",
        tipo = TipoOt.PREVENTIVA, prioridad = PrioridadOt.MEDIA, ultimaActualizacion = 1000,
    )

    private fun evento(id: String = "OT-1", tipo: TipoEvento = TipoEvento.CREACION) = EventoOt(
        otId = id, tipo = tipo, actor = "Planificador demo", fechaMillis = 1000,
    )

    @Test fun migracionDesdeVersionCuatroConservaDatosYFotos() {
        val archivo = context.getDatabasePath(nombre)
        archivo.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { antigua ->
            val columnas = listOf("local", "equipo", "descripcion", "fechaProgramada", "tipo", "prioridad",
                "estado", "tecnico", "checkIn", "ubicacionCheckIn", "checkOut", "diagnostico", "actividad",
                "materiales", "observacionCliente", "respuestaTecnico", "fechaConformidad", "fotoEvidencia")
            antigua.execSQL("CREATE TABLE ordenes (id TEXT PRIMARY KEY, " +
                columnas.joinToString { "$it TEXT NOT NULL DEFAULT ''" } + ", ultimaActualizacion INTEGER NOT NULL DEFAULT 0)")
            antigua.insertOrThrow("ordenes", null, ContentValues().apply {
                put("id", "OT-ANTERIOR")
                put("local", "Local anterior")
                put("equipo", "Congelador")
                put("estado", EstadoOt.EN_VISITA.name)
                put("tecnico", "Brian Bravo")
                put("diagnostico", "Filtro sucio")
                put("checkIn", "29/10/2026 10:00")
                put("ubicacionCheckIn", "-36.82, -73.05")
                put("fotoEvidencia", "antes.jpg\ndespues.jpg")
            })
            antigua.version = 4
        }
        val db = base()
        val conservada = db.leer().single()
        assertEquals("OT-ANTERIOR", conservada.id)
        assertEquals("Local anterior", conservada.local)
        assertEquals("Brian Bravo", conservada.tecnico)
        assertEquals("Filtro sucio", conservada.diagnostico)
        assertEquals("-36.82, -73.05", conservada.ubicacionCheckIn)
        assertEquals(listOf("antes.jpg", "despues.jpg"), conservada.fotosEvidencia)
        assertEquals(EstadoOt.EN_VISITA, conservada.estado)
        assertEquals(TipoEvento.REGISTRO_PREVIO, db.leerEventos().single().tipo)
        assertTrue(db.leerEventos().single().detalle.contains("29/10/2026 10:00"))
        assertEquals(6, db.readableDatabase.version)
        assertNotNull(conservada.equipoId)
        val equipo = db.leerCatastro().equipos.first { it.id == conservada.equipoId }
        assertEquals("Congelador", equipo.nombre)
    }

    @Test fun falloDelEventoRevierteLaCreacionYLaActualizacion() {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        db.writableDatabase.execSQL("CREATE TRIGGER fallo_evento BEFORE INSERT ON eventos_ot BEGIN SELECT RAISE(ABORT, 'fallo de prueba'); END")
        assertFalse(db.actualizar(orden().copy(estado = EstadoOt.ASIGNADA), EstadoOt.CREADA,
            evento(tipo = TipoEvento.ASIGNACION)))
        assertEquals(EstadoOt.CREADA, db.leer().single().estado)
        assertEquals(1, db.leerEventos().size)
        assertFalse(db.insertar(orden("OT-2"), evento("OT-2")))
        assertEquals(1, db.leer().size)
    }

    @Test fun migracionDesdeVersionCincoConservaLosEventosExistentes() {
        val archivo = context.getDatabasePath(nombre)
        archivo.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { antigua ->
            val columnas = listOf("local", "equipo", "descripcion", "fechaProgramada", "tipo", "prioridad",
                "estado", "tecnico", "checkIn", "ubicacionCheckIn", "checkOut", "diagnostico", "actividad",
                "materiales", "observacionCliente", "respuestaTecnico", "fechaConformidad", "fotoEvidencia")
            antigua.execSQL("CREATE TABLE ordenes (id TEXT PRIMARY KEY, " +
                columnas.joinToString { "$it TEXT NOT NULL DEFAULT ''" } + ", ultimaActualizacion INTEGER NOT NULL DEFAULT 0)")
            antigua.execSQL("CREATE TABLE eventos_ot (id INTEGER PRIMARY KEY AUTOINCREMENT, otId TEXT NOT NULL REFERENCES ordenes(id) ON DELETE CASCADE, tipo TEXT NOT NULL, rol TEXT NOT NULL, actor TEXT NOT NULL, fechaMillis INTEGER NOT NULL, detalle TEXT NOT NULL)")
            antigua.insertOrThrow("ordenes", null, ContentValues().apply {
                put("id", "OT-V5"); put("local", "Local existente"); put("equipo", "Equipo existente")
                put("estado", EstadoOt.CREADA.name)
            })
            antigua.insertOrThrow("eventos_ot", null, ContentValues().apply {
                put("id", 42); put("otId", "OT-V5"); put("tipo", TipoEvento.CREACION.name)
                put("rol", Perfil.PLANIFICADOR.name); put("actor", "Planificador demo")
                put("fechaMillis", 1000); put("detalle", "Evento conservado")
            })
            antigua.version = 5
        }
        val db = base()
        assertNotNull(db.leer().single().equipoId)
        val evento = db.leerEventos().single()
        assertEquals(42L, evento.id)
        assertEquals("Evento conservado", evento.detalle)
        assertEquals(TipoEvento.CREACION, evento.tipo)
        assertEquals(6, db.readableDatabase.version)
    }

    @Test fun estadoAnteriorIncorrectoNoAgregaUnEvento() {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        assertFalse(db.actualizar(orden().copy(estado = EstadoOt.ACEPTADA), EstadoOt.ASIGNADA,
            evento(tipo = TipoEvento.ACEPTACION)))
        assertEquals(EstadoOt.CREADA, db.leer().single().estado)
        assertEquals(1, db.leerEventos().size)
    }

    @Test fun guardaEventosYLosEliminaJuntoConLaOrden() {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        assertTrue(db.actualizar(orden().copy(estado = EstadoOt.ASIGNADA), EstadoOt.CREADA,
            evento(tipo = TipoEvento.ASIGNACION)))
        db.close()
        val reabierta = base()
        assertEquals(listOf(TipoEvento.CREACION, TipoEvento.ASIGNACION), reabierta.leerEventos().map { it.tipo })
        assertFalse(reabierta.eliminar("OT-1", EstadoOt.ACEPTADA))
        assertTrue(reabierta.eliminar("OT-1", EstadoOt.ASIGNADA))
        assertTrue(reabierta.leerEventos().isEmpty())
    }

    @Test fun recorridoCompletoConservaEventosYActorDelRechazo() {
        val db = base()
        val repo = OrdenesRepository(context, db, "${nombre}_preferencias")
        val id = repo.crear("Local de prueba", "Congelador", "Revisar temperatura",
            "29/10/2026 10:00", TipoOt.PREVENTIVA, PrioridadOt.MEDIA)!!
        assertTrue(repo.asignar(id, "Brian Bravo"))
        assertTrue(repo.rechazar(id))
        val rechazo = repo.eventos.value.last()
        assertEquals(TipoEvento.RECHAZO, rechazo.tipo)
        assertEquals("Brian Bravo", rechazo.actor)
        assertEquals("", repo.ordenes.value.single().tecnico)
        assertTrue(repo.asignar(id, "Brian Bravo"))
        assertTrue(repo.aceptar(id))
        assertTrue(repo.iniciarVisita(id, null))
        assertTrue(repo.guardarBorrador(id, "Filtro sucio", "Limpieza", ""))
        assertTrue(repo.enviarInforme(id, "Filtro sucio", "Limpieza de filtros", ""))
        assertTrue(repo.observar(id, "Revisar la temperatura nuevamente"))
        assertTrue(repo.responderObservacion(id, "Se verificó el funcionamiento"))
        assertTrue(repo.aprobar(id))
        assertEquals(EstadoOt.CERRADA, repo.ordenes.value.single().estado)
        val cerrada = repo.ordenes.value.single()
        assertNotNull(cerrada.equipoId)
        assertEquals(com.example.proyecto_acis.domain.TiempoVisita.fecha(cerrada.checkOut),
            repo.catastro.value.equipos.first { it.id == cerrada.equipoId }.ultimaMantencionMillis)
        val tipos = listOf(TipoEvento.CREACION, TipoEvento.ASIGNACION, TipoEvento.RECHAZO,
            TipoEvento.ASIGNACION, TipoEvento.ACEPTACION, TipoEvento.LLEGADA, TipoEvento.INFORME,
            TipoEvento.OBSERVACION, TipoEvento.RESPUESTA, TipoEvento.CONFORMIDAD)
        assertEquals(tipos, repo.eventos.value.map { it.tipo })
        val cantidad = repo.eventos.value.size
        assertFalse(repo.aceptar(id))
        assertEquals(cantidad, repo.eventos.value.size)
        db.close()
        val reabierto = OrdenesRepository(context, base(), "${nombre}_preferencias")
        assertEquals(EstadoOt.CERRADA, reabierto.ordenes.value.single().estado)
        assertEquals(tipos, reabierto.eventos.value.map { it.tipo })
    }

    @Test fun renombrarUnEquipoConservaLaReferenciaYLosTextosDeLaOt() {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val anterior = db.leer().single()
        val equipo = db.leerCatastro().equipos.first { it.id == anterior.equipoId }
        assertTrue(db.guardarEquipo(com.example.proyecto_acis.domain.DatosEquipo(
            equipo.id, equipo.localId, "Refrigerador actualizado", "Refrigeración", "Pasillo principal", true)))
        val conservada = db.leer().single()
        assertEquals(anterior.equipoId, conservada.equipoId)
        assertEquals("Refrigerador", conservada.equipo)
        assertTrue(db.actualizar(conservada.copy(estado = EstadoOt.ASIGNADA), EstadoOt.CREADA,
            evento(tipo = TipoEvento.ASIGNACION)))
        assertEquals(equipo.id, db.leer().single().equipoId)
        assertEquals("Refrigerador actualizado", db.leerCatastro().equipos.first { it.id == equipo.id }.nombre)
    }

    @Test fun cierreYMantencionSeReviertenSiFallaElEvento() {
        val db = base()
        val pendiente = orden().copy(estado = EstadoOt.PENDIENTE_CONFORMIDAD,
            checkIn = "29/10/2026 10:00", checkOut = "29/10/2026 10:45")
        assertTrue(db.insertar(pendiente, evento(tipo = TipoEvento.INFORME)))
        val actual = db.leer().single()
        db.writableDatabase.execSQL("CREATE TRIGGER fallo_evento BEFORE INSERT ON eventos_ot BEGIN SELECT RAISE(ABORT, 'fallo de prueba'); END")
        assertFalse(db.actualizar(actual.copy(estado = EstadoOt.CERRADA), EstadoOt.PENDIENTE_CONFORMIDAD,
            evento(tipo = TipoEvento.CONFORMIDAD)))
        assertNull(db.leerCatastro().equipos.first { it.id == actual.equipoId }.ultimaMantencionMillis)
        assertEquals(EstadoOt.PENDIENTE_CONFORMIDAD, db.leer().single().estado)
        db.writableDatabase.execSQL("DROP TRIGGER fallo_evento")
        assertTrue(db.actualizar(actual.copy(estado = EstadoOt.CERRADA), EstadoOt.PENDIENTE_CONFORMIDAD,
            evento(tipo = TipoEvento.CONFORMIDAD)))
        assertEquals(com.example.proyecto_acis.domain.TiempoVisita.fecha(actual.checkOut),
            db.leerCatastro().equipos.first { it.id == actual.equipoId }.ultimaMantencionMillis)
    }
}
