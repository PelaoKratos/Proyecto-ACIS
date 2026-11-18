package com.example.proyecto_acis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AccionesCatastro(onVolver: () -> Unit, enabled: Boolean = true, onGuardar: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
        Button(enabled = enabled, onClick = onGuardar, modifier = Modifier.weight(1f)) { Text("Guardar") }
    }
}
