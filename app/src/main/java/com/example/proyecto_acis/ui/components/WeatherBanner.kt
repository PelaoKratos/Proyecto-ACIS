package com.example.proyecto_acis.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.ContextoClima
import com.example.proyecto_acis.ui.ClimaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherBanner(contexto: ContextoClima?, viewModel: ClimaViewModel) {
    if (contexto == null) return
    val solicitud = contexto.solicitud
    LaunchedEffect(solicitud) { if (solicitud != null) viewModel.consultar(solicitud) }
    val estado = solicitud?.let(viewModel::estado)
    val resultado = estado?.resultado
    val hora = resultado?.hora
    val alerta = hora?.hayAlerta == true
    val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("es-CL")) }
    Card(Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (alerta) Color(0xFFFFD07A) else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(if (alerta) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info),
                    contentDescription = null, tint = if (alerta) Color(0xFFFFD07A) else MaterialTheme.colorScheme.primary)
                Text(if (alerta) "Atención al clima · equipo exterior" else "Clima · equipo exterior",
                    style = MaterialTheme.typography.titleSmall)
            }
            Text(contexto.local, style = MaterialTheme.typography.bodyMedium)
            if (solicitud != null) Text("Visita: ${formato.format(Date(solicitud.instante))}",
                style = MaterialTheme.typography.bodySmall)
            if (contexto.aviso.isNotBlank()) Text(contexto.aviso)
            if (estado?.cargando == true) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Consultando pronóstico…", style = MaterialTheme.typography.bodySmall)
            }
            if (hora != null) {
                Text("Lluvia: ${hora.lluvia}% · Viento: ${String.format(Locale.forLanguageTag("es-CL"), "%.1f", hora.viento)} km/h")
                Text(if (alerta) "Revisa las condiciones antes de realizar el trabajo exterior."
                    else "Sin alertas por lluvia o viento según los umbrales definidos.", style = MaterialTheme.typography.bodySmall)
            }
            if (resultado?.aviso?.isNotBlank() == true) Text(resultado.aviso)
            if (resultado?.enCache == true && hora != null) Text("No se pudo actualizar. Se muestra la última consulta guardada.")
            resultado?.actualizado?.let {
                Text("Consulta: ${formato.format(Date(it))} · Open-Meteo", style = MaterialTheme.typography.bodySmall)
            }
            if (solicitud != null) TextButton(enabled = estado?.cargando != true,
                onClick = { viewModel.consultar(solicitud, forzar = true) }) { Text("Actualizar clima") }
            Text("Pronóstico orientativo. Puedes continuar con la orden.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
