package com.example.proyecto_acis.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.compose.*
import com.example.proyecto_acis.R
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import com.example.proyecto_acis.data.Perfil

private object RutasAcis {
    const val ACCESO = "acceso"
    const val INICIO = "inicio"
    const val REGISTROS = "registros"
    const val CREAR = "crear"
    const val EDITAR = "editar/{ordenId}"
    const val DETALLE = "detalle/{ordenId}"
    fun detalle(id: String) = "detalle/$id"
}

@Composable
fun AcisApp(viewModel: OrdenesViewModel, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: RutasAcis.ACCESO
    val ordenes by viewModel.ordenes.collectAsState()
    val perfil = viewModel.perfil
    val visibles = viewModel.ordenesParaPerfil(ordenes, perfil)
    val enAcceso = ruta == RutasAcis.ACCESO
    fun irPrincipal(destino: String) {
        nav.navigate(destino) {
            popUpTo(RutasAcis.INICIO) { inclusive = destino == RutasAcis.INICIO }
            launchSingleTop = true
        }
    }

    Column(modifier.fillMaxSize()) {
        if (!enAcceso) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(painterResource(R.drawable.ic_acis_back), "Retroceder")
                    }
                    Text("ACIS", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${perfil.etiqueta} · demo", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp))
                }
            }
        }
        NavHost(nav, startDestination = RutasAcis.ACCESO, modifier = Modifier.weight(1f),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            composable(RutasAcis.ACCESO) {
                SeleccionPerfil { seleccionado ->
                    viewModel.seleccionarPerfil(seleccionado)
                    nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
                }
            }
            composable(RutasAcis.INICIO) {
                InicioPerfil(perfil, visibles, onRegistros = { irPrincipal(RutasAcis.REGISTROS) },
                    onCrear = { viewModel.prepararCreacion(); nav.navigate(RutasAcis.CREAR) },
                    onSeleccionar = { nav.navigate(RutasAcis.detalle(it)) })
            }
            composable(RutasAcis.REGISTROS) {
                ListaOrdenes(visibles, perfil, onCrear = { viewModel.prepararCreacion(); nav.navigate(RutasAcis.CREAR) },
                    onSeleccionar = { nav.navigate(RutasAcis.detalle(it)) })
            }
            composable(RutasAcis.CREAR) {
                if (perfil == Perfil.PLANIFICADOR) CrearOrden(viewModel,
                    onVolver = { nav.popBackStack() }, onCrear = {
                        viewModel.crear()?.let { id ->
                            nav.navigate(RutasAcis.detalle(id)) {
                                popUpTo(RutasAcis.CREAR) { inclusive = true }
                            }
                        }
                    })
            }
            composable(RutasAcis.DETALLE) { destino ->
                val orden = visibles.firstOrNull { it.id == destino.arguments?.getString("ordenId") }
                if (orden != null) DetalleOrden(orden, perfil, viewModel, onVolver = { nav.popBackStack() },
                    onEditar = { if (viewModel.prepararEdicion(orden.id)) nav.navigate("editar/${orden.id}") })
                else Text("La orden no está disponible para este perfil.", Modifier.padding(20.dp))
            }
            composable(RutasAcis.EDITAR) { destino ->
                val id = destino.arguments?.getString("ordenId").orEmpty()
                CrearOrden(viewModel, onVolver = { nav.popBackStack() },
                    onCrear = { if (viewModel.guardarEdicion(id)) nav.popBackStack() }, editando = true)
            }
        }
        if (!enAcceso) {
            NavigationBar(containerColor = Color(0xFF1D2524), windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(selected = ruta == RutasAcis.INICIO,
                    onClick = { irPrincipal(RutasAcis.INICIO) },
                    icon = { Icon(painterResource(R.drawable.ic_acis_home), null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = ruta == RutasAcis.REGISTROS || ruta == RutasAcis.DETALLE,
                    onClick = { irPrincipal(RutasAcis.REGISTROS) },
                    icon = { Icon(painterResource(R.drawable.ic_acis_records), null) }, label = { Text("Registros") })
                NavigationBarItem(selected = false, onClick = {
                    nav.navigate(RutasAcis.ACCESO) { popUpTo(RutasAcis.ACCESO) { inclusive = true } }
                }, icon = { Icon(painterResource(R.drawable.ic_acis_profile), contentDescription = null) },
                    label = { Text("Cambiar perfil") }, alwaysShowLabel = true)
            }
        }
    }
}

private fun descripcionPerfil(perfil: Perfil) = when (perfil) {
    Perfil.PLANIFICADOR -> "Crea, programa y asigna órdenes de trabajo."
    Perfil.TECNICO -> "Consulta tus asignaciones y registra las visitas e informes."
    Perfil.CLIENTE -> "Revisa los trabajos y registra tu conformidad u observaciones."
}

@Composable
private fun SeleccionPerfil(onSeleccionar: (Perfil) -> Unit) {
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

@Composable
private fun InicioPerfil(perfil: Perfil, ordenes: List<OrdenTrabajo>, onRegistros: () -> Unit,
    onCrear: () -> Unit, onSeleccionar: (String) -> Unit) {
    val pendientes = ordenes.count { it.estado != EstadoOt.CERRADA }
    val porRevisar = ordenes.count { it.estado == EstadoOt.PENDIENTE_CONFORMIDAD }
    val fechaHoy = remember { SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "CL")).format(Date()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(when (perfil) { Perfil.PLANIFICADOR -> "Control de órdenes"; Perfil.TECNICO -> "Mis trabajos"; Perfil.CLIENTE -> "Tus servicios" },
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(fechaHoy.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("RESUMEN DE ACTIVIDAD", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("%02d".format(if (perfil == Perfil.CLIENTE) porRevisar else pendientes),
                                style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Text(if (perfil == Perfil.CLIENTE) "POR REVISAR" else "SIN CERRAR", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("%02d".format(ordenes.size), style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text("REGISTROS", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Vista de ${perfil.etiqueta.lowercase()}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR) item {
            Button(onClick = onCrear, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                Text("+   Crear nueva OT", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (perfil == Perfil.CLIENTE) "Trabajos recientes" else "Órdenes recientes", style = MaterialTheme.typography.titleLarge)
                Text("$pendientes sin cerrar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ordenes.isEmpty()) item {
            Text("Aún no hay registros para mostrar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ordenes.take(3).forEach { orden ->
            item(key = orden.id) {
                Card(onClick = { onSeleccionar(orden.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(orden.id, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace)
                            Text("Prioridad ${orden.prioridad.etiqueta.lowercase()}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(orden.equipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${orden.local} · ${orden.fechaProgramada}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                Text(orden.estado.etiqueta, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("→", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
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
