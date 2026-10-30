package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.EstadoOt
import org.junit.Assert.assertEquals
import org.junit.Test

class ReglasEstadoOtTest {
    @Test fun permiteSoloElRecorridoYElRetornoPorRechazoUObservacion() {
        val permitidos = setOf(
            EstadoOt.CREADA to EstadoOt.ASIGNADA,
            EstadoOt.ASIGNADA to EstadoOt.ACEPTADA,
            EstadoOt.ASIGNADA to EstadoOt.CREADA,
            EstadoOt.ACEPTADA to EstadoOt.EN_VISITA,
            EstadoOt.EN_VISITA to EstadoOt.PENDIENTE_CONFORMIDAD,
            EstadoOt.PENDIENTE_CONFORMIDAD to EstadoOt.OBSERVADA,
            EstadoOt.PENDIENTE_CONFORMIDAD to EstadoOt.CERRADA,
            EstadoOt.OBSERVADA to EstadoOt.PENDIENTE_CONFORMIDAD,
        )
        EstadoOt.entries.forEach { actual ->
            EstadoOt.entries.forEach { nuevo ->
                assertEquals("$actual -> $nuevo", actual == nuevo || (actual to nuevo) in permitidos,
                    ReglasEstadoOt.permite(actual, nuevo))
            }
        }
    }
}
