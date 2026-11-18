package com.example.proyecto_acis.data.remote

import com.example.proyecto_acis.domain.service.HoraClima
import org.junit.Assert.*
import org.junit.Test

class RespuestaClimaTest {
    @Test fun vientoNoFinitoNoSeConvierteEnUnaHoraValida() {
        val respuesta = RespuestaClima(HorasClima(
            tiempos = listOf(3600, 7200), lluvias = listOf(65, 50), vientos = listOf(Double.NaN, 15.0)))
        assertEquals(listOf(HoraClima(7_200_000L, 50, 15.0)), respuesta.aPronostico(1000).horas)
    }

    @Test fun unTiempoFueraDeRangoNoDesbordaLosMilisegundos() {
        val respuesta = RespuestaClima(HorasClima(
            tiempos = listOf(Long.MAX_VALUE, -1, 3600), lluvias = listOf(65, 10, 50), vientos = listOf(15.0, 15.0, 40.0)))
        assertEquals(listOf(HoraClima(3_600_000L, 50, 40.0)), respuesta.aPronostico(1000).horas)
    }
}
