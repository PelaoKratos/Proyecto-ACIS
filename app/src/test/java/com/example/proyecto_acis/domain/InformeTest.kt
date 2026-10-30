package com.example.proyecto_acis.domain

import org.junit.Assert.*
import org.junit.Test

class InformeTest {
    @Test fun conservaMaterialesAnterioresYFilasDelBorrador() {
        val filas = listOf(MaterialInforme("Filtro", "2"), MaterialInforme("Cable", ""))
        val (anterior, leidas) = MaterialesInforme.leer(MaterialesInforme.escribir("Paño de limpieza", filas))
        assertEquals("Paño de limpieza", anterior)
        assertEquals(filas, leidas)
    }

    @Test fun rechazaCantidadesInvalidasYAdmiteDecimales() {
        listOf("", "0", "-1", "NaN", "Infinity", "dos").forEach {
            assertNotNull(MaterialesInforme.errorCantidad(it))
        }
        assertNull(MaterialesInforme.errorCantidad("0,5"))
        assertNull(MaterialesInforme.errorCantidad("2"))
    }

    @Test fun calculaVisitaSinAceptarFechasInvalidas() {
        assertEquals(45L, TiempoVisita.minutos("29/10/2026 10:00", "29/10/2026 10:45"))
        assertNull(TiempoVisita.minutos("31/02/2026 10:00", "29/10/2026 10:45"))
        assertNull(TiempoVisita.minutos("29/10/2026 11:00", "29/10/2026 10:45"))
    }
}
