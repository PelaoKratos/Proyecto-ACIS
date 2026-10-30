package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.EstadoOt

object ReglasEstadoOt {
    private val siguientes = mapOf(
        EstadoOt.CREADA to setOf(EstadoOt.ASIGNADA),
        EstadoOt.ASIGNADA to setOf(EstadoOt.ACEPTADA, EstadoOt.CREADA),
        EstadoOt.ACEPTADA to setOf(EstadoOt.EN_VISITA),
        EstadoOt.EN_VISITA to setOf(EstadoOt.PENDIENTE_CONFORMIDAD),
        EstadoOt.PENDIENTE_CONFORMIDAD to setOf(EstadoOt.OBSERVADA, EstadoOt.CERRADA),
        EstadoOt.OBSERVADA to setOf(EstadoOt.PENDIENTE_CONFORMIDAD),
        EstadoOt.CERRADA to emptySet(),
    )

    fun permite(actual: EstadoOt, nuevo: EstadoOt): Boolean =
        actual == nuevo || nuevo in siguientes.getValue(actual)
}
