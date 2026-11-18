package com.example.proyecto_acis.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.proyecto_acis.data.repository.OrdenesRepository
import com.example.proyecto_acis.data.sync.EstadoSync
import com.example.proyecto_acis.data.sync.SyncRepositorySimulado
import com.example.proyecto_acis.domain.model.Cliente
import com.example.proyecto_acis.domain.model.Equipo
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.EventoOt
import com.example.proyecto_acis.domain.model.Local
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoEvento
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.domain.service.DestinoMapa
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.domain.validation.DatosCliente
import com.example.proyecto_acis.domain.validation.DatosEquipo
import com.example.proyecto_acis.domain.validation.DatosLocal
import java.util.UUID
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

abstract class HistorialDbCasos {
    protected abstract val context: Context
    private lateinit var nombre: String
    private var helper: OrdenesDb? = null

    @Before fun preparar() {
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

    @Test fun migracionDesdeVersionCuatroConservaDatosYFotos() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
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
        assertEquals(9, db.readableDatabase.version)
        assertNotNull(conservada.equipoId)
        val equipo = db.leerCatastro().equipos.first { it.id == conservada.equipoId }
        assertEquals("Congelador", equipo.nombre)
    }

    @Test fun falloDelEventoRevierteLaCreacionYLaActualizacion() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
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

