package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import org.junit.Assert.*
import org.junit.Test

class ResumenOrdenesTest {
    private fun orden(id: String, equipo: String = "Refrigerador", estado: EstadoOt = EstadoOt.CREADA,
        llegada: String = "", salida: String = "") = OrdenTrabajo(
        id = id, local = "Local centro", equipo = equipo, descripcion = "Revisión",
        fechaProgramada = "29/10/2026 09:00", estado = estado, checkIn = llegada, checkOut = salida,
    )

    @Test fun promedioExcluyeVisitasIncompletasOConHorariosInvalidos() {
        val resumen = ResumenOrdenes.operativo(listOf(
            orden("1", estado = EstadoOt.CERRADA, llegada = "29/10/2026 09:00", salida = "29/10/2026 09:30"),
            orden("2", estado = EstadoOt.PENDIENTE_CONFORMIDAD, llegada = "29/10/2026 10:00", salida = "29/10/2026 11:00"),
            orden("3", llegada = "29/10/2026 12:00"),
            orden("4", llegada = "29/10/2026 13:00", salida = "29/10/2026 12:00"),
        ))
        assertEquals(4, resumen.total)
        assertEquals(3, resumen.abiertas)
        assertEquals(1, resumen.cerradas)
        assertEquals(2, resumen.visitasConTiempo)
        assertEquals(45.0, resumen.minutosPromedio!!, 0.001)
    }

    @Test fun unReporteVacioNoInventaUnPromedio() {
        assertNull(ResumenOrdenes.operativo(emptyList()).minutosPromedio)
    }

    @Test fun catalogoCombinaEquiposSinDuplicarlosYUsaUltimaMantencionCerrada() {
        val equipos = ResumenOrdenes.equipos(listOf(
            orden("1", estado = EstadoOt.CERRADA, salida = "28/10/2026 10:00"),
            orden("2", estado = EstadoOt.CERRADA, salida = "29/10/2026 10:00"),
            orden("3", equipo = "Congelador"),
        ), mapOf("Local centro" to listOf("Refrigerador")))
        assertEquals(2, equipos.size)
        val refrigerador = equipos.first { it.nombre == "Refrigerador" }
        assertTrue(refrigerador.delCatalogoDemo)
        assertEquals(2, refrigerador.visitas)
        assertEquals("29/10/2026 10:00", refrigerador.ultimaMantencion)
        assertFalse(equipos.first { it.nombre == "Congelador" }.delCatalogoDemo)
    }
}
