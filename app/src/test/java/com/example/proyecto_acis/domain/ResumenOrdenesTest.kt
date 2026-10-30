package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro
import com.example.proyecto_acis.data.Cliente
import com.example.proyecto_acis.data.Local
import com.example.proyecto_acis.data.Equipo
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import org.junit.Assert.*
import org.junit.Test

class ResumenOrdenesTest {
    @Test fun historialSigueElIdAunqueCambieElNombreYNoMezclaOtroEquipo() {
        val actual = orden("actual", equipo = "Nombre actualizado", equipoId = "e1")
        val previa = orden("previa", salida = "28/10/2026 10:00", equipoId = "e1")
        val diferente = orden("otro", equipo = "Nombre actualizado", salida = "29/10/2026 10:00", equipoId = "e2")
        assertEquals(listOf(previa), ResumenOrdenes.historialEquipo(actual, listOf(actual, previa, diferente)))
    }

    @Test fun mantencionesAgrupaPorIdYEligeLaUltimaSalida() {
        val vieja = orden("vieja", estado = EstadoOt.CERRADA, salida = "28/10/2026 10:00", equipoId = "e1")
        val nueva = orden("nueva", equipo = "Nombre actualizado", estado = EstadoOt.CERRADA,
            salida = "29/10/2026 10:00", equipoId = "e1")
        assertEquals(listOf(nueva), ResumenOrdenes.ultimasMantenciones(listOf(nueva, vieja)))
    }

    private fun orden(id: String, equipo: String = "Refrigerador", estado: EstadoOt = EstadoOt.CREADA,
        llegada: String = "", salida: String = "", equipoId: String? = null) = OrdenTrabajo(
        id = id, local = "Local centro", equipo = equipo, descripcion = "Revisión",
        fechaProgramada = "29/10/2026 09:00", estado = estado, checkIn = llegada, checkOut = salida, equipoId = equipoId,
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

    @Test fun catalogoConservaLaRelacionAunqueCambieElNombreDelEquipo() {
        val fecha = TiempoVisita.fecha("29/10/2026 10:00")!!
        val catastro = Catastro(
            clientes = listOf(Cliente("c1", "Cliente demo", true), Cliente("c2", "Cliente local")),
            locales = listOf(Local("l1", "c1", "Local centro"), Local("l2", "c2", "Local norte")),
            equipos = listOf(Equipo("e1", "l1", "Refrigerador renovado", "Refrigeración", ultimaMantencionMillis = fecha),
                Equipo("e2", "l2", "Congelador", "Refrigeración")),
        )
        val equipos = ResumenOrdenes.equipos(listOf(
            orden("1", estado = EstadoOt.CERRADA, equipoId = "e1"),
            orden("2", estado = EstadoOt.CERRADA, equipoId = "e1"),
            orden("3", equipo = "Congelador", equipoId = "e2"),
        ), catastro)
        assertEquals(2, equipos.size)
        val refrigerador = equipos.first { it.id == "e1" }
        assertTrue(refrigerador.delCatalogoDemo)
        assertEquals("Refrigerador renovado", refrigerador.nombre)
        assertEquals(2, refrigerador.visitas)
        assertEquals("29/10/2026 10:00", refrigerador.ultimaMantencion)
        assertFalse(equipos.first { it.id == "e2" }.delCatalogoDemo)
    }
}
