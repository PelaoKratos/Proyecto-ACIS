package com.example.proyecto_acis.data

import com.example.proyecto_acis.domain.DatosCliente
import com.example.proyecto_acis.domain.DatosLocal
import com.example.proyecto_acis.domain.DatosEquipo
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrdenesDb(context: Context, nombre: String = "acis_ordenes.db") : SQLiteOpenHelper(context, nombre, null, 8) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        CatastroSql.crear(db)
        db.execSQL("""
            CREATE TABLE ordenes (
                id TEXT PRIMARY KEY,
                local TEXT NOT NULL,
                equipo TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                fechaProgramada TEXT NOT NULL,
                tipo TEXT NOT NULL,
                prioridad TEXT NOT NULL,
                estado TEXT NOT NULL,
                tecnico TEXT NOT NULL,
                checkIn TEXT NOT NULL,
                ubicacionCheckIn TEXT NOT NULL DEFAULT '',
                checkOut TEXT NOT NULL,
                diagnostico TEXT NOT NULL,
                actividad TEXT NOT NULL,
                materiales TEXT NOT NULL,
                observacionCliente TEXT NOT NULL,
                respuestaTecnico TEXT NOT NULL,
                fechaConformidad TEXT NOT NULL,
                fotoEvidencia TEXT NOT NULL DEFAULT '',
                ultimaActualizacion INTEGER NOT NULL DEFAULT 0,
                equipoId TEXT REFERENCES equipos(id)
            )
        """.trimIndent())
        crearTablaEventos(db)
        agregarSla(db)
        crearSync(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN fotoEvidencia TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN ultimaActualizacion INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN ubicacionCheckIn TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 5) {
            crearTablaEventos(db)
            val fecha = System.currentTimeMillis()
            db.query("ordenes", arrayOf("id", "estado", "checkIn", "checkOut", "fechaConformidad"), null, null, null, null, "id ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    insertarEvento(db, EventoOt(otId = cursor.valor("id"), tipo = TipoEvento.REGISTRO_PREVIO,
                        actor = "Sistema local", fechaMillis = fecha,
                        detalle = detalleRegistroPrevio(cursor.valor("estado"), cursor.valor("checkIn"),
                            cursor.valor("checkOut"), cursor.valor("fechaConformidad"))))
                }
            }
        }
        if (oldVersion < 6) {
            CatastroSql.crear(db)
            db.execSQL("ALTER TABLE ordenes ADD COLUMN equipoId TEXT REFERENCES equipos(id)")
            CatastroSql.vincularOrdenes(db)
        }
        if (oldVersion < 7) agregarSla(db)
        if (oldVersion < 8) crearSync(db)
    }

    fun leer(): List<OrdenTrabajo> = readableDatabase.query(
        "ordenes", null, null, null, null, null, "id ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(OrdenTrabajo(
                    id = cursor.valor("id"),
                    local = cursor.valor("local"),
                    equipo = cursor.valor("equipo"),
                    descripcion = cursor.valor("descripcion"),
                    fechaProgramada = cursor.valor("fechaProgramada"),
                    tipo = TipoOt.entries.firstOrNull { it.name == cursor.valor("tipo") }
                        ?: TipoOt.SIN_ESPECIFICAR,
                    prioridad = PrioridadOt.entries.firstOrNull { it.name == cursor.valor("prioridad") }
                        ?: PrioridadOt.SIN_ESPECIFICAR,
                    estado = EstadoOt.entries.firstOrNull { it.name == cursor.valor("estado") }
                        ?: EstadoOt.CREADA,
                    tecnico = cursor.valor("tecnico"),
                    checkIn = cursor.valor("checkIn"),
                    ubicacionCheckIn = cursor.valor("ubicacionCheckIn"),
                    checkOut = cursor.valor("checkOut"),
                    diagnostico = cursor.valor("diagnostico"),
                    actividad = cursor.valor("actividad"),
                    materiales = cursor.valor("materiales"),
                    observacionCliente = cursor.valor("observacionCliente"),
                    respuestaTecnico = cursor.valor("respuestaTecnico"),
                    fechaConformidad = cursor.valor("fechaConformidad"),
                    fotoEvidencia = cursor.valor("fotoEvidencia"),
                    ultimaActualizacion = cursor.getLong(cursor.getColumnIndexOrThrow("ultimaActualizacion")),
                    acuerdoSla = leerAcuerdo(cursor),
                    equipoId = cursor.getString(cursor.getColumnIndexOrThrow("equipoId")),
                ))
            }
        }
    }

    fun leerEventos(): List<EventoOt> = readableDatabase.query(
        "eventos_ot", null, null, null, null, null, "fechaMillis ASC, id ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(EventoOt(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                otId = cursor.valor("otId"),
                tipo = TipoEvento.entries.firstOrNull { it.name == cursor.valor("tipo") } ?: TipoEvento.REGISTRO_PREVIO,
                actor = cursor.valor("actor"), rolActor = cursor.valor("rol"),
                fechaMillis = cursor.getLong(cursor.getColumnIndexOrThrow("fechaMillis")),
                detalle = cursor.valor("detalle"),
            ))
        }
    }

    fun insertar(orden: OrdenTrabajo, evento: EventoOt? = null): Boolean {
        if (evento != null && evento.otId != orden.id) return false
        return transaccion { db ->
            val equipo = vincularEquipo(db, orden)
            val vinculada = if (evento?.tipo == TipoEvento.CREACION)
                equipo.copy(acuerdoSla = CatastroSql.acuerdo(db, equipo)) else equipo
            db.insertOrThrow("ordenes", null, valores(vinculada))
            marcarPendiente(db, vinculada.id)
            if (vinculada.estado == EstadoOt.CERRADA) vinculada.equipoId?.let { CatastroSql.actualizarMantencion(db, it, vinculada.checkOut) }
            if (evento != null) insertarEvento(db, evento)
            true
        }
    }

    fun actualizar(orden: OrdenTrabajo, estadoAnterior: EstadoOt, evento: EventoOt? = null): Boolean {
        if (evento != null && evento.otId != orden.id) return false
        return transaccion { db ->
            val vinculada = vincularEquipo(db, orden)
            if (db.update("ordenes", valores(vinculada).apply {
                listOf("slaClienteId", "slaClienteNombre", "slaHorasLlegada", "slaHorasResolucion", "slaInicio").forEach { remove(it) }
            }, "id = ? AND estado = ?", arrayOf(orden.id, estadoAnterior.name)) != 1) false
            else {
                marcarPendiente(db, vinculada.id)
                if (vinculada.estado == EstadoOt.CERRADA) vinculada.equipoId?.let { CatastroSql.actualizarMantencion(db, it, vinculada.checkOut) }
                if (evento != null) insertarEvento(db, evento)
                true
            }
        }
    }

    fun guardarTodas(ordenes: List<OrdenTrabajo>): Boolean = transaccion { db ->
        ordenes.forEach { orden ->
            val vinculada = vincularEquipo(db, orden)
            db.insertOrThrow("ordenes", null, valores(vinculada))
            db.execSQL("INSERT INTO sync_ot (otId, revision, estado) VALUES (?, 0, 'LOCAL')", arrayOf(vinculada.id))
            if (vinculada.estado == EstadoOt.CERRADA) vinculada.equipoId?.let { CatastroSql.actualizarMantencion(db, it, vinculada.checkOut) }
            insertarEvento(db, EventoOt(otId = orden.id, tipo = TipoEvento.REGISTRO_PREVIO,
                actor = "Sistema local", fechaMillis = System.currentTimeMillis(),
                detalle = detalleRegistroPrevio(orden.estado.etiqueta, orden.checkIn, orden.checkOut, orden.fechaConformidad)))
        }
        true
    }

    fun leerCatastro(): Catastro = CatastroSql.leer(readableDatabase)

    fun guardarCliente(datos: DatosCliente): Boolean = transaccion { db -> CatastroSql.guardarCliente(db, datos) }
    fun guardarLocal(datos: DatosLocal): Boolean = transaccion { db -> CatastroSql.guardarLocal(db, datos) }

    fun guardarEquipo(datos: DatosEquipo): Boolean = transaccion { db -> CatastroSql.guardarEquipo(db, datos) }


    private fun vincularEquipo(db: SQLiteDatabase, orden: OrdenTrabajo): OrdenTrabajo =
        if (orden.equipoId != null) orden else orden.copy(equipoId = CatastroSql.asegurarEquipo(db, orden.local, orden.equipo))

    private fun transaccion(operacion: (SQLiteDatabase) -> Boolean): Boolean {
        val db = try { writableDatabase } catch (_: Exception) { return false }
        return try {
            db.beginTransaction()
            val correcto = operacion(db)
            if (correcto) db.setTransactionSuccessful()
            correcto
        } catch (_: Exception) {
            false
        } finally {
            if (db.inTransaction()) db.endTransaction()
        }
    }

    private fun crearSync(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sync_ot (otId TEXT PRIMARY KEY REFERENCES ordenes(id) ON DELETE CASCADE, revision INTEGER NOT NULL, estado TEXT NOT NULL, intentoMillis INTEGER)")
        db.execSQL("INSERT INTO sync_ot (otId, revision, estado) SELECT id, 0, 'LOCAL' FROM ordenes")
    }
    private fun marcarPendiente(db: SQLiteDatabase, id: String) {
        db.execSQL("INSERT OR IGNORE INTO sync_ot (otId, revision, estado) VALUES (?, 0, 'LOCAL')", arrayOf(id))
        db.execSQL("UPDATE sync_ot SET revision = revision + 1, estado = 'PENDIENTE', intentoMillis = NULL WHERE otId = ?", arrayOf(id))
    }
    fun leerSync(): List<RegistroSync> = readableDatabase.query("sync_ot", null, null, null, null, null, "otId ASC").use { cursor ->
        buildList { while (cursor.moveToNext()) {
            val indice = cursor.getColumnIndexOrThrow("intentoMillis")
            add(RegistroSync(cursor.valor("otId"), cursor.getLong(cursor.getColumnIndexOrThrow("revision")),
                EstadoSync.entries.firstOrNull { it.name == cursor.valor("estado") } ?: EstadoSync.LOCAL,
                if (cursor.isNull(indice)) null else cursor.getLong(indice)))
        } }
    }
    fun resultadoSync(id: String, revision: Long, error: Boolean, instante: Long): Boolean = transaccion { db ->
        db.update("sync_ot", ContentValues().apply {
            put("estado", if (error) "ERROR" else "ENVIADO"); put("intentoMillis", instante)
        }, "otId = ? AND revision = ? AND estado != 'ENVIADO'", arrayOf(id, revision.toString())) == 1
    }

    private fun agregarSla(db: SQLiteDatabase) {
        CatastroSql.agregarSla(db)
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaClienteId TEXT")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaClienteNombre TEXT")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaHorasLlegada INTEGER")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaHorasResolucion INTEGER")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaInicio INTEGER")
    }
    private fun leerAcuerdo(cursor: Cursor): AcuerdoSla? {
        if (cursor.isNull(cursor.getColumnIndexOrThrow("slaClienteId"))) return null
        return AcuerdoSla(cursor.valor("slaClienteId"), cursor.valor("slaClienteNombre"),
            cursor.getInt(cursor.getColumnIndexOrThrow("slaHorasLlegada")),
            cursor.getInt(cursor.getColumnIndexOrThrow("slaHorasResolucion")),
            cursor.getLong(cursor.getColumnIndexOrThrow("slaInicio")))
    }

    private fun crearTablaEventos(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE eventos_ot (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                otId TEXT NOT NULL REFERENCES ordenes(id) ON DELETE CASCADE,
                tipo TEXT NOT NULL,
                rol TEXT NOT NULL,
                actor TEXT NOT NULL,
                fechaMillis INTEGER NOT NULL,
                detalle TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX eventos_por_orden ON eventos_ot(otId, fechaMillis, id)")
    }

    private fun insertarEvento(db: SQLiteDatabase, evento: EventoOt) {
        db.insertOrThrow("eventos_ot", null, ContentValues().apply {
            put("otId", evento.otId)
            put("tipo", evento.tipo.name)
            put("rol", evento.rolActor)
            put("actor", evento.actor)
            put("fechaMillis", evento.fechaMillis)
            put("detalle", evento.detalle)
        })
    }

    fun eliminar(id: String, estado: EstadoOt): Boolean = try {
        writableDatabase.delete("ordenes", "id = ? AND estado = ?", arrayOf(id, estado.name)) == 1
    } catch (_: Exception) {
        false
    }

    private fun valores(orden: OrdenTrabajo) = ContentValues().apply {
        put("id", orden.id)
        put("local", orden.local)
        put("equipo", orden.equipo)
        put("descripcion", orden.descripcion)
        put("fechaProgramada", orden.fechaProgramada)
        put("tipo", orden.tipo.name)
        put("prioridad", orden.prioridad.name)
        put("estado", orden.estado.name)
        put("tecnico", orden.tecnico)
        put("checkIn", orden.checkIn)
        put("ubicacionCheckIn", orden.ubicacionCheckIn)
        put("checkOut", orden.checkOut)
        put("diagnostico", orden.diagnostico)
        put("actividad", orden.actividad)
        put("materiales", orden.materiales)
        put("observacionCliente", orden.observacionCliente)
        put("respuestaTecnico", orden.respuestaTecnico)
        put("fechaConformidad", orden.fechaConformidad)
        put("fotoEvidencia", orden.fotoEvidencia)
        put("ultimaActualizacion", orden.ultimaActualizacion)
        put("equipoId", orden.equipoId)
        put("slaClienteId", orden.acuerdoSla?.clienteId)
        put("slaClienteNombre", orden.acuerdoSla?.clienteNombre)
        put("slaHorasLlegada", orden.acuerdoSla?.horasLlegada)
        put("slaHorasResolucion", orden.acuerdoSla?.horasResolucion)
        put("slaInicio", orden.acuerdoSla?.inicioMillis)
    }

    private fun Cursor.valor(campo: String): String = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
