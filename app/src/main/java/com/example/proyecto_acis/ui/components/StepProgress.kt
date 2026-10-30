package com.example.proyecto_acis.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StepProgress(paso: Int, total: Int) {
    val progreso by animateFloatAsState(paso.toFloat() / total, label = "progreso de la orden")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Paso $paso de $total", color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
    }
}
