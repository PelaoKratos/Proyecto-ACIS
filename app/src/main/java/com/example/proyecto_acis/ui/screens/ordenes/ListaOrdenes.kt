package com.example.proyecto_acis.ui.screens.ordenes

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.ui.components.EstadoBadge
import com.example.proyecto_acis.ui.components.PrioridadBadge
import com.example.proyecto_acis.ui.theme.EstadoColors

@Composable
internal fun ListaOrdenes(
    ordenes: List<OrdenTrabajo>,
    perfil: Perfil,
    onCrear: () -> Unit,
    onSeleccionar: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (perfil) {
                                Perfil.PLANIFICADOR -> "Órdenes de trabajo"
                                Perfil.TECNICO -> "Mis órdenes"
                                Perfil.CLIENTE -> "Revisión de trabajos"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(if (perfil == Perfil.TECNICO) "Órdenes más nuevas primero" else "Actividad más reciente primero", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium) {
                        Text("${ordenes.size}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (perfil == Perfil.PLANIFICADOR) {
                    Button(onClick = onCrear, modifier = Modifier.fillMaxWidth()) { Text("Crear nueva OT") }
                }
            }
        }
        if (ordenes.isEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sin órdenes por ahora", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (perfil) {
                                Perfil.PLANIFICADOR -> "Crea la primera orden para comenzar."
                                Perfil.TECNICO -> "Cuando te asignen un trabajo, aparecerá aquí."
                                Perfil.CLIENTE -> "Cuando haya trabajos para revisar, aparecerán aquí."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(ordenes, key = { it.id }) { orden ->
            Card(
                onClick = { onSeleccionar(orden.id) }, modifier = Modifier.fillMaxWidth().drawWithContent {
                    drawContent()
                    drawRect(EstadoColors.prioridad(orden.prioridad).texto,
                        topLeft = Offset.Zero, size = Size(3.dp.toPx(), size.height))
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(orden.id, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        AnimatedContent(targetState = orden.estado, label = "estado de OT") { estado ->
                            EstadoBadge(estado)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(orden.equipo, style = MaterialTheme.typography.titleLarge)
                        Text(orden.local, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(orden.tipo.etiqueta, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(orden.fechaProgramada, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        PrioridadBadge(orden.prioridad)
                    }
                }
            }
        }
    }
}
