package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class EstadoSla(val etiqueta: String) {
    CUMPLIDO("Cumplido"), VENCIDO("Vencido"), EN_RIESGO("En riesgo"), EN_PLAZO("En plazo"), SIN_DATOS("Sin datos suficientes")
}
data class EvaluacionSla(val orden: OrdenTrabajo, val estado: EstadoSla, val proximoLimite: Long? = null, val motivo: String = "", val minutosAtencion: Double? = null)
data class ClienteSla(val nombre: String, val cumplidas: Int, val evaluadas: Int, val pendientes: Int) {
    val porcentaje get() = if (evaluadas == 0) null else cumplidas * 100.0 / evaluadas
}
data class ResumenSla(val periodo: String, val ordenes: List<EvaluacionSla>, val porCliente: List<ClienteSla>) {
    val cumplidas get() = ordenes.count { it.estado == EstadoSla.CUMPLIDO }
    val vencidas get() = ordenes.count { it.estado == EstadoSla.VENCIDO }
    val evaluadas get() = cumplidas + vencidas
    val sinDatos get() = ordenes.count { it.estado == EstadoSla.SIN_DATOS }
    val pendientes get() = ordenes.count { it.estado == EstadoSla.EN_PLAZO || it.estado == EstadoSla.EN_RIESGO }
    val porcentaje get() = if (evaluadas == 0) null else cumplidas * 100.0 / evaluadas
    val visitasConTiempo get() = ordenes.count { it.minutosAtencion != null }
    val minutosPromedio get() = ordenes.mapNotNull { it.minutosAtencion }.takeIf { it.isNotEmpty() }?.average()
    val enRiesgo get() = ordenes.filter {
        it.estado == EstadoSla.EN_RIESGO || (it.estado == EstadoSla.VENCIDO &&
            it.orden.estado in setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA, EstadoOt.EN_VISITA))
    }.sortedBy { it.proximoLimite }
}

object ReporteSla {
    const val MARGEN_RIESGO = 60 * 60_000L
    fun evaluar(orden: OrdenTrabajo, eventos: List<EventoOt>, ahora: Long): EvaluacionSla {
        fun sinDatos(motivo: String) = EvaluacionSla(orden, EstadoSla.SIN_DATOS, motivo = motivo)
        val acuerdo = orden.acuerdoSla ?: return sinDatos("Sin acuerdo registrado al crear la orden.")
        if (acuerdo.horasLlegada !in 1..720 || acuerdo.horasResolucion !in acuerdo.horasLlegada..720)
            return sinDatos("Plazos incompletos o inválidos.")
        val propios = eventos.filter { it.otId == orden.id }
        val creacion = propios.filter { it.tipo == TipoEvento.CREACION }.minOfOrNull { it.fechaMillis }
            ?: return sinDatos("No hay un evento de creación verificable.")
        val llegada = propios.filter { it.tipo == TipoEvento.LLEGADA }.minOfOrNull { it.fechaMillis }
        val informe = propios.filter { it.tipo == TipoEvento.INFORME }.minOfOrNull { it.fechaMillis }
        if (creacion > ahora || (llegada != null && (llegada < creacion || llegada > ahora)) ||
            (informe != null && (llegada == null || informe < llegada || informe > ahora)))
            return sinDatos("Los eventos no permiten comprobar los tiempos.")
        val inicioVisita = orden.estado in setOf(EstadoOt.EN_VISITA, EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA)
        val finalizada = orden.estado in setOf(EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA)
        if ((inicioVisita && llegada == null) || (finalizada && informe == null))
            return sinDatos("Faltan eventos de llegada o informe.")
        val limiteLlegada = acuerdo.inicioMillis + acuerdo.horasLlegada * 3_600_000L
        val limiteResolucion = acuerdo.inicioMillis + acuerdo.horasResolucion * 3_600_000L
        val limite = if (llegada == null) limiteLlegada else limiteResolucion
        val tarde = (llegada != null && llegada > limiteLlegada) || (informe != null && informe > limiteResolucion) ||
            (llegada == null && ahora > limiteLlegada) || (informe == null && ahora > limiteResolucion)
        val estado = when {
            tarde -> EstadoSla.VENCIDO
            llegada != null && informe != null -> EstadoSla.CUMPLIDO
            limite - ahora <= MARGEN_RIESGO -> EstadoSla.EN_RIESGO
            else -> EstadoSla.EN_PLAZO
        }
        val incumpleLlegada = llegada != null && llegada > limiteLlegada
        return EvaluacionSla(orden, estado, if (incumpleLlegada) limiteLlegada else limite,
            if (incumpleLlegada || llegada == null) "Plazo de llegada" else "Plazo de resolución",
            if (llegada != null && informe != null) (informe - llegada) / 60_000.0 else null)
    }
    fun mensual(lista: List<OrdenTrabajo>, eventos: List<EventoOt>, ahora: Long, desplazamientoMes: Int = 0): ResumenSla {
        val calendario = Calendar.getInstance().apply {
            timeInMillis = ahora
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        calendario.add(Calendar.MONTH, desplazamientoMes)
        val inicio = calendario.timeInMillis
        calendario.add(Calendar.MONTH, 1)
        val fin = calendario.timeInMillis
        val periodo = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-CL")).format(java.util.Date(inicio))
        val consideradas = lista.filter {
            val fecha = it.acuerdoSla?.inicioMillis ?: TiempoVisita.fecha(it.fechaProgramada)
            fecha != null && fecha >= inicio && fecha < fin
        }.map { evaluar(it, eventos, ahora) }
        val clientes = consideradas.filter { it.orden.acuerdoSla != null }.groupBy { it.orden.acuerdoSla!!.clienteId }
            .map { (_, filas) ->
                ClienteSla(filas.first().orden.acuerdoSla!!.clienteNombre,
                    filas.count { it.estado == EstadoSla.CUMPLIDO },
                    filas.count { it.estado == EstadoSla.CUMPLIDO || it.estado == EstadoSla.VENCIDO },
                    filas.count { it.estado == EstadoSla.EN_PLAZO || it.estado == EstadoSla.EN_RIESGO })
            }.sortedBy { it.nombre }
        return ResumenSla(periodo, consideradas, clientes)
    }
}
