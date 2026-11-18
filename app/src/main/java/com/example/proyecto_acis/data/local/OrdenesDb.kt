package com.example.proyecto_acis.data.local

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.proyecto_acis.data.local.room.*
import com.example.proyecto_acis.data.local.room.entity.SyncEntidad
import com.example.proyecto_acis.domain.model.*
import com.example.proyecto_acis.domain.validation.DatosCliente
import com.example.proyecto_acis.domain.validation.DatosEquipo
import com.example.proyecto_acis.domain.validation.DatosLocal
import java.io.Closeable

@WorkerThread
class OrdenesDb(context: Context, nombre: String = "acis_ordenes.db") : Closeable {
    private val base = lazy { AcisRoom.abrir(context.applicationContext, nombre) }
    private val room get() = base.value
    private val catastro by lazy { CatastroRoom(room) }

    // Acceso para las pruebas de migracion y transacciones.
    val readableDatabase: SupportSQLiteDatabase get() = room.openHelper.readableDatabase
    val writableDatabase: SupportSQLiteDatabase get() = room.openHelper.writableDatabase

    fun leer(): List<OrdenTrabajo> = room.ordenes().leer().map { it.aModelo() }
    fun leerEventos(): List<EventoOt> = room.registros().eventos().map { it.aModelo() }
    fun leerCatastro(): Catastro = catastro.leer()
    fun leerSync() = room.registros().sincronizacion().map { it.aModelo() }

    fun guardarCliente(datos: DatosCliente): Boolean = transaccion { catastro.guardarCliente(datos) }
    fun guardarLocal(datos: DatosLocal): Boolean = transaccion { catastro.guardarLocal(datos) }
    fun guardarEquipo(datos: DatosEquipo): Boolean = transaccion { catastro.guardarEquipo(datos) }

    fun insertar(orden: OrdenTrabajo, evento: EventoOt? = null): Boolean {
        if (evento != null && evento.otId != orden.id) return false
        return transaccion {
            val equipo = vincularEquipo(orden)
            val nueva = if (evento?.tipo == TipoEvento.CREACION) equipo.copy(acuerdoSla = catastro.acuerdo(equipo)) else equipo
            room.ordenes().insertar(nueva.aEntidad())
            marcarPendiente(nueva.id)
            if (nueva.estado == EstadoOt.CERRADA) nueva.equipoId?.let { catastro.registrarMantencion(it, nueva.checkOut) }
            if (evento != null) room.registros().insertar(evento.aEntidad())
            true
        }
    }

    fun actualizar(orden: OrdenTrabajo, estadoAnterior: EstadoOt, evento: EventoOt? = null): Boolean {
        if (evento != null && evento.otId != orden.id) return false
        return transaccion {
            val anterior = room.ordenes().buscar(orden.id) ?: return@transaccion false
            if (anterior.estado != estadoAnterior.name) return@transaccion false
            val nueva = vincularEquipo(orden)
            val entidad = nueva.aEntidad().copy(slaClienteId = anterior.slaClienteId,
                slaClienteNombre = anterior.slaClienteNombre, slaHorasLlegada = anterior.slaHorasLlegada,
                slaHorasResolucion = anterior.slaHorasResolucion, slaInicio = anterior.slaInicio)
            if (room.ordenes().actualizar(entidad) != 1) return@transaccion false
            marcarPendiente(nueva.id)
            if (nueva.estado == EstadoOt.CERRADA) nueva.equipoId?.let { catastro.registrarMantencion(it, nueva.checkOut) }
            if (evento != null) room.registros().insertar(evento.aEntidad())
            true
        }
    }

    fun guardarTodas(ordenes: List<OrdenTrabajo>): Boolean = transaccion {
        ordenes.forEach { orden ->
            val nueva = vincularEquipo(orden)
            room.ordenes().insertar(nueva.aEntidad())
            room.registros().insertar(SyncEntidad(nueva.id, 0, "LOCAL"))
            if (nueva.estado == EstadoOt.CERRADA) nueva.equipoId?.let { catastro.registrarMantencion(it, nueva.checkOut) }
            room.registros().insertar(EventoOt(otId = nueva.id, tipo = TipoEvento.REGISTRO_PREVIO,
                actor = "Sistema local", fechaMillis = System.currentTimeMillis(),
                detalle = detalleRegistroPrevio(nueva.estado.etiqueta, nueva.checkIn, nueva.checkOut, nueva.fechaConformidad)).aEntidad())
        }
        true
    }

    fun eliminar(id: String, estado: EstadoOt): Boolean = transaccion {
        room.ordenes().eliminar(id, estado.name) == 1
    }

    fun resultadoSync(id: String, revision: Long, error: Boolean, instante: Long): Boolean = transaccion {
        room.registros().resultado(id, revision, if (error) "ERROR" else "ENVIADO", instante) == 1
    }

    private fun vincularEquipo(orden: OrdenTrabajo) = if (orden.equipoId != null) orden
        else orden.copy(equipoId = catastro.asegurarEquipo(orden.local, orden.equipo))

    private fun marcarPendiente(id: String) {
        room.registros().insertar(SyncEntidad(id, 0, "LOCAL"))
        room.registros().marcarPendiente(id)
    }

    private class OperacionCancelada : RuntimeException()
    private fun transaccion(operacion: () -> Boolean): Boolean = try {
        room.runInTransaction { if (!operacion()) throw OperacionCancelada() }
        true
    } catch (_: Exception) { false }

    override fun close() { if (base.isInitialized()) room.close() }
}
