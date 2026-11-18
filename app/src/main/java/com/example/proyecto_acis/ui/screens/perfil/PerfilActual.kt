package com.example.proyecto_acis.ui.screens.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.proyecto_acis.R
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.ui.components.PanelSincronizacion
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel

@Composable
internal fun PerfilActual(perfil: Perfil, tecnico: String, viewModel: OrdenesViewModel, ids: Set<String>, onCambiar: () -> Unit,
    onDemo: (() -> Unit)? = null, mensajeDemo: String = "") {
    var confirmarDemo by rememberSaveable { mutableStateOf(false) }
    val preferencias by viewModel.preferencias.collectAsState()
    if (confirmarDemo && onDemo != null) AlertDialog(onDismissRequest = { confirmarDemo = false },
        title = { Text("¿Cargar órdenes de ejemplo?") },
        text = { Text("Se crearán seis órdenes de demostración para recorrer los perfiles. Solo está disponible cuando no hay órdenes guardadas.") },
        confirmButton = { TextButton(onClick = { onDemo(); confirmarDemo = false }) { Text("Cargar ejemplos") } },
        dismissButton = { TextButton(onClick = { confirmarDemo = false }) { Text("Cancelar") } })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
            Text("Acceso de demostración", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_acis_profile), contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Text(if (perfil == Perfil.TECNICO) tecnico else perfil.etiqueta,
                        style = MaterialTheme.typography.titleLarge)
                    Text(descripcionPerfil(perfil), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Recordar mi perfil", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium)
                        Switch(checked = preferencias.datos.recordarPerfil, enabled = !preferencias.cargando,
                            onCheckedChange = { activar -> viewModel.ejecutar { viewModel.recordarPerfil(activar) } },
                            modifier = Modifier.semantics { contentDescription = "Recordar mi perfil" })
                    }
                    Text("Al abrir ACIS, entra con el último perfil de demostración que elegiste. Puedes cambiarlo cuando quieras.",
                        style = MaterialTheme.typography.bodySmall)
                    if (preferencias.mensaje.isNotBlank()) Text(preferencias.mensaje,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Text("Almacenamiento local", style = MaterialTheme.typography.titleMedium)
            Text("Las órdenes, borradores y fotos se guardan en este dispositivo. Este acceso permite explorar las funciones de cada rol.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { com.example.proyecto_acis.ui.components.PanelSincronizacion(viewModel, ids) }
        if (onDemo != null) item {
            OutlinedButton(onClick = { confirmarDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Cargar órdenes de demostración") }
        }
        if (mensajeDemo.isNotBlank()) item { Text(mensajeDemo, style = MaterialTheme.typography.bodyMedium) }
        item { OutlinedButton(onClick = onCambiar, modifier = Modifier.fillMaxWidth()) { Text("Cambiar de perfil") } }
    }
}

internal fun descripcionPerfil(perfil: Perfil) = when (perfil) {
    Perfil.PLANIFICADOR -> "Crea, programa y asigna órdenes de trabajo."
    Perfil.TECNICO -> "Consulta tus asignaciones y registra las visitas e informes."
    Perfil.CLIENTE -> "Revisa los trabajos y registra tu conformidad u observaciones."
}
