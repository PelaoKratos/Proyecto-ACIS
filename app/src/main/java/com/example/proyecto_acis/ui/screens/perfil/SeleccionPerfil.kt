package com.example.proyecto_acis.ui.screens.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.proyecto_acis.R
import com.example.proyecto_acis.domain.model.Perfil

@Composable
internal fun SeleccionPerfil(onSeleccionar: (Perfil) -> Unit) {
    val colores = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("ACIS", color = colores.primary, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("Servicio técnico", color = colores.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(28.dp))
            Text("Elige un perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Tres accesos de demostración para explorar el servicio desde cada rol.",
                style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ACCESOS / ESTE DISPOSITIVO", color = colores.primary,
                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                Text("03 PERFILES", color = colores.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
        }
        Perfil.entries.forEach { perfil ->
            item {
                val icono = when (perfil) {
                    Perfil.PLANIFICADOR -> R.drawable.ic_acis_records
                    Perfil.TECNICO -> R.drawable.ic_acis_tools
                    Perfil.CLIENTE -> R.drawable.ic_acis_profile
                }
                val categoria = when (perfil) {
                    Perfil.PLANIFICADOR -> "GESTIÓN / ÓRDENES"
                    Perfil.TECNICO -> "OPERACIÓN / VISITAS"
                    Perfil.CLIENTE -> "REVISIÓN / SERVICIOS"
                }
                val accion = when (perfil) {
                    Perfil.PLANIFICADOR -> "Administrar órdenes"
                    Perfil.TECNICO -> "Ver trabajos"
                    Perfil.CLIENTE -> "Ver servicios"
                }
                Card(onClick = { onSeleccionar(perfil) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colores.surface),
                    border = BorderStroke(1.dp, colores.outlineVariant), shape = MaterialTheme.shapes.small) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = colores.primaryContainer, shape = MaterialTheme.shapes.small) {
                                Icon(painterResource(icono), contentDescription = null, tint = colores.primary,
                                    modifier = Modifier.padding(10.dp).size(24.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(categoria, style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace, color = colores.onSurfaceVariant)
                                Text(perfil.etiqueta, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(descripcionPerfil(perfil), style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurfaceVariant)
                        Surface(color = colores.primaryContainer, shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(accion, style = MaterialTheme.typography.labelLarge, color = colores.primary)
                                Text("→", style = MaterialTheme.typography.titleLarge, color = colores.primary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(top = 14.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("▪  Modo demostración", style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace, color = colores.onSurfaceVariant)
                Text("▯  Los datos se guardan en este dispositivo.", style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant)
            }
        }
    }
}
