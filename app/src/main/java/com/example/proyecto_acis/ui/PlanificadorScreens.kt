package com.example.proyecto_acis.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.ResumenSla
import com.example.proyecto_acis.domain.EstadoSla
import com.example.proyecto_acis.domain.EquipoResumen
import com.example.proyecto_acis.domain.ResumenOperativo
import com.example.proyecto_acis.ui.theme.EstadoColors
import java.util.Locale

@Composable
internal fun EquiposPlanificador(equipos: List<EquipoResumen>, onCrear: (String, String) -> Unit,
    onNuevo: () -> Unit, onEditar: (String) -> Unit, onClientes: () -> Unit, onLocales: () -> Unit, mensaje: String = "") {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Catálogo", style = MaterialTheme.typography.headlineMedium)
            Text("Clientes, locales y equipos guardados en el dispositivo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClientes, modifier = Modifier.weight(1f)) { Text("Clientes") }
                OutlinedButton(onClick = onLocales, modifier = Modifier.weight(1f)) { Text("Locales") }
            }
        }
        item { Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth()) { Text("Agregar equipo") } }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
        if (equipos.isEmpty()) item {
            Text("Todavía no hay equipos para mostrar.")
        }
        items(equipos, key = { it.id }) { equipo ->
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(equipo.local, color = MaterialTheme.colorScheme.primary)
                    Text(equipo.cliente, style = MaterialTheme.typography.bodySmall)
                    Text("${equipo.tipo} · ${if (equipo.esExterior) "Exterior" else "Interior / sin especificar"}",
                        style = MaterialTheme.typography.bodySmall)
                    if (equipo.ubicacion.isNotBlank()) Text(equipo.ubicacion, style = MaterialTheme.typography.bodySmall)
                    Text(if (equipo.delCatalogoDemo) "Del catálogo de demostración" else "Registro local",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("${equipo.visitas} órdenes registradas", style = MaterialTheme.typography.bodyMedium)
                    Text(equipo.ultimaMantencion?.let { "Última mantención cerrada: $it" }
                        ?: "Sin mantenciones cerradas registradas.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onEditar(equipo.id) }, modifier = Modifier.fillMaxWidth()) { Text("Editar equipo") }
                    OutlinedButton(onClick = { onCrear(equipo.local, equipo.nombre) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Crear OT para este equipo")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReportePlanificador(resumen: ResumenOperativo, sla: ResumenSla, onMes: (Int) -> Unit, onSeleccionar: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Reportes", style = MaterialTheme.typography.headlineMedium)
            Text("Todos los registros de este dispositivo", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Cumplimiento de plazos", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onMes(-1) }) { Text("Anterior") }
                Text(sla.periodo, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { onMes(1) }) { Text("Siguiente") }
            }
            Text("Periodo según programación original. Los plazos se conservan al crear cada orden.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sla.porcentaje?.let { String.format(Locale.forLanguageTag("es-CL"), "%.1f%%", it) }
                        ?: "Sin resultados suficientes", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${sla.cumplidas} cumplidas de ${sla.evaluadas} evaluadas · ${sla.vencidas} vencidas")
                    Text(sla.minutosPromedio?.let { String.format(Locale.forLanguageTag("es-CL"), "Tiempo medio en sitio: %.1f min", it) }
                        ?: "Tiempo medio: sin eventos suficientes")
                    Text("${sla.visitasConTiempo} visitas con llegada e informe verificables en el periodo.", style = MaterialTheme.typography.bodySmall)
                    Text("${sla.pendientes} pendientes dentro de plazo · ${sla.sinDatos} sin datos suficientes",
                        style = MaterialTheme.typography.bodySmall)
                    Text("El porcentaje considera cumplidas y vencidas. Las pendientes y las órdenes sin acuerdo o eventos suficientes no cuentan como cumplidas.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Cumplimiento por cliente", style = MaterialTheme.typography.titleMedium) }
        if (sla.porCliente.isEmpty()) item { Text("No hay órdenes con un acuerdo registrado en este periodo.") }
        sla.porCliente.forEachIndexed { indice, cliente ->
            item(key = "cliente-sla-$indice") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cliente.nombre)
                    Text(cliente.porcentaje?.let { String.format(Locale.forLanguageTag("es-CL"), "%.1f%% · %d evaluadas", it, cliente.evaluadas) }
                        ?: "Sin resultados suficientes", style = MaterialTheme.typography.bodySmall)
                    cliente.porcentaje?.let { valor -> LinearProgressIndicator(progress = { (valor / 100).toFloat() }, modifier = Modifier.fillMaxWidth()) }
                    Text("${cliente.pendientes} pendientes dentro de plazo", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Órdenes en riesgo o vencidas por atender", style = MaterialTheme.typography.titleMedium) }
        if (sla.enRiesgo.isEmpty()) item { Text("No hay órdenes en estas condiciones para el periodo seleccionado.") }
        sla.enRiesgo.forEach { fila ->
            item(key = "sla-${fila.orden.id}") {
                OutlinedCard(onClick = { onSeleccionar(fila.orden.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${fila.orden.id} · ${fila.estado.etiqueta}", style = MaterialTheme.typography.titleSmall)
                        Text("${fila.orden.equipo} · ${fila.orden.acuerdoSla?.clienteNombre.orEmpty()}")
                        fila.proximoLimite?.let { limite ->
                            val fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL")).format(java.util.Date(limite))
                            Text("${fila.motivo}: $fecha", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Ver orden →", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { HorizontalDivider(); Text("Actividad general · todos los registros", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricaLocal("Órdenes", resumen.total.toString(), Modifier.weight(1f))
                MetricaLocal("Sin cerrar", resumen.abiertas.toString(), Modifier.weight(1f))
            }
        }
        item { MetricaLocal("Trabajos cerrados", resumen.cerradas.toString(), Modifier.fillMaxWidth()) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tiempo medio en sitio", style = MaterialTheme.typography.titleMedium)
                    Text(resumen.minutosPromedio?.let { String.format(Locale.forLanguageTag("es-CL"), "%.1f min", it) }
                        ?: "Sin datos suficientes", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${resumen.visitasConTiempo} visitas con llegada y salida válidas.", style = MaterialTheme.typography.bodySmall)
                    Text("Las visitas sin horarios completos no participan en este promedio.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Órdenes por estado", style = MaterialTheme.typography.titleLarge) }
        if (resumen.total == 0) item {
            Text("Crea una orden para comenzar a registrar actividad.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        resumen.porEstado.forEach { (estado, cantidad) ->
            item(key = estado.name) {
                Surface(color = EstadoColors.estado(estado).fondo, shape = MaterialTheme.shapes.small) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(estado.etiqueta, modifier = Modifier.weight(1f), color = EstadoColors.estado(estado).texto)
                        Text(cantidad.toString(), color = EstadoColors.estado(estado).texto)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricaLocal(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(titulo, style = MaterialTheme.typography.bodySmall)
        }
    }
}
