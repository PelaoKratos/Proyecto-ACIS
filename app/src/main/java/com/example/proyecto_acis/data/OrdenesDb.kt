package com.example.proyecto_acis.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrdenesDb(context: Context) : SQLiteOpenHelper(context, "acis_ordenes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
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
                checkOut TEXT NOT NULL,
                diagnostico TEXT NOT NULL,
                actividad TEXT NOT NULL,
                materiales TEXT NOT NULL,
                observacionCliente TEXT NOT NULL,
                respuestaTecnico TEXT NOT NULL,
                fechaConformidad TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

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
                    checkOut = cursor.valor("checkOut"),
                    diagnostico = cursor.valor("diagnostico"),
                    actividad = cursor.valor("actividad"),
                    materiales = cursor.valor("materiales"),
                    observacionCliente = cursor.valor("observacionCliente"),
                    respuestaTecnico = cursor.valor("respuestaTecnico"),
                    fechaConformidad = cursor.valor("fechaConformidad"),
                ))
            }
        }
    }

    fun guardar(ordenes: List<OrdenTrabajo>): Boolean {
        val db = writableDatabase
        return try {
            db.beginTransaction()
            db.delete("ordenes", null, null)
            ordenes.forEach { orden ->
                val valores = ContentValues().apply {
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
                    put("checkOut", orden.checkOut)
                    put("diagnostico", orden.diagnostico)
                    put("actividad", orden.actividad)
                    put("materiales", orden.materiales)
                    put("observacionCliente", orden.observacionCliente)
                    put("respuestaTecnico", orden.respuestaTecnico)
                    put("fechaConformidad", orden.fechaConformidad)
                }
                if (db.insert("ordenes", null, valores) == -1L) error("No se pudo guardar la OT")
            }
            db.setTransactionSuccessful()
            true
        } catch (_: Exception) {
            false
        } finally {
            if (db.inTransaction()) db.endTransaction()
        }
    }

    private fun Cursor.valor(campo: String): String = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
