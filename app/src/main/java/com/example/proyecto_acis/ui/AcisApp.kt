package com.example.proyecto_acis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.proyecto_acis.ui.theme.ProyectoACISTheme

private data class OrdenTrabajoEjemplo(
    val id: String,
    val local: String,
    val equipo: String,
    val tipo: String,
    val prioridad: String,
    val fecha: String,
    val estado: String,
    val descripcion: String,
)

private val ordenesDeEjemplo = listOf(
    OrdenTrabajoEjemplo(
        id = "OT-1001",
        local = "Supermercado Norte",
        equipo = "Unidad condensadora UC-01",
        tipo = "Mantención preventiva",
        prioridad = "Media",
        fecha = "8 oct, 09:00",
        estado = "Asignada",
        descripcion = "Revisar funcionamiento y limpiar filtros.",
    ),
    OrdenTrabajoEjemplo(
        id = "OT-1002",
        local = "Tienda Centro",
        equipo = "Equipo de climatización CL-04",
        tipo = "Reparación",
        prioridad = "Alta",
        fecha = "8 oct, 12:00",
        estado = "Aceptada",
        descripcion = "Revisar ruido informado por el local.",
    ),
    OrdenTrabajoEjemplo(
        id = "OT-1003",
        local = "Supermercado Sur",
        equipo = "Cámara frigorífica CF-02",
        tipo = "Mantención preventiva",
        prioridad = "Baja",
        fecha = "9 oct, 10:30",
        estado = "Asignada",
        descripcion = "Inspeccionar el equipo y registrar su estado.",
    ),
)

@Composable
fun AcisApp(modifier: Modifier = Modifier) {
    var ordenSeleccionadaId by rememberSaveable { mutableStateOf<String?>(null) }
    val ordenSeleccionada = ordenesDeEjemplo.firstOrNull { it.id == ordenSeleccionadaId }

    BackHandler(enabled = ordenSeleccionada != null) {
        ordenSeleccionadaId = null
    }

    if (ordenSeleccionada == null) {
        ListaOrdenes(
            onOrdenSeleccionada = { ordenSeleccionadaId = it },
            modifier = modifier,
        )
    } else {
        DetalleOrden(
            orden = ordenSeleccionada,
            onVolver = { ordenSeleccionadaId = null },
            modifier = modifier,
        )
    }
}

@Composable
private fun ListaOrdenes(
    onOrdenSeleccionada: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = "ACIS",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mis órdenes de trabajo",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Vista del técnico · Datos de ejemplo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(ordenesDeEjemplo, key = { it.id }) { orden ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOrdenSeleccionada(orden.id) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${orden.id} · ${orden.estado}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = orden.local,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = orden.equipo, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${orden.fecha} · Prioridad ${orden.prioridad.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetalleOrden(
    orden: OrdenTrabajoEjemplo,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = onVolver) {
                Text("← Volver a mis órdenes")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = orden.id,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = orden.estado,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CampoDetalle("Local", orden.local)
                    CampoDetalle("Equipo", orden.equipo)
                    CampoDetalle("Tipo", orden.tipo)
                    CampoDetalle("Prioridad", orden.prioridad)
                    CampoDetalle("Fecha programada", orden.fecha)
                    CampoDetalle("Descripción", orden.descripcion)
                }
            }
        }
    }
}

@Composable
private fun CampoDetalle(etiqueta: String, valor: String) {
    Column {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun AcisAppPreview() {
    ProyectoACISTheme {
        AcisApp()
    }
}
