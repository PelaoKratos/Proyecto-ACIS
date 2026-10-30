package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.DatosDemostracion
import com.example.proyecto_acis.data.EstadoOt
import org.junit.Assert.*
import org.junit.Test

class DatosDemostracionTest {
    @Test fun ejemplosTienenIdsUnicosYVisitasFinalizadasCoherentes() {
        val ejemplos = DatosDemostracion.crear(1020, 1_793_265_600_000)
        assertEquals(6, ejemplos.size)
        assertEquals(6, ejemplos.map { it.id }.distinct().size)
        assertEquals("OT-1020", ejemplos.first().id)
        assertEquals("OT-1025", ejemplos.last().id)
        assertEquals(5, ejemplos.map { it.local to it.equipo }.distinct().size)
        assertEquals(6, ejemplos.map { it.estado }.distinct().size)
        ejemplos.forEach {
            assertNotNull(TiempoVisita.fecha(it.fechaProgramada))
            assertTrue(it.ubicacionCheckIn.isBlank())
            assertTrue(it.fotosEvidencia.isEmpty())
            if (it.estado != EstadoOt.CREADA) assertTrue(it.tecnico.isNotBlank())
            if (it.checkOut.isNotBlank()) assertEquals(45L, TiempoVisita.minutos(it.checkIn, it.checkOut))
        }
    }
}
