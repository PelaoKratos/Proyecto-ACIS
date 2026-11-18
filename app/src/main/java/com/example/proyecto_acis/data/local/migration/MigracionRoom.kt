package com.example.proyecto_acis.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object MigracionRoom {
    private val tablas = listOf("clientes", "locales", "equipos", "ordenes", "eventos_ot", "sync_ot")

    val DESDE_8 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Room ejecuta esta migración dentro de una transacción.
            val secuencia = db.query("SELECT seq FROM sqlite_sequence WHERE name = 'eventos_ot'").use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
            tablas.forEach { db.execSQL("CREATE TEMP TABLE respaldo_$it AS SELECT * FROM $it") }
            tablas.asReversed().forEach { db.execSQL("DROP TABLE $it") }
            crearTablas(db)
            tablas.forEach { tabla ->
                val columnas = db.query("PRAGMA table_info($tabla)").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
                }.joinToString(", ") { "`$it`" }
                db.execSQL("INSERT INTO $tabla ($columnas) SELECT $columnas FROM respaldo_$tabla")
                db.execSQL("DROP TABLE respaldo_$tabla")
            }
            db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = 'eventos_ot'", arrayOf(secuencia))
            db.execSQL("INSERT INTO sqlite_sequence(name, seq) SELECT 'eventos_ot', ? WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = 'eventos_ot')", arrayOf(secuencia))
            db.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) }
        }
    }

    private fun crearTablas(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE clientes (
            id TEXT NOT NULL PRIMARY KEY, nombre TEXT COLLATE NOCASE NOT NULL,
            esDemo INTEGER NOT NULL DEFAULT 0, horasLlegada INTEGER, horasResolucion INTEGER)""")
        db.execSQL("CREATE UNIQUE INDEX clientes_nombre ON clientes(nombre)")
        db.execSQL("""CREATE TABLE locales (
            id TEXT NOT NULL PRIMARY KEY, clienteId TEXT NOT NULL, nombre TEXT COLLATE NOCASE NOT NULL,
            direccion TEXT NOT NULL DEFAULT '', latitud REAL, longitud REAL,
            FOREIGN KEY(clienteId) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX locales_por_cliente ON locales(clienteId)")
        db.execSQL("CREATE UNIQUE INDEX locales_nombre ON locales(nombre)")
        db.execSQL("""CREATE TABLE equipos (
            id TEXT NOT NULL PRIMARY KEY, localId TEXT NOT NULL, nombre TEXT COLLATE NOCASE NOT NULL,
            tipo TEXT NOT NULL, ubicacion TEXT NOT NULL DEFAULT '', esExterior INTEGER NOT NULL DEFAULT 0,
            ultimaMantencion INTEGER,
            FOREIGN KEY(localId) REFERENCES locales(id) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX equipos_por_local ON equipos(localId)")
        db.execSQL("CREATE UNIQUE INDEX equipos_local_nombre ON equipos(localId, nombre)")
        db.execSQL("""CREATE TABLE ordenes (
            id TEXT NOT NULL PRIMARY KEY, local TEXT NOT NULL, equipo TEXT NOT NULL, descripcion TEXT NOT NULL,
            fechaProgramada TEXT NOT NULL, tipo TEXT NOT NULL, prioridad TEXT NOT NULL, estado TEXT NOT NULL,
            tecnico TEXT NOT NULL, checkIn TEXT NOT NULL, ubicacionCheckIn TEXT NOT NULL DEFAULT '',
            checkOut TEXT NOT NULL, diagnostico TEXT NOT NULL, actividad TEXT NOT NULL, materiales TEXT NOT NULL,
            observacionCliente TEXT NOT NULL, respuestaTecnico TEXT NOT NULL, fechaConformidad TEXT NOT NULL,
            fotoEvidencia TEXT NOT NULL DEFAULT '', ultimaActualizacion INTEGER NOT NULL DEFAULT 0, equipoId TEXT,
            slaClienteId TEXT, slaClienteNombre TEXT, slaHorasLlegada INTEGER, slaHorasResolucion INTEGER, slaInicio INTEGER,
            FOREIGN KEY(equipoId) REFERENCES equipos(id) ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX ordenes_por_equipo ON ordenes(equipoId)")
        db.execSQL("""CREATE TABLE eventos_ot (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, otId TEXT NOT NULL, tipo TEXT NOT NULL,
            rol TEXT NOT NULL, actor TEXT NOT NULL, fechaMillis INTEGER NOT NULL, detalle TEXT NOT NULL,
            FOREIGN KEY(otId) REFERENCES ordenes(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX eventos_por_orden ON eventos_ot(otId, fechaMillis, id)")
        db.execSQL("""CREATE TABLE sync_ot (
            otId TEXT NOT NULL PRIMARY KEY, revision INTEGER NOT NULL, estado TEXT NOT NULL, intentoMillis INTEGER,
            FOREIGN KEY(otId) REFERENCES ordenes(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
    }
}
