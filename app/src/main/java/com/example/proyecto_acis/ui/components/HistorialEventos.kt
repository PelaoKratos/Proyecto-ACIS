package com.example.proyecto_acis.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.data.EventoOt
import com.example.proyecto_acis.data.Perfil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorialEventos(eventos: List<EventoOt>, hitosAnteriores: List<Pair<String, String>> = emptyList()) {
    var completo by rememberSaveable(eventos.firstOrNull()?.otId) { mutableStateOf(false) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("es-CL")) }
    Card(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Historial de la orden", style = MaterialTheme.typography.titleMedium)
            if (hitosAnteriores.isNotEmpty()) {
                Text("Horarios conservados del registro anterior", style = MaterialTheme.typography.labelLarge)
                hitosAnteriores.forEach { (titulo, fecha) ->
                    Text("$titulo · $fecha", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
            (if (completo) eventos else eventos.takeLast(5)).forEach { evento ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("●", color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(evento.tipo.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(formato.format(Date(evento.fechaMillis)), style = MaterialTheme.typography.bodySmall)
                        val rol = Perfil.entries.firstOrNull { it.name == evento.rolActor }?.etiqueta ?: "Sistema"
                        Text("${evento.actor} · $rol", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (evento.detalle.isNotBlank()) Text(evento.detalle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (eventos.size > 5) TextButton(onClick = { completo = !completo }) {
                Text(if (completo) "Mostrar últimos eventos" else "Ver los ${eventos.size} eventos")
            }
        }
    }
}
