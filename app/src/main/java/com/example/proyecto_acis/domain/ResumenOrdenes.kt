package com.example.proyecto_acis.domain

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
)

object ResumenOrdenes {
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

    fun equipos(ordenes: List<OrdenTrabajo>, catalogo: Map<String, List<String>>): List<EquipoResumen> {
        val demo = catalogo.flatMap { (local, equipos) -> equipos.map { local to it } }.toSet()
        val registrados = ordenes.groupBy { it.local to it.equipo }
        return (demo + registrados.keys).map { (local, equipo) ->
            val visitas = registrados[local to equipo].orEmpty()
            val ultima = visitas.filter { it.estado == EstadoOt.CERRADA && TiempoVisita.fecha(it.checkOut) != null }
                .maxByOrNull { TiempoVisita.fecha(it.checkOut) ?: Long.MIN_VALUE }
            EquipoResumen(local, equipo, (local to equipo) in demo, visitas.size, ultima?.checkOut)
        }.sortedWith(compareBy<EquipoResumen> { it.local }.thenBy { it.nombre })
    }
}
