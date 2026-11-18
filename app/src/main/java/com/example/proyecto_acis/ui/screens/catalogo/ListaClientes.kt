package com.example.proyecto_acis.ui.screens.catalogo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.model.Catastro
import com.example.proyecto_acis.domain.validation.ValidadorCatastro

@Composable
internal fun ListaClientes(catastro: Catastro, onNuevo: () -> Unit, onEditar: (String) -> Unit, mensaje: String = "") {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Clientes", style = MaterialTheme.typography.headlineMedium)
            Text("Organiza las empresas o personas a las que prestas servicio.")
        }
        item { Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth()) { Text("Agregar cliente") } }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
        if (catastro.clientes.isEmpty()) item { Text("Agrega tu primer cliente para registrar sus locales.") }
        items(catastro.clientes, key = { it.id }) { cliente ->
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cliente.nombre, style = MaterialTheme.typography.titleMedium)
                    val cantidad = catastro.locales.count { it.clienteId == cliente.id }
                    Text(if (cantidad == 1) "1 local registrado" else "$cantidad locales registrados")
                    Text(if (cliente.horasLlegada != null && cliente.horasResolucion != null)
                        "Plazos: llegada ${cliente.horasLlegada} h · resolución ${cliente.horasResolucion} h"
                        else "Sin plazos de servicio configurados", style = MaterialTheme.typography.bodySmall)
                    if (cliente.esDemo) Text("Registro de demostración", style = MaterialTheme.typography.bodySmall)
                    if (cliente.id == ValidadorCatastro.CLIENTE_PENDIENTE) {
                        Text("Edita cada local para indicar a qué cliente pertenece.")
                    } else OutlinedButton(onClick = { onEditar(cliente.id) }) { Text("Editar cliente") }
                }
            }
        }
    }
}
