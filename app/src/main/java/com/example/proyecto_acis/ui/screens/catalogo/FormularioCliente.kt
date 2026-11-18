package com.example.proyecto_acis.ui.screens.catalogo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.ui.components.AccionesCatastro
import com.example.proyecto_acis.ui.components.CampoFormulario
import com.example.proyecto_acis.ui.viewmodel.CatastroViewModel

@Composable
internal fun FormularioCliente(viewModel: CatastroViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    val datos = estado.cliente.datos
    val errores = estado.cliente.errores
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(if (datos.id == null) "Nuevo cliente" else "Editar cliente",
                style = MaterialTheme.typography.headlineMedium) }
            item { CampoFormulario(datos.nombre, viewModel::cambiarNombreCliente,
                "Nombre del cliente", errores.nombre, ejemplo = "Ej. Comercial del Sur") }
            item {
                Text("Plazos de servicio (SLA) · opcionales", style = MaterialTheme.typography.titleMedium)
                Text("Horas desde la programación original de la visita. Deja ambos vacíos si no hay acuerdo. Los cambios se aplican a nuevas órdenes.")
            }
            item { CampoFormulario(datos.horasLlegada, viewModel::cambiarHorasLlegada,
                "Horas para llegar", errores.llegada, ejemplo = "Ej. 4", tipoTeclado = androidx.compose.ui.text.input.KeyboardType.Number) }
            item { CampoFormulario(datos.horasResolucion, viewModel::cambiarHorasResolucion,
                "Horas para resolver", errores.resolucion, ejemplo = "Ej. 24", tipoTeclado = androidx.compose.ui.text.input.KeyboardType.Number) }
            if (estado.cliente.mensaje.isNotBlank()) item { Text(estado.cliente.mensaje, color = MaterialTheme.colorScheme.error) }
        }
        AccionesCatastro(onVolver) { viewModel.ejecutar { focus.clearFocus(); if (viewModel.guardarCliente()) onVolver() } }
    }
}
