package com.example.proyecto_acis.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatosDemostracion {
    fun crear(primerNumero: Int, ahora: Long): List<OrdenTrabajo> {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
        fun fecha(minutos: Long) = formato.format(Date(ahora + minutos * 60_000))
        val equipos = CatalogoDemo.equiposPorLocal.flatMap { (local, lista) -> lista.map { local to it } }
        val estados = listOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA,
            EstadoOt.EN_VISITA, EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.CERRADA)
        return estados.mapIndexed { indice, estado ->
            val (local, equipo) = equipos[indice % equipos.size]
            val finalizada = estado == EstadoOt.PENDIENTE_CONFORMIDAD || estado == EstadoOt.CERRADA
            val llegada = when (estado) {
                EstadoOt.EN_VISITA -> fecha(-15)
                EstadoOt.PENDIENTE_CONFORMIDAD -> fecha(-90)
                EstadoOt.CERRADA -> fecha(-2880)
                else -> ""
            }
            val salida = when (estado) {
                EstadoOt.PENDIENTE_CONFORMIDAD -> fecha(-45)
                EstadoOt.CERRADA -> fecha(-2835)
                else -> ""
            }
            OrdenTrabajo(
                id = "OT-${primerNumero + indice}", local = local, equipo = equipo,
                descripcion = if (indice % 2 == 0) "Revisar temperatura y realizar limpieza preventiva."
                    else "Revisar el funcionamiento y las conexiones del equipo.",
                fechaProgramada = llegada.ifBlank { fecha(120L + indice * 60) },
                tipo = if (indice % 2 == 0) TipoOt.PREVENTIVA else TipoOt.CORRECTIVA,
                prioridad = listOf(PrioridadOt.ALTA, PrioridadOt.MEDIA, PrioridadOt.BAJA)[indice % 3],
                estado = estado,
                tecnico = if (estado == EstadoOt.CREADA) "" else if (indice % 2 == 0) "Brian Bravo" else "Técnico demo",
                checkIn = llegada, checkOut = salida,
                diagnostico = if (finalizada) "Filtro sucio" else "",
                actividad = if (finalizada) "Limpieza de filtros y revisión del funcionamiento." else "",
                materiales = if (finalizada) "Paño de limpieza · 1" else "",
                fechaConformidad = if (estado == EstadoOt.CERRADA) fecha(-2820) else "",
                ultimaActualizacion = ahora - indice * 1000L,
            )
        }
    }
}
