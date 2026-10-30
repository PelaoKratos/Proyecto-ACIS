package com.example.proyecto_acis.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.proyecto_acis.domain.DatosCliente
import com.example.proyecto_acis.domain.DatosLocal
import com.example.proyecto_acis.domain.ValidadorCatastro
import com.example.proyecto_acis.domain.DatosEquipo
import com.example.proyecto_acis.domain.ValidadorEquipo
import com.example.proyecto_acis.domain.TiempoVisita
import java.util.UUID

internal object CatastroSql {
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

    fun guardarCliente(db: SQLiteDatabase, datos: DatosCliente): Boolean {
        if (ValidadorCatastro.cliente(datos, leer(db)) != null) return false
        val valores = ContentValues().apply {
            put("nombre", datos.nombre.trim())
            put("horasLlegada", datos.horasLlegada.trim().toIntOrNull())
            put("horasResolucion", datos.horasResolucion.trim().toIntOrNull())
        }
        return if (datos.id == null) {
            valores.put("id", UUID.randomUUID().toString())
            db.insertOrThrow("clientes", null, valores) != -1L
        } else db.update("clientes", valores, "id = ?", arrayOf(datos.id)) == 1
    }

    fun guardarLocal(db: SQLiteDatabase, datos: DatosLocal): Boolean {
        if (ValidadorCatastro.local(datos, leer(db)).hayErrores) return false
        val valores = ContentValues().apply {
            put("clienteId", datos.clienteId); put("nombre", datos.nombre.trim()); put("direccion", datos.direccion.trim())
            put("latitud", datos.latitud); put("longitud", datos.longitud)
        }
        return if (datos.id == null) {
            valores.put("id", UUID.randomUUID().toString())
            db.insertOrThrow("locales", null, valores) != -1L
        } else db.update("locales", valores, "id = ?", arrayOf(datos.id)) == 1
    }

    fun guardarEquipo(db: SQLiteDatabase, datos: DatosEquipo): Boolean {
        val conOrdenes = db.query(true, "ordenes", arrayOf("equipoId"), "equipoId IS NOT NULL", null, null, null, null, null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        val catastro = leer(db)
        if (ValidadorEquipo.validar(datos, catastro, conOrdenes).hayErrores) return false
        val valores = ContentValues().apply {
            put("localId", datos.localId); put("nombre", datos.nombre.trim()); put("tipo", datos.tipo.trim())
            put("ubicacion", datos.ubicacion.trim()); put("esExterior", if (datos.esExterior) 1 else 0)
        }
        return if (datos.id == null) {
            valores.put("id", UUID.randomUUID().toString())
            db.insertOrThrow("equipos", null, valores) != -1L
        } else db.update("equipos", valores, "id = ?", arrayOf(datos.id)) == 1
    }

    fun leer(db: SQLiteDatabase): Catastro {
        val clientes = db.query("clientes", null, null, null, null, null, "nombre ASC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(Cliente(cursor.texto("id"), cursor.texto("nombre"), cursor.numero("esDemo") == 1L,
                cursor.enteroOpcional("horasLlegada"), cursor.enteroOpcional("horasResolucion"))) }
        }
        val locales = db.query("locales", null, null, null, null, null, "nombre ASC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(Local(cursor.texto("id"), cursor.texto("clienteId"), cursor.texto("nombre"),
                cursor.texto("direccion"), cursor.decimal("latitud"), cursor.decimal("longitud"))) }
        }
        val equipos = db.query("equipos", null, null, null, null, null, "nombre ASC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(Equipo(cursor.texto("id"), cursor.texto("localId"), cursor.texto("nombre"),
                cursor.texto("tipo"), cursor.texto("ubicacion"), cursor.numero("esExterior") == 1L,
                cursor.getColumnIndexOrThrow("ultimaMantencion").let { if (cursor.isNull(it)) null else cursor.getLong(it) })) }
        }
        return Catastro(clientes, locales, equipos)
    }

    fun agregarSla(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE clientes ADD COLUMN horasLlegada INTEGER")
        db.execSQL("ALTER TABLE clientes ADD COLUMN horasResolucion INTEGER")
        db.execSQL("UPDATE clientes SET horasLlegada = 4, horasResolucion = 24 WHERE id = 'cliente-demo-comercial'")
        db.execSQL("UPDATE clientes SET horasLlegada = 8, horasResolucion = 48 WHERE id = 'cliente-demo-servicios'")
    }

    fun acuerdo(db: SQLiteDatabase, orden: OrdenTrabajo): AcuerdoSla? {
        val datos = leer(db)
        val equipo = datos.equipos.firstOrNull { it.id == orden.equipoId } ?: return null
        val local = datos.locales.firstOrNull { it.id == equipo.localId } ?: return null
        val cliente = datos.clientes.firstOrNull { it.id == local.clienteId } ?: return null
        val llegada = cliente.horasLlegada ?: return null
        val resolucion = cliente.horasResolucion ?: return null
        val inicio = TiempoVisita.fecha(orden.fechaProgramada) ?: return null
        return AcuerdoSla(cliente.id, cliente.nombre, llegada, resolucion, inicio)
    }
    private fun Cursor.enteroOpcional(campo: String): Int? = getColumnIndexOrThrow(campo).let {
        if (isNull(it)) null else getInt(it)
    }

    private fun id(db: SQLiteDatabase, tabla: String, seleccion: String, argumentos: Array<String>): String? =
        db.query(tabla, arrayOf("id"), seleccion, argumentos, null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun Cursor.texto(campo: String) = getString(getColumnIndexOrThrow(campo)).orEmpty()
    private fun Cursor.numero(campo: String) = getLong(getColumnIndexOrThrow(campo))
    private fun Cursor.decimal(campo: String): Double? = getColumnIndexOrThrow(campo).let { if (isNull(it)) null else getDouble(it) }
}
