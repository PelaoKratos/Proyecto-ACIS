package com.example.proyecto_acis.ui.screens.catalogo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.proyecto_acis.device.location.obtenerUbicacionCheckIn
import com.example.proyecto_acis.domain.model.Cliente
import com.example.proyecto_acis.domain.validation.ValidadorCatastro
import com.example.proyecto_acis.ui.components.AccionesCatastro
import com.example.proyecto_acis.ui.components.CampoFormulario
import com.example.proyecto_acis.ui.components.ErrorSeleccion
import com.example.proyecto_acis.ui.viewmodel.CatastroViewModel
import kotlinx.coroutines.launch

@Composable
internal fun FormularioLocal(viewModel: CatastroViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    val catastro by viewModel.catastro.collectAsState()
    val datos = estado.local.datos
    val errores = estado.local.errores
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
                if (estado.local.mensajeUbicacion.isNotBlank()) Text(estado.local.mensajeUbicacion, style = MaterialTheme.typography.bodySmall)
                errores.ubicacion?.let { ErrorSeleccion(it) }
            }
            if (estado.local.mensaje.isNotBlank()) item { Text(estado.local.mensaje, color = MaterialTheme.colorScheme.error) }
        }
        AccionesCatastro(onVolver, enabled = !buscando) { viewModel.ejecutar { focus.clearFocus(); if (viewModel.guardarLocal()) onVolver() } }
    }
}
