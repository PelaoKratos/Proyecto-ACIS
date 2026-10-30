package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import org.junit.Assert.*
import org.junit.Test

class DestinoMapaTest {
    private val local = Local("l1", "c1", "Nombre actualizado", "Av. Prat 120, Concepción")
    private val catastro = Catastro(locales = listOf(local), equipos = listOf(Equipo("e1", "l1", "Equipo", "Frío")))
    private val orden = OrdenTrabajo(id = "OT-1", local = "Nombre anterior", equipo = "Equipo",
        descripcion = "Revisar", fechaProgramada = "30/10/2026 10:00", tipo = TipoOt.PREVENTIVA,
        prioridad = PrioridadOt.MEDIA, equipoId = "e1")

    @Test fun direccionDelLocalSeResuelvePorIdentificadorAunqueCambieElNombre() {
        assertEquals(local.direccion, DestinoMapa.consulta(orden, catastro))
        assertEquals(local, DestinoMapa.local(orden, catastro))
        assertEquals("Nombre anterior", DestinoMapa.consulta(orden, Catastro()))
    }

    @Test fun prioridadCoordenadasDireccionYNombreSinConfundirLlegadaDelTecnico() {
        assertEquals("-36.82,-73.05", DestinoMapa.consulta(local.copy(latitud = -36.82, longitud = -73.05)))
        assertEquals(local.direccion, DestinoMapa.consulta(local.copy(latitud = Double.NaN, longitud = 0.0)))
        assertEquals(local.nombre, DestinoMapa.consulta(local.copy(direccion = "")))
        assertEquals(local.direccion, DestinoMapa.consulta(orden.copy(ubicacionCheckIn = "1,2"), catastro))
        assertEquals(local.direccion, DestinoMapa.consulta(orden.copy(equipoId = null, local = local.nombre), catastro))
    }
}
