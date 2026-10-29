package com.example.proyecto_acis.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(painterResource(R.drawable.ic_acis_back), "Retroceder")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("ACIS", style = MaterialTheme.typography.titleLarge)
                        Text(perfil.etiqueta, style = MaterialTheme.typography.labelMedium)
                    }
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
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(selected = ruta == RutasAcis.INICIO,
                    onClick = { irPrincipal(RutasAcis.INICIO) },
                    icon = { Icon(painterResource(R.drawable.ic_acis_home), null) }, label = { Text("Inicio") })
                NavigationBarItem(selected = ruta != RutasAcis.INICIO,
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Image(painter = painterResource(R.drawable.ic_acis_logo), contentDescription = "Logo de ACIS",
                modifier = Modifier.size(76.dp))
            Spacer(Modifier.height(12.dp))
            Text("ACIS", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary)
            Text("Selecciona tu perfil", style = MaterialTheme.typography.headlineSmall)
            Text("Acceso de demostración en este dispositivo.", style = MaterialTheme.typography.bodyMedium)
        }
        Perfil.entries.forEach { perfil ->
            item {
                val colores = MaterialTheme.colorScheme
                val fondo = when (perfil) {
                    Perfil.PLANIFICADOR -> colores.primaryContainer
                    Perfil.TECNICO -> colores.secondaryContainer
                    Perfil.CLIENTE -> colores.tertiaryContainer
                }
                val texto = when (perfil) {
                    Perfil.PLANIFICADOR -> colores.onPrimaryContainer
                    Perfil.TECNICO -> colores.onSecondaryContainer
                    Perfil.CLIENTE -> colores.onTertiaryContainer
                }
                Card(onClick = { onSeleccionar(perfil) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(perfil.etiqueta, style = MaterialTheme.typography.titleLarge)
                        Text(descripcionPerfil(perfil), style = MaterialTheme.typography.bodyMedium)
                        Text("Entrar como ${perfil.etiqueta.lowercase()} →", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun InicioPerfil(perfil: Perfil, ordenes: List<OrdenTrabajo>, onRegistros: () -> Unit,
    onCrear: () -> Unit, onSeleccionar: (String) -> Unit) {
    val pendientes = ordenes.count { it.estado != EstadoOt.CERRADA }
    val porRevisar = ordenes.filter { it.estado == EstadoOt.PENDIENTE_CONFORMIDAD }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text(if (perfil == Perfil.CLIENTE) "Tus servicios" else "Inicio",
                style = MaterialTheme.typography.headlineMedium)
            Text(if (perfil == Perfil.CLIENTE) "Consulta el avance y revisa los trabajos realizados."
                else descripcionPerfil(perfil), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (perfil == Perfil.CLIENTE) "${porRevisar.size}" else "$pendientes",
                        style = MaterialTheme.typography.headlineLarge)
                    Text(if (perfil == Perfil.CLIENTE) "Trabajos por revisar" else "Órdenes sin cerrar",
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (perfil == Perfil.CLIENTE && porRevisar.isEmpty()) "No tienes trabajos pendientes de revisión."
                        else "${ordenes.size} registros disponibles")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (perfil == Perfil.PLANIFICADOR) Button(onCrear, Modifier.fillMaxWidth()) { Text("Crear nueva OT") }
                if (perfil == Perfil.CLIENTE && porRevisar.isNotEmpty()) {
                    Button(onClick = { onSeleccionar(porRevisar.first().id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Revisar trabajo pendiente")
                    }
                    OutlinedButton(onRegistros, Modifier.fillMaxWidth()) { Text("Ver todos mis trabajos") }
                } else if (perfil == Perfil.PLANIFICADOR) {
                    OutlinedButton(onRegistros, Modifier.fillMaxWidth()) { Text("Ver registros") }
                } else {
                    Button(onRegistros, Modifier.fillMaxWidth()) {
                        Text(if (perfil == Perfil.CLIENTE) "Ver mis trabajos" else "Ver mis órdenes")
                    }
                }
            }
        }
        if (perfil == Perfil.CLIENTE && porRevisar.isNotEmpty()) item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Último trabajo por revisar", style = MaterialTheme.typography.titleMedium)
                    Text("${porRevisar.first().local} · ${porRevisar.first().equipo}")
                    Text("Abre el trabajo para aprobarlo o dejar un comentario.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
