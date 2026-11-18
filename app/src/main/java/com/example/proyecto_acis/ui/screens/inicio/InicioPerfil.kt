package com.example.proyecto_acis.ui.screens.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.proyecto_acis.device.maps.buscarLocalEnMapa
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.service.DestinoMapa
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.ui.components.WeatherBanner
import com.example.proyecto_acis.ui.theme.EstadoColors
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.FiltroOt
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun InicioPerfil(perfil: Perfil, ordenes: List<OrdenTrabajo>, viewModel: OrdenesViewModel, climaViewModel: ClimaViewModel, onRegistros: () -> Unit,
    onCrear: () -> Unit, onSeleccionar: (String) -> Unit) {
    val catastro by viewModel.catastro.collectAsState()
    val context = LocalContext.current
    val siguienteVisita = viewModel.siguienteVisita(ordenes)
    var errorMapa by rememberSaveable { mutableStateOf("") }
    val pendientes = ordenes.count { it.estado != EstadoOt.CERRADA }
    val porRevisar = ordenes.count { it.estado == EstadoOt.PENDIENTE_CONFORMIDAD }
    var filtro by rememberSaveable { mutableStateOf(FiltroOt.TODAS) }
    val mostradas = when (perfil) {
        Perfil.PLANIFICADOR -> viewModel.filtrarOrdenes(ordenes, filtro)
        Perfil.TECNICO -> viewModel.jornada(ordenes)
        Perfil.CLIENTE -> ordenes
    }
    val primeraRevision = viewModel.primeraRevision(ordenes)
    var ordenRechazo by rememberSaveable { mutableStateOf<String?>(null) }
    if (ordenRechazo != null) AlertDialog(onDismissRequest = { ordenRechazo = null },
        title = { Text("¿Rechazar esta orden?") },
        text = { Text("La orden volverá al planificador para reasignarla.") },
        confirmButton = { TextButton(onClick = { viewModel.ejecutar { ordenRechazo?.let { viewModel.rechazar(it) }; ordenRechazo = null  } }) { Text("Rechazar") } },
        dismissButton = { TextButton(onClick = { ordenRechazo = null }) { Text("Cancelar") } })

    val sinAsignar = ordenes.count { it.estado == EstadoOt.CREADA }
    val enCurso = ordenes.count { it.estado == EstadoOt.ACEPTADA || it.estado == EstadoOt.EN_VISITA }
    val fechaHoy = remember { SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CL")).format(Date()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(when (perfil) { Perfil.PLANIFICADOR -> "Control de órdenes"; Perfil.TECNICO -> "Mi jornada"; Perfil.CLIENTE -> "Tus servicios" },
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(fechaHoy.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Resumen de actividad", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("%02d".format(if (perfil == Perfil.CLIENTE) porRevisar else if (perfil == Perfil.PLANIFICADOR) sinAsignar else pendientes),
                                style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Text(if (perfil == Perfil.CLIENTE) "Por revisar" else if (perfil == Perfil.PLANIFICADOR) "Sin asignar" else "Sin cerrar", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("%02d".format(if (perfil == Perfil.PLANIFICADOR) enCurso else ordenes.size), style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(if (perfil == Perfil.PLANIFICADOR) "En curso" else "Registros", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (perfil == Perfil.PLANIFICADOR) {
                        Text("${viewModel.programadasVencidas(ordenes)} órdenes con fecha programada vencida",
                            style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFD07A))
                        Text("Se consideran las órdenes que siguen sin cerrar.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Vista de ${perfil.etiqueta.lowercase()}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (perfil == Perfil.CLIENTE && primeraRevision != null) item {
            Card(onClick = { onSeleccionar(primeraRevision.id) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$porRevisar por revisar →", style = MaterialTheme.typography.titleMedium)
                    Text("${primeraRevision.equipo} · informe listo", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (perfil == Perfil.CLIENTE && viewModel.ultimasMantenciones(ordenes).isNotEmpty()) {
            item { Text("Últimas mantenciones por equipo", style = MaterialTheme.typography.titleLarge) }
            viewModel.ultimasMantenciones(ordenes).forEach { servicio ->
                item(key = "mantencion-${servicio.id}") {
                    Card(onClick = { onSeleccionar(servicio.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(servicio.equipo, style = MaterialTheme.typography.titleMedium)
                            Text(servicio.local, style = MaterialTheme.typography.bodySmall)
                            Text("Última visita: ${servicio.checkOut}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR) item {
            Button(onClick = onCrear, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                Text("+   Crear nueva OT", fontWeight = FontWeight.Bold)
            }
        }
        if (perfil == Perfil.PLANIFICADOR) {
            item {
                Text("Clima de las visitas", style = MaterialTheme.typography.titleLarge)
                Text("Se consulta para equipos exteriores con la ubicación del local. Alertas: lluvia desde 60% o viento desde 40 km/h.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (viewModel.proximasExteriores(ordenes, catastro).isEmpty()) {
                    Text("No hay órdenes exteriores pendientes. En Catálogo, edita el equipo y marca Instalado en exterior; luego registra la ubicación en Locales.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            viewModel.proximasExteriores(ordenes, catastro).forEach { orden ->
                item(key = "clima-${orden.id}") {
                    Text("${orden.id} · ${orden.equipo}", style = MaterialTheme.typography.labelLarge)
                    WeatherBanner(
                        ReglasClima.orden(orden, catastro), climaViewModel)
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR) item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FiltroOt.entries.forEach { opcion ->
                    FilterChip(selected = filtro == opcion, onClick = { filtro = opcion },
                        label = { Text(when (opcion) {
                            FiltroOt.TODAS -> "Todas"
                            FiltroOt.SIN_ASIGNAR -> "Sin asignar"
                            FiltroOt.EN_VISITA -> "En visita"
                            FiltroOt.OBSERVADAS -> "Observadas"
                        }) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (perfil == Perfil.CLIENTE) "Historial de servicios" else if (perfil == Perfil.TECNICO) "Visitas de hoy" else "Órdenes recientes", style = MaterialTheme.typography.titleLarge)
                Text("$pendientes sin cerrar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (mostradas.isEmpty()) item {
            Text(if (perfil == Perfil.TECNICO) "No tienes visitas programadas para hoy. Consulta las demás en Registros." else if (ordenes.isEmpty()) "Aún no hay registros para mostrar." else "No hay órdenes en este filtro.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (errorMapa.isNotBlank()) item { Text(errorMapa, color = MaterialTheme.colorScheme.error) }
        mostradas.take(if (perfil == Perfil.TECNICO) Int.MAX_VALUE else 3).forEach { orden ->
            item(key = orden.id) {
                Card(onClick = { onSeleccionar(orden.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (perfil == Perfil.TECNICO && orden.id == siguienteVisita?.id) 2.dp else 1.dp,
                        if (perfil == Perfil.TECNICO && orden.id == siguienteVisita?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (perfil == Perfil.TECNICO && orden.id == siguienteVisita?.id) {
                            Text(if (orden.estado == EstadoOt.EN_VISITA) "Visita en curso" else "Siguiente visita",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(orden.id, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace)
                            Text("Prioridad ${orden.prioridad.etiqueta.lowercase()}", style = MaterialTheme.typography.labelSmall,
                                color = EstadoColors.prioridad(orden.prioridad).texto)
                        }
                        Text(orden.equipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${orden.local} · ${orden.fechaProgramada}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = EstadoColors.estado(orden.estado).fondo, shape = MaterialTheme.shapes.small) {
                                Text(orden.estado.etiqueta, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    color = EstadoColors.estado(orden.estado).texto, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("→", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                        }
                        if (orden.checkOut.isNotBlank()) {
                            TiempoVisita.minutos(orden.checkIn, orden.checkOut)?.let { duracion ->
                                Text("$duracion min en sitio", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { ordenRechazo = orden.id }, modifier = Modifier.weight(1f)) { Text("Rechazar") }
                                Button(onClick = { viewModel.ejecutar { viewModel.aceptar(orden.id)  } }, modifier = Modifier.weight(1f)) { Text("Aceptar") }
                            }
                        }
                        if (perfil == Perfil.TECNICO) WeatherBanner(
                            ReglasClima.orden(orden, catastro), climaViewModel)
                        if (perfil == Perfil.TECNICO && orden.id == siguienteVisita?.id) {
                            OutlinedButton(onClick = {
                                errorMapa = if (buscarLocalEnMapa(context, com.example.proyecto_acis.domain.service.DestinoMapa.consulta(orden, catastro))) "" else "No hay una aplicación disponible para abrir el mapa."
                            }, modifier = Modifier.fillMaxWidth()) { Text("Buscar local en Maps") }
                            Text(com.example.proyecto_acis.domain.service.DestinoMapa.descripcion(orden, catastro),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
                            Button(onClick = { onSeleccionar(orden.id) }, modifier = Modifier.fillMaxWidth()) { Text("Registrar llegada") }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onRegistros, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium) { Text("Ver los ${ordenes.size} registros") }
        }
        item {
            Text("Demo · datos guardados en este dispositivo", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
