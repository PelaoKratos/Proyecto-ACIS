package com.example.proyecto_acis.domain

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale

object TiempoVisita {
    fun minutos(llegada: String, salida: String = "", ahora: Long = System.currentTimeMillis()): Long? {
        val inicio = fecha(llegada) ?: return null
        val fin = if (salida.isBlank()) ahora else fecha(salida) ?: return null
        if (fin < inicio) return null
        return (fin - inicio) / 60_000
    }

    fun fecha(texto: String): Long? {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL")).apply { isLenient = false }
        val posicion = ParsePosition(0)
        val fecha = formato.parse(texto, posicion)
        return fecha?.time?.takeIf { posicion.index == texto.length }
    }
}
