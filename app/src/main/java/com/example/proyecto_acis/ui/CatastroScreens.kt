package com.example.proyecto_acis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.proyecto_acis.data.Catastro
import com.example.proyecto_acis.data.obtenerUbicacionCheckIn
import com.example.proyecto_acis.domain.ValidadorCatastro
import kotlinx.coroutines.launch

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
                    if (cliente.esDemo) Text("Registro de demostración", style = MaterialTheme.typography.bodySmall)
                    if (cliente.id == ValidadorCatastro.CLIENTE_PENDIENTE) {
                        Text("Edita cada local para indicar a qué cliente pertenece.")
                    } else OutlinedButton(onClick = { onEditar(cliente.id) }) { Text("Editar cliente") }
                }
            }
        }
    }
}

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
                        errorMapa = if (buscarLocalEnMapa(context, com.example.proyecto_acis.domain.DestinoMapa.consulta(local))) ""
                            else "No se pudo abrir el mapa."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Abrir en Maps") }
                }
            }
        }
    }
}

@Composable
internal fun FormularioCliente(viewModel: CatastroViewModel, onVolver: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(if (viewModel.datosCliente.id == null) "Nuevo cliente" else "Editar cliente",
                style = MaterialTheme.typography.headlineMedium) }
            item { CampoFormulario(viewModel.datosCliente.nombre, viewModel::cambiarNombreCliente,
                "Nombre del cliente", viewModel.errorCliente, ejemplo = "Ej. Comercial del Sur") }
            if (viewModel.mensajeCliente.isNotBlank()) item { Text(viewModel.mensajeCliente, color = MaterialTheme.colorScheme.error) }
        }
        AccionesCatastro(onVolver) { focus.clearFocus(); if (viewModel.guardarCliente()) onVolver() }
    }
}

@Composable
internal fun FormularioLocal(viewModel: CatastroViewModel, onVolver: () -> Unit) {
    val catastro by viewModel.catastro.collectAsState()
    val datos = viewModel.datosLocal
    val errores = viewModel.erroresLocal
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var abierto by remember { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }
    fun capturar() {
        if (buscando) return
        scope.launch {
            buscando = true
            try {
                val ubicacion = obtenerUbicacionCheckIn(context)
                if (ubicacion != null) viewModel.registrarUbicacion(ubicacion)
                else viewModel.informarUbicacion("No se obtuvo la ubicación. Activa la ubicación del teléfono o continúa sin ella.")
            } catch (_: SecurityException) {
                viewModel.informarUbicacion("No hay permiso de ubicación. Puedes continuar sin ella.")
            } finally { buscando = false }
        }
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) capturar()
        else viewModel.informarUbicacion("Permiso no concedido. Puedes guardar el local sin ubicación.")
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(if (datos.id == null) "Nuevo local" else "Editar local", style = MaterialTheme.typography.headlineMedium)
                Text("La dirección y la ubicación son opcionales.")
            }
            item {
                Text("Cliente", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(catastro.clientes.firstOrNull { it.id == datos.clienteId }?.nombre ?: "Seleccionar cliente")
                    }
                    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                        catastro.clientes.filter { it.id != ValidadorCatastro.CLIENTE_PENDIENTE }.forEach { cliente ->
                            DropdownMenuItem(text = { Text(cliente.nombre) }, onClick = {
                                viewModel.cambiarClienteLocal(cliente.id); abierto = false
                            })
                        }
                    }
                }
                errores.cliente?.let { ErrorSeleccion(it) }
            }
            item { CampoFormulario(datos.nombre, viewModel::cambiarNombreLocal, "Nombre del local", errores.nombre,
                ejemplo = "Ej. Sucursal Concepción centro") }
            item { CampoFormulario(datos.direccion, viewModel::cambiarDireccionLocal, "Dirección (opcional)",
                ejemplo = "Ej. Av. Prat 120, Concepción") }
            item {
                Text("Ubicación del local", style = MaterialTheme.typography.titleMedium)
                Text("Usa la ubicación del teléfono solo si estás en este local. El permiso entrega una ubicación aproximada.")
                Text(if (datos.latitud != null && datos.longitud != null) "Ubicación registrada" else "Sin ubicación registrada",
                    color = MaterialTheme.colorScheme.primary)
                OutlinedButton(enabled = !buscando, onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) capturar()
                    else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }, modifier = Modifier.fillMaxWidth()) { Text(if (buscando) "Buscando ubicación…" else "Usar mi ubicación para este local") }
                if (datos.latitud != null) TextButton(enabled = !buscando, onClick = viewModel::quitarUbicacion) { Text("Quitar ubicación") }
                if (buscando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (viewModel.mensajeUbicacion.isNotBlank()) Text(viewModel.mensajeUbicacion, style = MaterialTheme.typography.bodySmall)
                errores.ubicacion?.let { ErrorSeleccion(it) }
            }
            if (viewModel.mensajeLocal.isNotBlank()) item { Text(viewModel.mensajeLocal, color = MaterialTheme.colorScheme.error) }
        }
        AccionesCatastro(onVolver, enabled = !buscando) { focus.clearFocus(); if (viewModel.guardarLocal()) onVolver() }
    }
}

@Composable
private fun AccionesCatastro(onVolver: () -> Unit, enabled: Boolean = true, onGuardar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
        Button(enabled = enabled, onClick = onGuardar, modifier = Modifier.weight(1f)) { Text("Guardar") }
    }
}
