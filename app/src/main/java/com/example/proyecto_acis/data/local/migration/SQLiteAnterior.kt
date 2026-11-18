package com.example.proyecto_acis.data.local.migration

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.proyecto_acis.domain.model.EventoOt
import com.example.proyecto_acis.domain.model.TipoEvento
import com.example.proyecto_acis.domain.model.detalleRegistroPrevio

internal class SQLiteAnterior(context: Context, nombre: String = "acis_ordenes.db") : SQLiteOpenHelper(context, nombre, null, 8) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        CatastroAnterior.crear(db)
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
            CatastroAnterior.crear(db)
            db.execSQL("ALTER TABLE ordenes ADD COLUMN equipoId TEXT REFERENCES equipos(id)")
            CatastroAnterior.vincularOrdenes(db)
        }
        if (oldVersion < 7) agregarSla(db)
        if (oldVersion < 8) crearSync(db)
    }

    private fun crearSync(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sync_ot (otId TEXT PRIMARY KEY REFERENCES ordenes(id) ON DELETE CASCADE, revision INTEGER NOT NULL, estado TEXT NOT NULL, intentoMillis INTEGER)")
        db.execSQL("INSERT INTO sync_ot (otId, revision, estado) SELECT id, 0, 'LOCAL' FROM ordenes")
    }
    private fun agregarSla(db: SQLiteDatabase) {
        CatastroAnterior.agregarSla(db)
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaClienteId TEXT")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaClienteNombre TEXT")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaHorasLlegada INTEGER")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaHorasResolucion INTEGER")
        db.execSQL("ALTER TABLE ordenes ADD COLUMN slaInicio INTEGER")
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

    private fun Cursor.valor(campo: String): String = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
