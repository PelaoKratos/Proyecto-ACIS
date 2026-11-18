package com.example.proyecto_acis.data.local.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.proyecto_acis.data.demo.CatalogoDemo
import com.example.proyecto_acis.data.local.migration.MigracionRoom
import com.example.proyecto_acis.data.local.migration.SQLiteAnterior
import com.example.proyecto_acis.data.local.room.dao.*
import com.example.proyecto_acis.data.local.room.entity.*

@Database(entities = [ClienteEntidad::class, LocalEntidad::class, EquipoEntidad::class,
    OrdenEntidad::class, EventoEntidad::class, SyncEntidad::class], version = 9, exportSchema = true)
abstract class AcisRoom : RoomDatabase() {
    abstract fun ordenes(): OrdenDao
    abstract fun catastro(): CatastroDao
    abstract fun registros(): RegistroDao

    companion object {
        fun abrir(context: Context, nombre: String): AcisRoom {
            val archivo = context.getDatabasePath(nombre)
            if (archivo.exists()) {
                val version = SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
                if (version in 1..7) SQLiteAnterior(context, nombre).use { it.writableDatabase }
            }
            return Room.databaseBuilder(context.applicationContext, AcisRoom::class.java, nombre)
                .addMigrations(MigracionRoom.DESDE_8)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) { cargarCatalogo(db) }
                })
                .build()
        }

        private fun cargarCatalogo(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT INTO clientes (id, nombre, esDemo, horasLlegada, horasResolucion) VALUES (?, ?, 1, 4, 24)",
                arrayOf("cliente-demo-comercial", "Comercial Sur · demo"))
            db.execSQL("INSERT INTO clientes (id, nombre, esDemo, horasLlegada, horasResolucion) VALUES (?, ?, 1, 8, 48)",
                arrayOf("cliente-demo-servicios", "Servicios Centro · demo"))
            CatalogoDemo.equiposPorLocal.entries.forEachIndexed { indice, (nombre, equipos) ->
                val localId = "local-demo-$indice"
                db.execSQL("INSERT INTO locales (id, clienteId, nombre) VALUES (?, ?, ?)",
                    arrayOf(localId, if (indice == 1) "cliente-demo-servicios" else "cliente-demo-comercial", nombre))
                equipos.forEachIndexed { numero, equipo ->
                    db.execSQL("INSERT INTO equipos (id, localId, nombre, tipo, esExterior) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>("equipo-demo-$indice-$numero", localId, equipo, "Por especificar", if (equipo.contains("exterior")) 1 else 0))
                }
            }
        }
    }
}
