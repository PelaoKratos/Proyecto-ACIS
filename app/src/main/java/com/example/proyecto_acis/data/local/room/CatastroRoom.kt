package com.example.proyecto_acis.data.local.room

import com.example.proyecto_acis.data.local.room.entity.*
import com.example.proyecto_acis.domain.model.*
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.domain.validation.*
import java.util.UUID

internal class CatastroRoom(private val base: AcisRoom) {
    private val dao get() = base.catastro()
    fun leer() = Catastro(dao.clientes().map { it.aModelo() },
        dao.locales().map { it.aModelo() }, dao.equipos().map { it.aModelo() })

    fun asegurarEquipo(local: String, equipo: String): String? {
        if (local.isBlank() || equipo.isBlank()) return null
        val localId = dao.localPorNombre(local.trim()) ?: run {
            val clientes = dao.clientes()
            val cliente = clientes.firstOrNull { it.id == ValidadorCatastro.CLIENTE_PENDIENTE }
                ?: clientes.firstOrNull { it.nombre.equals("Cliente por identificar", true) }
                ?: ClienteEntidad(ValidadorCatastro.CLIENTE_PENDIENTE, "Cliente por identificar").also(dao::insertar)
            LocalEntidad(UUID.randomUUID().toString(), cliente.id, local.trim()).also(dao::insertar).id
        }
        return dao.equipoPorNombre(localId, equipo.trim()) ?: EquipoEntidad(
            UUID.randomUUID().toString(), localId, equipo.trim(), "Por especificar").also(dao::insertar).id
    }

    fun guardarCliente(datos: DatosCliente): Boolean {
        val anteriores = dao.clientes()
        if (ValidadorCatastro.cliente(datos, leer()) != null) return false
        val cliente = ClienteEntidad(datos.id ?: UUID.randomUUID().toString(), datos.nombre.trim(),
            anteriores.firstOrNull { it.id == datos.id }?.esDemo ?: false,
            datos.horasLlegada.trim().toIntOrNull(), datos.horasResolucion.trim().toIntOrNull())
        return if (datos.id == null) { dao.insertar(cliente); true } else dao.actualizar(cliente) == 1
    }

    fun guardarLocal(datos: DatosLocal): Boolean {
        if (ValidadorCatastro.local(datos, leer()).hayErrores) return false
        val local = LocalEntidad(datos.id ?: UUID.randomUUID().toString(), datos.clienteId,
            datos.nombre.trim(), datos.direccion.trim(), datos.latitud, datos.longitud)
        return if (datos.id == null) { dao.insertar(local); true } else dao.actualizar(local) == 1
    }

    fun guardarEquipo(datos: DatosEquipo): Boolean {
        val catastro = leer()
        if (ValidadorEquipo.validar(datos, catastro, base.ordenes().equiposConOrdenes().toSet()).hayErrores) return false
        val equipo = EquipoEntidad(datos.id ?: UUID.randomUUID().toString(), datos.localId,
            datos.nombre.trim(), datos.tipo.trim(), datos.ubicacion.trim(), datos.esExterior,
            catastro.equipos.firstOrNull { it.id == datos.id }?.ultimaMantencionMillis)
        return if (datos.id == null) { dao.insertar(equipo); true } else dao.actualizar(equipo) == 1
    }

    fun registrarMantencion(id: String, fecha: String) {
        TiempoVisita.fecha(fecha)?.let { dao.registrarMantencion(id, it) }
    }

    fun acuerdo(orden: OrdenTrabajo): AcuerdoSla? {
        val datos = leer()
        val equipo = datos.equipos.firstOrNull { it.id == orden.equipoId } ?: return null
        val local = datos.locales.firstOrNull { it.id == equipo.localId } ?: return null
        val cliente = datos.clientes.firstOrNull { it.id == local.clienteId } ?: return null
        return AcuerdoSla(cliente.id, cliente.nombre, cliente.horasLlegada ?: return null,
            cliente.horasResolucion ?: return null, TiempoVisita.fecha(orden.fechaProgramada) ?: return null)
    }
}
