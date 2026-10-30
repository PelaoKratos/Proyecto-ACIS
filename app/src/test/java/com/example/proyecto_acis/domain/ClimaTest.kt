package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import org.junit.Assert.*
import org.junit.Test

class ClimaTest {
    private val local = Local("l1", "c1", "Sucursal", latitud = -36.82, longitud = -73.05)
    private val equipo = Equipo("e1", "l1", "Condensador", "Frío", esExterior = true)
    private val catastro = Catastro(locales = listOf(local), equipos = listOf(equipo))
    private val orden = OrdenTrabajo("OT-1", "Sucursal", "Condensador", "Revisar", "30/10/2026 11:30", equipoId = "e1")

    @Test fun alertaIncluyeElLimiteSinMarcarLosValoresInferiores() {
        assertFalse(HoraClima(0, 59, 39.9).hayAlerta)
        assertTrue(HoraClima(0, 60, 0.0).hayAlerta)
        assertTrue(HoraClima(0, 0, 40.0).hayAlerta)
    }
    @Test fun soloEquipoExteriorYLocalValidoPermitenConsultar() {
        assertNotNull(ReglasClima.orden(orden, catastro)?.solicitud)
        assertNull(ReglasClima.orden(orden, catastro.copy(equipos = listOf(equipo.copy(esExterior = false)))))
        assertNull(ReglasClima.orden(orden.copy(estado = EstadoOt.CERRADA), catastro))
        val sinUbicacion = ReglasClima.orden(orden, catastro.copy(locales = listOf(local.copy(latitud = null, longitud = null))))!!
        assertNull(sinUbicacion.solicitud)
        assertTrue(sinUbicacion.aviso.isNotBlank())
        assertNull(ReglasClima.contexto(local, "fecha incorrecta").solicitud)
    }
    @Test fun seleccionPorHoraNoUsaOtroDiaNiElClimaActual() {
        val solicitud = SolicitudClima(-36.82, -73.05, 10 * 3_600_000L + 30 * 60_000L)
        val hora = HoraClima(10 * 3_600_000L, 65, 12.0)
        val pronostico = Pronostico(1000, listOf(hora))
        assertEquals(hora, ReglasClima.resultado(pronostico, solicitud, 2000, false).hora)
        assertNull(ReglasClima.resultado(pronostico, solicitud.copy(instante = 11 * 3_600_000L), 2000, false).hora)
        assertNull(ReglasClima.resultado(pronostico, solicitud, ReglasClima.MAX_CACHE + 1001, true).hora)
        assertNull(ReglasClima.resultado(pronostico, solicitud, 0, true).hora)
        assertFalse(ReglasClima.vigente(pronostico, 1001 + ReglasClima.VIGENCIA))
    }
}
