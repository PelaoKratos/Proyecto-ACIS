package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import org.junit.Assert.*
import org.junit.Test

class OrdenTecnicoTest {
    private fun orden(numero: Int, hora: String, estado: EstadoOt = EstadoOt.ASIGNADA, actualizacion: Long = 0) =
        OrdenTrabajo("OT-$numero", "Local", "Equipo", "Revisar", "30/10/2026 $hora",
            estado = estado, ultimaActualizacion = actualizacion)

    @Test fun nuevasPrimeroAunqueUnaOrdenViejaSeActualiceDespues() {
        val lista = listOf(orden(1009, "15:00", actualizacion = 9999), orden(1011, "09:00"), orden(1010, "11:00"))
        assertEquals(listOf("OT-1011", "OT-1010", "OT-1009"), OrdenTecnico.recientes(lista).map { it.id })
    }
    @Test fun siguienteVisitaConservaLaHoraMasCercanaYDaPreferenciaALaActiva() {
        val lista = listOf(orden(1011, "15:00"), orden(1009, "09:00", EstadoOt.ACEPTADA))
        assertEquals("OT-1009", OrdenTecnico.siguiente(lista)?.id)
        val activa = orden(1010, "11:00", EstadoOt.EN_VISITA)
        assertEquals(activa, OrdenTecnico.siguiente(lista + activa))
        assertNull(OrdenTecnico.siguiente(listOf(orden(1008, "08:00", EstadoOt.CERRADA))))
    }
}
