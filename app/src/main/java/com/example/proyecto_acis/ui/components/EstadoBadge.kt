package com.example.proyecto_acis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.ui.theme.EstadoColors

@Composable
internal fun EstadoBadge(estado: EstadoOt) {
    val colores = EstadoColors.estado(estado)
    BadgeTexto(estado.etiqueta, colores.fondo, colores.texto)
}

@Composable
internal fun PrioridadBadge(prioridad: PrioridadOt) {
    val colores = EstadoColors.prioridad(prioridad)
    BadgeTexto("Prioridad ${prioridad.etiqueta.lowercase()}", colores.fondo, colores.texto)
}

@Composable
internal fun BadgeTexto(etiqueta: String, fondo: Color, texto: Color) {
    val fondoAnimado by animateColorAsState(fondo, label = "fondo del estado")
    val textoAnimado by animateColorAsState(texto, label = "texto del estado")
    Surface(
        color = fondoAnimado,
        contentColor = textoAnimado,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            etiqueta,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