    @Test fun migracionDesdeVersionCincoConservaLosEventosExistentes() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
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
        assertEquals(9, db.readableDatabase.version)
    }

    @Test fun estadoAnteriorIncorrectoNoAgregaUnEvento() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        assertFalse(db.actualizar(orden().copy(estado = EstadoOt.ACEPTADA), EstadoOt.ASIGNADA,
            evento(tipo = TipoEvento.ACEPTACION)))
        assertEquals(EstadoOt.CREADA, db.leer().single().estado)
        assertEquals(1, db.leerEventos().size)
    }

    @Test fun guardaEventosYLosEliminaJuntoConLaOrden() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
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

    @Test fun recorridoCompletoConservaEventosYActorDelRechazo() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        val repo = OrdenesRepository(context, db, "${nombre}_preferencias").also { it.inicializar() }
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
        assertEquals(com.example.proyecto_acis.domain.service.TiempoVisita.fecha(cerrada.checkOut),
            repo.catastro.value.equipos.first { it.id == cerrada.equipoId }.ultimaMantencionMillis)
        val tipos = listOf(TipoEvento.CREACION, TipoEvento.ASIGNACION, TipoEvento.RECHAZO,
            TipoEvento.ASIGNACION, TipoEvento.ACEPTACION, TipoEvento.LLEGADA, TipoEvento.INFORME,
            TipoEvento.OBSERVACION, TipoEvento.RESPUESTA, TipoEvento.CONFORMIDAD)
        assertEquals(tipos, repo.eventos.value.map { it.tipo })
        val cantidad = repo.eventos.value.size
        assertFalse(repo.aceptar(id))
        assertEquals(cantidad, repo.eventos.value.size)
        db.close()
        val reabierto = OrdenesRepository(context, base(), "${nombre}_preferencias").also { it.inicializar() }
        assertEquals(EstadoOt.CERRADA, reabierto.ordenes.value.single().estado)
        assertEquals(tipos, reabierto.eventos.value.map { it.tipo })
    }

    @Test fun renombrarUnEquipoConservaLaReferenciaYLosTextosDeLaOt() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val anterior = db.leer().single()
        val equipo = db.leerCatastro().equipos.first { it.id == anterior.equipoId }
        assertTrue(db.guardarEquipo(com.example.proyecto_acis.domain.validation.DatosEquipo(
            equipo.id, equipo.localId, "Refrigerador actualizado", "Refrigeración", "Pasillo principal", true)))
        val conservada = db.leer().single()
        assertEquals(anterior.equipoId, conservada.equipoId)
        assertEquals("Refrigerador", conservada.equipo)
        assertTrue(db.actualizar(conservada.copy(estado = EstadoOt.ASIGNADA), EstadoOt.CREADA,
            evento(tipo = TipoEvento.ASIGNACION)))
        assertEquals(equipo.id, db.leer().single().equipoId)
        assertEquals("Refrigerador actualizado", db.leerCatastro().equipos.first { it.id == equipo.id }.nombre)
    }

    @Test fun cierreYMantencionSeReviertenSiFallaElEvento() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
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
        assertEquals(com.example.proyecto_acis.domain.service.TiempoVisita.fecha(actual.checkOut),
            db.leerCatastro().equipos.first { it.id == actual.equipoId }.ultimaMantencionMillis)
    }
    @Test fun clientesYLocalesPersistenSinCambiarLosTextosHistoricos() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        val repo = OrdenesRepository(context, db, "${nombre}_preferencias").also { it.inicializar() }
        assertTrue(repo.guardarCliente(com.example.proyecto_acis.domain.validation.DatosCliente(nombre = "Cliente nuevo")))
        val cliente = repo.catastro.value.clientes.first { it.nombre == "Cliente nuevo" }
        assertFalse(repo.guardarCliente(com.example.proyecto_acis.domain.validation.DatosCliente(nombre = " cliente nuevo ")))
        assertTrue(repo.guardarLocal(com.example.proyecto_acis.domain.validation.DatosLocal(
            clienteId = cliente.id, nombre = "Local nuevo", direccion = "Av. Prat 120", latitud = -36.82, longitud = -73.05)))
        val local = repo.catastro.value.locales.first { it.nombre == "Local nuevo" }
        val id = repo.crear("Local nuevo", "Equipo nuevo", "Revisar", "30/10/2026 10:00", TipoOt.PREVENTIVA, PrioridadOt.MEDIA)!!
        val original = repo.ordenes.value.first { it.id == id }
        assertTrue(repo.guardarCliente(com.example.proyecto_acis.domain.validation.DatosCliente(cliente.id, "Cliente actualizado")))
        assertTrue(repo.guardarLocal(com.example.proyecto_acis.domain.validation.DatosLocal(
            local.id, cliente.id, "Local actualizado", "Dirección nueva")))
        val actual = repo.ordenes.value.first { it.id == id }
        assertEquals(original.equipoId, actual.equipoId)
        assertEquals("Local nuevo", actual.local)
        assertEquals("Dirección nueva", com.example.proyecto_acis.domain.service.DestinoMapa.consulta(actual, repo.catastro.value))
        assertNull(repo.catastro.value.locales.first { it.id == local.id }.latitud)
        db.close()
        val guardado = base().leerCatastro()
        assertEquals("Cliente actualizado", guardado.clientes.first { it.id == cliente.id }.nombre)
        assertEquals("Local actualizado", guardado.locales.first { it.id == local.id }.nombre)
        assertNull(guardado.locales.first { it.id == local.id }.longitud)
    }

    @Test fun acuerdoSlaConservaPlazosClienteYProgramacionAlEditar() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        val repo = OrdenesRepository(context, db, "${nombre}_preferencias").also { it.inicializar() }
        val equipo = repo.catastro.value.equipos.first()
        val local = repo.catastro.value.locales.first { it.id == equipo.localId }
        val cliente = repo.catastro.value.clientes.first { it.id == local.clienteId }
        assertTrue(repo.guardarCliente(com.example.proyecto_acis.domain.validation.DatosCliente(cliente.id, cliente.nombre, "4", "24")))
        val id = repo.crear(local.nombre, equipo.nombre, "Revisar", "30/10/2026 10:00", TipoOt.PREVENTIVA, PrioridadOt.MEDIA)!!
        val creada = repo.ordenes.value.first { it.id == id }
        val acuerdo = creada.acuerdoSla!!
        assertEquals(4, acuerdo.horasLlegada)
        assertEquals(cliente.id, acuerdo.clienteId)
        assertTrue(repo.guardarCliente(com.example.proyecto_acis.domain.validation.DatosCliente(cliente.id, "Cliente renombrado", "8", "48")))
        assertTrue(db.actualizar(creada.copy(fechaProgramada = "31/10/2026 12:00", acuerdoSla = null), EstadoOt.CREADA))
        db.close()
        assertEquals(acuerdo, base().leer().first { it.id == id }.acuerdoSla)
    }

    @Test fun migracionDesdeSeisConservaOrdenesSinInventarAcuerdos() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val archivo = context.getDatabasePath(nombre)
        archivo.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { anterior ->
            com.example.proyecto_acis.data.local.migration.CatastroAnterior.crear(anterior)
            val columnas = listOf("local", "equipo", "descripcion", "fechaProgramada", "tipo", "prioridad", "estado",
                "tecnico", "checkIn", "ubicacionCheckIn", "checkOut", "diagnostico", "actividad", "materiales", "observacionCliente",
                "respuestaTecnico", "fechaConformidad", "fotoEvidencia")
            anterior.execSQL("CREATE TABLE ordenes (id TEXT PRIMARY KEY, " + columnas.joinToString { "$it TEXT NOT NULL DEFAULT ''" } +
                ", ultimaActualizacion INTEGER NOT NULL DEFAULT 0, equipoId TEXT REFERENCES equipos(id))")
            anterior.execSQL("CREATE TABLE eventos_ot (id INTEGER PRIMARY KEY AUTOINCREMENT, otId TEXT NOT NULL REFERENCES ordenes(id) ON DELETE CASCADE, tipo TEXT NOT NULL, rol TEXT NOT NULL, actor TEXT NOT NULL, fechaMillis INTEGER NOT NULL, detalle TEXT NOT NULL)")
            anterior.insertOrThrow("ordenes", null, ContentValues().apply {
                put("id", "OT-ANTERIOR"); put("estado", EstadoOt.EN_VISITA.name); put("fotoEvidencia", "antes.jpg")
            })
            anterior.version = 6
        }
        val db = base()
        assertEquals(9, db.readableDatabase.version)
        assertEquals("antes.jpg", db.leer().single().fotoEvidencia)
        assertNull(db.leer().single().acuerdoSla)
        assertEquals(4, db.leerCatastro().clientes.first { it.id == "cliente-demo-comercial" }.horasLlegada)
        assertEquals(48, db.leerCatastro().clientes.first { it.id == "cliente-demo-servicios" }.horasResolucion)
    }

    @Test fun migracionDesdeSieteConservaAcuerdosYMarcaRegistrosLocales() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        val catastro = db.leerCatastro()
        val equipo = catastro.equipos.first()
        val local = catastro.locales.first { it.id == equipo.localId }
        assertTrue(db.insertar(orden().copy(local = local.nombre, equipo = equipo.nombre), evento()))
        val anterior = db.leer().single()
        assertNotNull(anterior.acuerdoSla)
        db.writableDatabase.execSQL("DROP TABLE sync_ot")
        db.writableDatabase.version = 7
        db.close()
        val nueva = base()
        assertEquals(9, nueva.readableDatabase.version)
        assertEquals(anterior, nueva.leer().single())
        assertEquals(EstadoSync.LOCAL, nueva.leerSync().single().estado)
        assertEquals(0L, nueva.leerSync().single().revision)
    }
    @Test fun simulacionPermiteErrorReintentoYDejaCambiosNuevosPendientes() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val sync = SyncRepositorySimulado(db) { }
        sync.recargar()
        assertEquals(EstadoSync.PENDIENTE, sync.registros.value.single().estado)
        assertEquals(1, sync.simular(setOf("OT-1"), true).errores)
        assertEquals(EstadoSync.ERROR, sync.registros.value.single().estado)
        assertEquals(1, sync.simular(setOf("OT-1"), false).enviados)
        assertEquals(EstadoSync.ENVIADO, sync.registros.value.single().estado)
        assertEquals(0, sync.simular(setOf("OT-1"), false).enviados)
        assertEquals(orden().descripcion, db.leer().single().descripcion)
        assertTrue(db.actualizar(db.leer().single().copy(descripcion = "Nueva actividad"), EstadoOt.CREADA))
        db.close()
        val guardada = base().leerSync().single()
        assertEquals(EstadoSync.PENDIENTE, guardada.estado)
        assertEquals(2L, guardada.revision)
    }
    @Test fun editarDuranteElEnvioNoMarcaLaRevisionNuevaComoEnviada() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val sync = SyncRepositorySimulado(db) {
            assertTrue(db.actualizar(db.leer().single().copy(descripcion = "Cambio durante simulación"), EstadoOt.CREADA))
        }
        val resultado = sync.simular(setOf("OT-1"), false)
        assertEquals(0, resultado.enviados)
        assertEquals(1, resultado.modificados)
        assertEquals(EstadoSync.PENDIENTE, sync.registros.value.single().estado)
        assertEquals(2L, sync.registros.value.single().revision)
    }
    @Test fun borrarDuranteSimulacionNoRecreaLaOrdenNiSuEstado() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val sync = SyncRepositorySimulado(db) { assertTrue(db.eliminar("OT-1", EstadoOt.CREADA)) }
        assertEquals(1, sync.simular(setOf("OT-1"), false).modificados)
        assertTrue(db.leer().isEmpty())
        assertTrue(db.leerSync().isEmpty())
    }
    @Test fun unEventoFallidoNoCambiaLaRevisionDeSincronizacion() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        assertTrue(db.insertar(orden(), evento()))
        val anterior = db.leerSync().single()
        db.writableDatabase.execSQL("CREATE TRIGGER fallo_sync BEFORE INSERT ON eventos_ot BEGIN SELECT RAISE(ABORT, 'fallo'); END")
        assertFalse(db.actualizar(db.leer().single().copy(estado = EstadoOt.ASIGNADA), EstadoOt.CREADA, evento(tipo = TipoEvento.ASIGNACION)))
        assertEquals(anterior, db.leerSync().single())
        assertEquals(EstadoOt.CREADA, db.leer().single().estado)
    }

    @Test fun migracionOchoARoomConservaTodasLasTablasYLosArchivos() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val foto = java.io.File(context.filesDir, "${nombre}_evidencia.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val tablas = listOf("clientes", "locales", "equipos", "ordenes", "eventos_ot", "sync_ot")
        fun captura(consulta: (String) -> android.database.Cursor) = tablas.associateWith { tabla ->
            consulta("SELECT * FROM $tabla ORDER BY ${if (tabla == "sync_ot") "otId" else "id"}").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.columnNames.associateWith { campo ->
                    cursor.getColumnIndexOrThrow(campo).let { if (cursor.isNull(it)) null else cursor.getString(it) }
                }) }
            }
        }
        try {
            val anterior = com.example.proyecto_acis.data.local.migration.SQLiteAnterior(context, nombre)
            val esperadas = anterior.use { helper ->
                val db = helper.writableDatabase
                db.execSQL("UPDATE locales SET direccion = 'Direccion guardada', latitud = -36.82, longitud = -73.05 WHERE id = 'local-demo-0'")
                val valores = ContentValues()
                db.rawQuery("PRAGMA table_info(ordenes)", null).use { columnas ->
                    while (columnas.moveToNext()) {
                        val campo = columnas.getString(columnas.getColumnIndexOrThrow("name"))
                        if (columnas.getInt(columnas.getColumnIndexOrThrow("notnull")) == 1) {
                            if (columnas.getString(columnas.getColumnIndexOrThrow("type")) == "INTEGER") valores.put(campo, 0L)
                            else valores.put(campo, "")
                        }
                    }
                }
                valores.apply {
                    put("id", "OT-800"); put("local", "Texto historico del local"); put("equipo", "Equipo historico")
                    put("descripcion", "Trabajo guardado"); put("fechaProgramada", "20/11/2026 09:00")
                    put("tipo", TipoOt.CORRECTIVA.name); put("prioridad", PrioridadOt.ALTA.name)
                    put("estado", EstadoOt.OBSERVADA.name); put("tecnico", "Brian Bravo")
                    put("checkIn", "20/11/2026 09:00"); put("checkOut", "20/11/2026 10:00")
                    put("ubicacionCheckIn", "-36.82, -73.05"); put("diagnostico", "Filtro sucio")
                    put("actividad", "Limpieza"); put("materiales", "Pano 1 unidad")
                    put("observacionCliente", "Revisar otra vez"); put("respuestaTecnico", "Respuesta previa")
                    put("fotoEvidencia", foto.name); put("ultimaActualizacion", 123456L)
                    put("equipoId", "equipo-demo-0-0"); put("slaClienteId", "cliente-demo-comercial")
                    put("slaClienteNombre", "Nombre original"); put("slaHorasLlegada", 4)
                    put("slaHorasResolucion", 24); put("slaInicio", 987654L)
                }
                db.insertOrThrow("ordenes", null, valores)
                db.execSQL("INSERT INTO eventos_ot (id, otId, tipo, rol, actor, fechaMillis, detalle) VALUES (71, 'OT-800', 'OBSERVACION', 'CLIENTE', 'Cliente demo', 123456, 'Revisar otra vez')")
                db.execSQL("UPDATE sqlite_sequence SET seq = 300 WHERE name = 'eventos_ot'")
                db.execSQL("INSERT INTO sync_ot (otId, revision, estado, intentoMillis) VALUES ('OT-800', 17, 'ERROR', 777)")
                captura { db.rawQuery(it, null) }
            }
            val migrada = base()
            assertEquals(9, migrada.readableDatabase.version)
            assertEquals(esperadas, captura { migrada.readableDatabase.query(it) })
            assertEquals(byteArrayOf(1, 2, 3).toList(), foto.readBytes().toList())
            assertEquals("Nombre original", migrada.leer().single().acuerdoSla?.clienteNombre)
            assertEquals(17L, migrada.leerSync().single().revision)
            assertTrue(migrada.actualizar(migrada.leer().single(), EstadoOt.OBSERVADA,
                EventoOt(otId = "OT-800", tipo = TipoEvento.RESPUESTA, actor = "Brian Bravo", fechaMillis = 123457L)))
            assertTrue(migrada.leerEventos().last().id > 300L)
        } finally { foto.delete() }
    }

    @Test fun falloDeMigracionNoBorraNiReemplazaLaBaseAnterior() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        com.example.proyecto_acis.data.local.migration.SQLiteAnterior(context, nombre).use { helper ->
            helper.writableDatabase.execSQL("INSERT INTO clientes (id, nombre, esDemo) VALUES (NULL, 'Registro antiguo incompleto', 0)")
        }
        val fallida = base()
        assertTrue(runCatching { fallida.leer() }.isFailure)
        fallida.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(nombre).path, null, SQLiteDatabase.OPEN_READONLY).use { anterior ->
            assertEquals(8, anterior.version)
            anterior.rawQuery("SELECT nombre FROM clientes WHERE id IS NULL", null).use {
                assertTrue(it.moveToFirst())
                assertEquals("Registro antiguo incompleto", it.getString(0))
            }
        }
    }

    @Test fun creacionesConcurrentesConservanNumerosDistintosYEventos() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        val db = base()
        val repo = OrdenesRepository(context, db, "${nombre}_preferencias")
        repo.inicializar()
        val ids = kotlinx.coroutines.coroutineScope {
            (1..8).map { numero -> async {
                repo.crear("Local concurrente", "Equipo", "Trabajo $numero", "20/11/2026 09:00", TipoOt.CORRECTIVA, PrioridadOt.MEDIA)
            } }.map { it.await() }
        }
        assertTrue(ids.all { it != null })
        assertEquals(8, ids.toSet().size)
        assertEquals(8, db.leer().size)
        assertEquals(8, db.leerEventos().size)
        assertTrue(db.leerSync().all { it.revision == 1L })
    }

}
