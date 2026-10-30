package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo

data class ResumenOperativo(
    val total: Int,
    val abiertas: Int,
    val cerradas: Int,
    val visitasConTiempo: Int,
    val minutosPromedio: Double?,
    val porEstado: Map<EstadoOt, Int>,
)

data class EquipoResumen(
    val local: String,
    val nombre: String,
    val delCatalogoDemo: Boolean,
    val visitas: Int,
    val ultimaMantencion: String?,
    val id: String = "",
    val cliente: String = "",
    val tipo: String = "",
    val ubicacion: String = "",
    val esExterior: Boolean = false,
)

object ResumenOrdenes {
    fun historialEquipo(orden: OrdenTrabajo, lista: List<OrdenTrabajo>): List<OrdenTrabajo> = lista.filter {
        val mismoEquipo = if (orden.equipoId != null && it.equipoId != null) orden.equipoId == it.equipoId
            else orden.local == it.local && orden.equipo == it.equipo
        it.id != orden.id && mismoEquipo && TiempoVisita.fecha(it.checkOut) != null
    }.sortedByDescending { TiempoVisita.fecha(it.checkOut) }.take(3)

    fun ultimasMantenciones(lista: List<OrdenTrabajo>): List<OrdenTrabajo> = lista
        .filter { it.estado == EstadoOt.CERRADA && TiempoVisita.fecha(it.checkOut) != null }
        .groupBy { it.equipoId?.let { id -> "id:$id" } ?: "texto:${it.local.length}:${it.local}${it.equipo}" }
        .values.mapNotNull { grupo -> grupo.maxByOrNull { TiempoVisita.fecha(it.checkOut) ?: Long.MIN_VALUE } }
        .sortedByDescending { TiempoVisita.fecha(it.checkOut) }.take(3)

    fun operativo(ordenes: List<OrdenTrabajo>): ResumenOperativo {
        val duraciones = ordenes.filter { it.checkOut.isNotBlank() }
            .mapNotNull { TiempoVisita.minutos(it.checkIn, it.checkOut) }
        val cerradas = ordenes.count { it.estado == EstadoOt.CERRADA }
        return ResumenOperativo(
            total = ordenes.size,
            abiertas = ordenes.size - cerradas,
            cerradas = cerradas,
            visitasConTiempo = duraciones.size,
            minutosPromedio = duraciones.takeIf { it.isNotEmpty() }?.average(),
            porEstado = EstadoOt.entries.associateWith { estado -> ordenes.count { it.estado == estado } },
        )
    }

    fun equipos(ordenes: List<OrdenTrabajo>, catastro: Catastro): List<EquipoResumen> {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
        val locales = catastro.locales.associateBy { it.id }
        val clientes = catastro.clientes.associateBy { it.id }
        return catastro.equipos.mapNotNull { equipo ->
            val local = locales[equipo.localId] ?: return@mapNotNull null
            val cliente = clientes[local.clienteId]
            EquipoResumen(local.nombre, equipo.nombre, cliente?.esDemo == true,
                ordenes.count { it.equipoId == equipo.id },
                equipo.ultimaMantencionMillis?.let { formato.format(Date(it)) },
                equipo.id, cliente?.nombre.orEmpty(), equipo.tipo, equipo.ubicacion, equipo.esExterior)
        }.sortedWith(compareBy<EquipoResumen> { it.local }.thenBy { it.nombre })
    }
}
