package com.example.proyecto_acis.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FormularioEquipo(viewModel: OrdenesViewModel, onVolver: () -> Unit) {
    val focus = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val catastro by viewModel.catastro.collectAsState()
    val datos = viewModel.datosEquipo
    val errores = viewModel.erroresEquipo
    var abierto by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(if (datos.id == null) "Nuevo equipo" else "Editar equipo", style = MaterialTheme.typography.headlineMedium)
                Text("Identifica el equipo y el local al que pertenece.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Local", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(catastro.locales.firstOrNull { it.id == datos.localId }?.nombre ?: "Seleccionar local")
                    }
                    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                        catastro.locales.forEach { local ->
                            DropdownMenuItem(text = { Text(local.nombre) }, onClick = {
                                viewModel.cambiarLocalEquipo(local.id); abierto = false
                            })
                        }
                    }
                }
                errores.local?.let { ErrorSeleccion(it) }
                catastro.locales.firstOrNull { it.id == datos.localId }?.let { local ->
                    catastro.clientes.firstOrNull { it.id == local.clienteId }?.let { cliente ->
                        Text("Cliente: ${cliente.nombre}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { CampoFormulario(datos.nombre, viewModel::cambiarNombreEquipo, "Nombre del equipo", errores.nombre,
                ejemplo = "Ej. Refrigerador de bebidas") }
            item { CampoFormulario(datos.tipo, viewModel::cambiarTipoEquipo, "Tipo de equipo", errores.tipo,
                ejemplo = "Ej. Refrigeración comercial") }
            item { CampoFormulario(datos.ubicacion, viewModel::cambiarUbicacionEquipo, "Ubicación en el local (opcional)",
                ejemplo = "Ej. Pasillo de bebidas") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Instalado en exterior", modifier = Modifier.weight(1f))
                    Switch(checked = datos.esExterior, onCheckedChange = viewModel::cambiarExteriorEquipo,
                        modifier = Modifier.semantics { contentDescription = "Equipo instalado en exterior" })
                }
                Text("La última mantención se actualiza al aprobar un trabajo realizado.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (viewModel.mensajeEquipo.isNotBlank()) item {
                Text(viewModel.mensajeEquipo, color = MaterialTheme.colorScheme.error)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = {
                focus.clearFocus(); teclado?.hide()
                if (viewModel.guardarEquipo()) onVolver()
            }, modifier = Modifier.weight(1f)) { Text("Guardar equipo") }
        }
    }
}
