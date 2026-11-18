package com.example.proyecto_acis.domain.rules

import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.service.TiempoVisita

object OrdenTecnico {
    fun recientes(lista: List<OrdenTrabajo>) = lista.sortedWith(
        compareByDescending<OrdenTrabajo> { it.id.removePrefix("OT-").toLongOrNull() ?: 0 }
            .thenByDescending { it.ultimaActualizacion })

    fun siguiente(listaDelDia: List<OrdenTrabajo>): OrdenTrabajo? =
        listaDelDia.filter { it.estado == EstadoOt.EN_VISITA }
            .minByOrNull { TiempoVisita.fecha(it.fechaProgramada) ?: Long.MAX_VALUE }
            ?: listaDelDia.filter { it.estado == EstadoOt.ACEPTADA || it.estado == EstadoOt.ASIGNADA }
                .minByOrNull { TiempoVisita.fecha(it.fechaProgramada) ?: Long.MAX_VALUE }
}
