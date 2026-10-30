package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro
import com.example.proyecto_acis.data.Local
import com.example.proyecto_acis.data.OrdenTrabajo

object DestinoMapa {
    fun local(orden: OrdenTrabajo, catastro: Catastro): Local? {
        val equipo = catastro.equipos.firstOrNull { it.id == orden.equipoId }
        return if (equipo != null) catastro.locales.firstOrNull { it.id == equipo.localId }
            else catastro.locales.firstOrNull { it.nombre.equals(orden.local, ignoreCase = true) }
    }

    fun consulta(local: Local): String = when {
        local.latitud != null && local.longitud != null && ValidadorCatastro.coordenadasValidas(local.latitud, local.longitud) ->
            "${local.latitud},${local.longitud}"
        local.direccion.isNotBlank() -> local.direccion.trim()
        else -> local.nombre
    }

    fun consulta(orden: OrdenTrabajo, catastro: Catastro): String = local(orden, catastro)?.let(::consulta) ?: orden.local

    fun descripcion(orden: OrdenTrabajo, catastro: Catastro): String {
        val local = local(orden, catastro)
        return when {
            local?.latitud != null && local.longitud != null -> "Se abrirá la ubicación aproximada registrada para el local."
            local != null && local.direccion.isNotBlank() -> "Dirección del local: ${local.direccion}"
            else -> "Búsqueda por nombre del local. Confirma la dirección antes de viajar."
        }
    }
}
