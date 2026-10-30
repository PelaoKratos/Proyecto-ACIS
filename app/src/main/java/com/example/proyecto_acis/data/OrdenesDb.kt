package com.example.proyecto_acis.data

import com.example.proyecto_acis.domain.DatosEquipo
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrdenesDb(context: Context, nombre: String = "acis_ordenes.db") : SQLiteOpenHelper(context, nombre, null, 6) {
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
            val vinculada = vincularEquipo(db, orden)
            db.insertOrThrow("ordenes", null, valores(vinculada))
            if (vinculada.estado == EstadoOt.CERRADA) vinculada.equipoId?.let { CatastroSql.actualizarMantencion(db, it, vinculada.checkOut) }
            if (evento != null) insertarEvento(db, evento)
            true
        }
    }

    fun actualizar(orden: OrdenTrabajo, estadoAnterior: EstadoOt, evento: EventoOt? = null): Boolean {
        if (evento != null && evento.otId != orden.id) return false
        return transaccion { db ->
            val vinculada = vincularEquipo(db, orden)
            if (db.update("ordenes", valores(vinculada), "id = ? AND estado = ?", arrayOf(orden.id, estadoAnterior.name)) != 1) false
            else {
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
            if (vinculada.estado == EstadoOt.CERRADA) vinculada.equipoId?.let { CatastroSql.actualizarMantencion(db, it, vinculada.checkOut) }
            insertarEvento(db, EventoOt(otId = orden.id, tipo = TipoEvento.REGISTRO_PREVIO,
                actor = "Sistema local", fechaMillis = System.currentTimeMillis(),
                detalle = detalleRegistroPrevio(orden.estado.etiqueta, orden.checkIn, orden.checkOut, orden.fechaConformidad)))
        }
        true
    }

    fun leerCatastro(): Catastro = CatastroSql.leer(readableDatabase)

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
    }

    private fun Cursor.valor(campo: String): String = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
