package com.example.proyecto_acis.data.local.migration

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.proyecto_acis.data.demo.CatalogoDemo
import com.example.proyecto_acis.domain.model.Cliente
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.service.TiempoVisita
import java.util.UUID

internal object CatastroAnterior {
    fun crear(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE clientes (id TEXT PRIMARY KEY, nombre TEXT NOT NULL UNIQUE COLLATE NOCASE, esDemo INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("""
            CREATE TABLE locales (
                id TEXT PRIMARY KEY,
                clienteId TEXT NOT NULL REFERENCES clientes(id),
                nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
                direccion TEXT NOT NULL DEFAULT '',
                latitud REAL,
                longitud REAL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE equipos (
                id TEXT PRIMARY KEY,
                localId TEXT NOT NULL REFERENCES locales(id),
                nombre TEXT NOT NULL COLLATE NOCASE,
                tipo TEXT NOT NULL,
                ubicacion TEXT NOT NULL DEFAULT '',
                esExterior INTEGER NOT NULL DEFAULT 0,
                ultimaMantencion INTEGER,
                UNIQUE(localId, nombre)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX locales_por_cliente ON locales(clienteId)")
        db.execSQL("CREATE INDEX equipos_por_local ON equipos(localId)")
        db.insertOrThrow("clientes", null, ContentValues().apply {
            put("id", "cliente-demo-comercial"); put("nombre", "Comercial Sur · demo"); put("esDemo", 1)
        })
        db.insertOrThrow("clientes", null, ContentValues().apply {
            put("id", "cliente-demo-servicios"); put("nombre", "Servicios Centro · demo"); put("esDemo", 1)
        })
        CatalogoDemo.equiposPorLocal.entries.forEachIndexed { indice, (nombre, equipos) ->
            val localId = "local-demo-$indice"
            db.insertOrThrow("locales", null, ContentValues().apply {
                put("id", localId); put("nombre", nombre)
                put("clienteId", if (indice == 1) "cliente-demo-servicios" else "cliente-demo-comercial")
            })
            equipos.forEachIndexed { numero, equipo ->
                db.insertOrThrow("equipos", null, ContentValues().apply {
                    put("id", "equipo-demo-$indice-$numero"); put("localId", localId)
                    put("nombre", equipo); put("tipo", "Por especificar")
                    put("esExterior", if (equipo.contains("exterior")) 1 else 0)
                })
            }
        }
    }

    fun vincularOrdenes(db: SQLiteDatabase) {
        db.query("ordenes", arrayOf("id", "local", "equipo", "estado", "checkOut"), null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                val equipoId = asegurarEquipo(db, cursor.texto("local"), cursor.texto("equipo")) ?: continue
                db.update("ordenes", ContentValues().apply { put("equipoId", equipoId) }, "id = ?", arrayOf(cursor.texto("id")))
                if (cursor.texto("estado") == EstadoOt.CERRADA.name)
                    actualizarMantencion(db, equipoId, cursor.texto("checkOut"))
            }
        }
    }

    fun asegurarEquipo(db: SQLiteDatabase, local: String, equipo: String): String? {
        if (local.isBlank() || equipo.isBlank()) return null
        val localId = id(db, "locales", "nombre = ? COLLATE NOCASE", arrayOf(local.trim())) ?: run {
            val clienteId = id(db, "clientes", "id = ?", arrayOf("cliente-por-identificar"))
                ?: id(db, "clientes", "nombre = ? COLLATE NOCASE", arrayOf("Cliente por identificar"))
                ?: run {
                    db.insertOrThrow("clientes", null, ContentValues().apply {
                        put("id", "cliente-por-identificar"); put("nombre", "Cliente por identificar")
                    })
                    "cliente-por-identificar"
                }
            val nuevo = UUID.randomUUID().toString()
            db.insertOrThrow("locales", null, ContentValues().apply {
                put("id", nuevo); put("clienteId", clienteId); put("nombre", local.trim())
            })
            nuevo
        }
        return id(db, "equipos", "localId = ? AND nombre = ? COLLATE NOCASE", arrayOf(localId, equipo.trim())) ?: run {
            val nuevo = UUID.randomUUID().toString()
            db.insertOrThrow("equipos", null, ContentValues().apply {
                put("id", nuevo); put("localId", localId); put("nombre", equipo.trim()); put("tipo", "Por especificar")
            })
            nuevo
        }
    }

    fun actualizarMantencion(db: SQLiteDatabase, equipoId: String, fecha: String) {
        val marca = TiempoVisita.fecha(fecha) ?: return
        db.update("equipos", ContentValues().apply { put("ultimaMantencion", marca) },
            "id = ? AND (ultimaMantencion IS NULL OR ultimaMantencion < ?)", arrayOf(equipoId, marca.toString()))
    }

    fun agregarSla(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE clientes ADD COLUMN horasLlegada INTEGER")
        db.execSQL("ALTER TABLE clientes ADD COLUMN horasResolucion INTEGER")
        db.execSQL("UPDATE clientes SET horasLlegada = 4, horasResolucion = 24 WHERE id = 'cliente-demo-comercial'")
        db.execSQL("UPDATE clientes SET horasLlegada = 8, horasResolucion = 48 WHERE id = 'cliente-demo-servicios'")
    }

    private fun id(db: SQLiteDatabase, tabla: String, seleccion: String, argumentos: Array<String>): String? =
        db.query(tabla, arrayOf("id"), seleccion, argumentos, null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun Cursor.texto(campo: String) = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
