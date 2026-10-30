package com.example.proyecto_acis.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.data.EstadoSync
import com.example.proyecto_acis.data.RegistroSync
import com.example.proyecto_acis.ui.OrdenesViewModel

@Composable
fun SyncStatusPill(registro: RegistroSync?) {
    val estado = registro?.estado ?: EstadoSync.LOCAL
    Surface(color = if (estado == EstadoSync.ERROR) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(10.dp)) {
            Text(estado.etiqueta, style = MaterialTheme.typography.labelMedium)
            Text("Simulación · sin servidor", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PanelSincronizacion(viewModel: OrdenesViewModel, ids: Set<String>) {
    val registros by viewModel.sincronizacion.collectAsState()
    val visibles = registros.filter { it.otId in ids }
    var fallar by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sincronización simulada", style = MaterialTheme.typography.titleMedium)
            Text("Prueba de funcionamiento. No hay servidor y no se envían órdenes, fotos ni ubicaciones por esta opción.")
            EstadoSync.entries.forEach { estado ->
                Text("${estado.etiqueta}: ${visibles.count { it.estado == estado }}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Checkbox(checked = fallar, enabled = !viewModel.simulandoEnvio, onCheckedChange = { fallar = it },
                    modifier = Modifier.semantics { contentDescription = "Simular un fallo de envío" })
                Text("Simular un fallo de envío", modifier = Modifier.padding(top = 12.dp))
            }
            Button(onClick = { viewModel.simularEnvio(ids, fallar) },
                enabled = !viewModel.simulandoEnvio && visibles.any { it.estado != EstadoSync.ENVIADO },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (viewModel.simulandoEnvio) "Simulando envío…" else "Simular envío / reintentar")
            }
            if (viewModel.simulandoEnvio) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (visibles.isEmpty()) Text("No hay órdenes disponibles para este perfil.")
            if (viewModel.mensajeSync.isNotBlank()) Text(viewModel.mensajeSync, style = MaterialTheme.typography.bodySmall)
            Text("Los cambios nuevos vuelven a quedar pendientes. Puedes seguir trabajando mientras se simula el envío.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
