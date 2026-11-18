package com.example.proyecto_acis.ui.screens.catalogo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.device.maps.buscarLocalEnMapa
import com.example.proyecto_acis.domain.model.Catastro
import com.example.proyecto_acis.domain.service.DestinoMapa

@Composable
internal fun ListaLocales(catastro: Catastro, onNuevo: () -> Unit, onEditar: (String) -> Unit, mensaje: String = "") {
    val context = LocalContext.current
    var errorMapa by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Locales", style = MaterialTheme.typography.headlineMedium)
            Text("Guarda la dirección para facilitar las visitas del técnico.")
        }
        item { Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth()) { Text("Agregar local") } }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
        if (errorMapa.isNotBlank()) item { Text(errorMapa, color = MaterialTheme.colorScheme.error) }
        if (catastro.locales.isEmpty()) item { Text("Todavía no hay locales. Registra primero un cliente.") }
        items(catastro.locales, key = { it.id }) { local ->
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(local.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(catastro.clientes.firstOrNull { it.id == local.clienteId }?.nombre.orEmpty(),
                        color = MaterialTheme.colorScheme.primary)
                    Text(local.direccion.ifBlank { "Sin dirección registrada" })
                    Text(if (local.latitud != null && local.longitud != null) "Ubicación guardada" else "Sin ubicación guardada",
                        style = MaterialTheme.typography.bodySmall)
                    val cantidad = catastro.equipos.count { it.localId == local.id }
                    Text(if (cantidad == 1) "1 equipo registrado" else "$cantidad equipos registrados")
                    OutlinedButton(onClick = { onEditar(local.id) }, modifier = Modifier.fillMaxWidth()) { Text("Editar local") }
                    TextButton(onClick = {
                        errorMapa = if (buscarLocalEnMapa(context, com.example.proyecto_acis.domain.service.DestinoMapa.consulta(local))) ""
                            else "No se pudo abrir el mapa."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Abrir en Maps") }
                }
            }
        }
    }
}
