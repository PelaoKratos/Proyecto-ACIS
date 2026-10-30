package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import org.junit.Assert.*
import org.junit.Test

class ReporteSlaTest {
    private val inicio = TiempoVisita.fecha("15/10/2026 09:00")!!
    private val hora = 3_600_000L
    private val acuerdo = AcuerdoSla("c1", "Cliente", 4, 24, inicio)
    private fun orden(estado: EstadoOt = EstadoOt.CREADA) = OrdenTrabajo("OT-1", "Local", "Equipo", "Revisar",
        "15/10/2026 09:00", estado = estado, acuerdoSla = acuerdo)
    private fun evento(tipo: TipoEvento, fecha: Long, id: String = "OT-1") = EventoOt(otId = id, tipo = tipo, actor = "Demo", fechaMillis = fecha)
    private val creacion get() = evento(TipoEvento.CREACION, inicio - hora)

    @Test fun llegadaYResolucionEnElLimiteCumplenSinEsperarLaAprobacion() {
        val eventos = listOf(creacion, evento(TipoEvento.LLEGADA, inicio + 4 * hora), evento(TipoEvento.INFORME, inicio + 24 * hora))
        assertEquals(EstadoSla.CUMPLIDO, ReporteSla.evaluar(orden(EstadoOt.PENDIENTE_CONFORMIDAD), eventos, inicio + 30 * hora).estado)
        assertEquals(EstadoSla.CUMPLIDO, ReporteSla.evaluar(orden(EstadoOt.CERRADA), eventos + evento(TipoEvento.CONFORMIDAD, inicio + 48 * hora), inicio + 50 * hora).estado)
    }
    @Test fun unMinutoTardeOPlazoAgotadoEsVencido() {
        assertEquals(EstadoSla.VENCIDO, ReporteSla.evaluar(orden(), listOf(creacion), inicio + 4 * hora + 1).estado)
        val eventos = listOf(creacion, evento(TipoEvento.LLEGADA, inicio + 4 * hora + 60_000))
        assertEquals(EstadoSla.VENCIDO, ReporteSla.evaluar(orden(EstadoOt.EN_VISITA), eventos, inicio + 5 * hora).estado)
        val tarde = listOf(creacion, evento(TipoEvento.LLEGADA, inicio + hora), evento(TipoEvento.INFORME, inicio + 24 * hora + 1))
        assertEquals(EstadoSla.VENCIDO, ReporteSla.evaluar(orden(EstadoOt.PENDIENTE_CONFORMIDAD), tarde, inicio + 25 * hora).estado)
    }
    @Test fun riesgoSoloEnLaUltimaHoraDelSiguientePlazo() {
        assertEquals(EstadoSla.EN_PLAZO, ReporteSla.evaluar(orden(), listOf(creacion), inicio + 2 * hora).estado)
        assertEquals(EstadoSla.EN_RIESGO, ReporteSla.evaluar(orden(), listOf(creacion), inicio + 3 * hora).estado)
        assertEquals(EstadoSla.EN_RIESGO, ReporteSla.evaluar(orden(EstadoOt.EN_VISITA), listOf(creacion,
            evento(TipoEvento.LLEGADA, inicio + hora)), inicio + 23 * hora).estado)
    }
    @Test fun registrosAntiguosOIncompletosNoSeCuentanComoCumplidos() {
        assertEquals(EstadoSla.SIN_DATOS, ReporteSla.evaluar(orden().copy(acuerdoSla = null), listOf(creacion), inicio).estado)
        assertEquals(EstadoSla.SIN_DATOS, ReporteSla.evaluar(orden(), emptyList(), inicio).estado)
        assertEquals(EstadoSla.SIN_DATOS, ReporteSla.evaluar(orden(EstadoOt.CERRADA), listOf(creacion), inicio + 48 * hora).estado)
        assertEquals(EstadoSla.SIN_DATOS, ReporteSla.evaluar(orden(EstadoOt.EN_VISITA), listOf(creacion,
            evento(TipoEvento.LLEGADA, inicio + hora)), inicio).estado)
    }
    @Test fun reporteSeparaPeriodoCoberturaYPendientesSinInflarPorcentaje() {
        val ahora = inicio + 2 * hora
        val cumplida = orden(EstadoOt.PENDIENTE_CONFORMIDAD)
        val pendiente = orden().copy(id = "OT-2")
        val antigua = orden().copy(id = "OT-3", acuerdoSla = null)
        val vencida = orden().copy(id = "OT-5", acuerdoSla = acuerdo.copy(inicioMillis = inicio - 8 * hora))
        val otroMes = orden().copy(id = "OT-4", acuerdoSla = acuerdo.copy(inicioMillis = TiempoVisita.fecha("01/11/2026 09:00")!!))
        val eventos = listOf(creacion, evento(TipoEvento.LLEGADA, inicio), evento(TipoEvento.INFORME, inicio + hora),
            evento(TipoEvento.CREACION, inicio - hora, "OT-2"), evento(TipoEvento.CREACION, inicio - 9 * hora, "OT-5"))
        val resumen = ReporteSla.mensual(listOf(cumplida, pendiente, antigua, otroMes, vencida), eventos, ahora)
        assertEquals(4, resumen.ordenes.size)
        assertEquals(2, resumen.evaluadas)
        assertEquals(1, resumen.pendientes)
        assertEquals(1, resumen.sinDatos)
        assertEquals(50.0, resumen.porcentaje!!, 0.01)
        assertEquals(60.0, resumen.minutosPromedio!!, 0.01)
        assertEquals(1, resumen.visitasConTiempo)
        assertEquals(2, resumen.porCliente.single().evaluadas)
        assertNull(ReporteSla.mensual(listOf(pendiente), eventos, ahora).porcentaje)
        assertEquals(1, ReporteSla.mensual(listOf(otroMes), eventos, ahora, 1).ordenes.size)
    }
    @Test fun postergarNoReiniciaElAcuerdoOriginal() {
        val cambiada = orden().copy(fechaProgramada = "20/10/2026 09:00")
        assertEquals(EstadoSla.VENCIDO, ReporteSla.evaluar(cambiada, listOf(creacion), inicio + 5 * hora).estado)
    }
}
