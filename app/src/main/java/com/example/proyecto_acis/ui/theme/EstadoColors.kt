package com.example.proyecto_acis.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.PrioridadOt

data class ColorChip(val fondo: Color, val texto: Color)

object EstadoColors {
    fun estado(valor: EstadoOt): ColorChip = when (valor) {
        EstadoOt.CREADA, EstadoOt.ASIGNADA -> ColorChip(Color(0xFF2B302E), Color(0xFFD2D7D2))
        EstadoOt.ACEPTADA -> ColorChip(Color(0xFF143455), Color(0xFF9ACBFF))
        EstadoOt.EN_VISITA -> ColorChip(Color(0xFF493410), Color(0xFFFFD07A))
        EstadoOt.PENDIENTE_CONFORMIDAD -> ColorChip(Color(0xFF392B50), Color(0xFFD8BAFF))
        EstadoOt.OBSERVADA -> ColorChip(Color(0xFF4D2628), Color(0xFFFFA6A6))
        EstadoOt.CERRADA -> ColorChip(Color(0xFF1F3D29), Color(0xFFA5E6B4))
    }

    fun prioridad(valor: PrioridadOt): ColorChip = when (valor) {
        PrioridadOt.ALTA -> ColorChip(Color(0xFF4D2628), Color(0xFFFFA6A6))
        PrioridadOt.MEDIA -> ColorChip(Color(0xFF493410), Color(0xFFFFD07A))
        PrioridadOt.BAJA, PrioridadOt.SIN_ESPECIFICAR -> ColorChip(Color(0xFF2B302E), Color(0xFFD2D7D2))
    }
}
