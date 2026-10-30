package com.example.proyecto_acis.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.proyecto_acis.domain.TiempoVisita
import com.example.proyecto_acis.ui.theme.EstadoColors
import androidx.compose.runtime.saveable.rememberSaveable

private object RutasAcis {
    const val ACCESO = "acceso"
    const val INICIO = "inicio"
    const val REGISTROS = "registros"
    const val PERFIL = "perfil"
    const val EQUIPOS = "equipos"
    const val EQUIPO = "equipo/{equipoId}"
    const val CLIENTES = "clientes"
    const val LOCALES = "locales"
    const val CLIENTE = "cliente/{clienteId}"
    const val LOCAL = "local/{localId}"
    const val REPORTES = "reportes"
    const val ASIGNAR = "asignar/{ordenId}"
    const val CREAR = "crear"
    const val EDITAR = "editar/{ordenId}"
    const val DETALLE = "detalle/{ordenId}"
    fun detalle(id: String) = "detalle/$id"
}

@Composable
fun AcisApp(viewModel: OrdenesViewModel, catastroViewModel: CatastroViewModel, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: RutasAcis.ACCESO
    val ordenes by viewModel.ordenes.collectAsState()
    val catastro by viewModel.catastro.collectAsState()
    val perfil = viewModel.perfil
    val visibles = viewModel.ordenesParaPerfil(ordenes, perfil)
    val enAcceso = ruta == RutasAcis.ACCESO
    fun irPrincipal(destino: String) {
        nav.navigate(destino) {
            popUpTo(RutasAcis.INICIO) { inclusive = destino == RutasAcis.INICIO }
            launchSingleTop = true
        }
    }

    var elegirTecnico by rememberSaveable { mutableStateOf(false) }
    if (elegirTecnico) AlertDialog(onDismissRequest = { elegirTecnico = false },
        title = { Text("Elige tu técnico") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.tecnicos.forEach { tecnico ->
                    OutlinedButton(onClick = {
                        viewModel.seleccionarTecnico(tecnico)
                        elegirTecnico = false
                        nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
                    }, modifier = Modifier.fillMaxWidth()) { Text(tecnico) }
                }
            }
        }, confirmButton = {}, dismissButton = {
            TextButton(onClick = { elegirTecnico = false }) { Text("Cancelar") }
        })
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
                    if (perfil == Perfil.PLANIFICADOR) IconButton(onClick = { irPrincipal(RutasAcis.PERFIL) }) {
                        Icon(painterResource(R.drawable.ic_acis_profile), contentDescription = "Abrir mi perfil")
                    }
                }
            }
        }
        NavHost(nav, startDestination = RutasAcis.ACCESO, modifier = Modifier.weight(1f),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            composable(RutasAcis.ACCESO) {
                SeleccionPerfil { seleccionado ->
                    if (seleccionado == Perfil.TECNICO) elegirTecnico = true
                    else {
                        viewModel.seleccionarPerfil(seleccionado)
                        nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
                    }
                }
            }
            composable(RutasAcis.INICIO) {
                InicioPerfil(perfil, visibles, viewModel, onRegistros = { irPrincipal(RutasAcis.REGISTROS) },
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
                            nav.navigate("asignar/$id") {
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
            composable(RutasAcis.ASIGNAR) { destino ->
                val orden = visibles.firstOrNull { it.id == destino.arguments?.getString("ordenId") }
                if (orden != null) DetalleOrden(orden, perfil, viewModel,
                    onVolver = { nav.popBackStack() }, asignacionInicial = true,
                    onEditar = { if (viewModel.prepararEdicion(orden.id)) nav.navigate("editar/${orden.id}") })
            }
            composable(RutasAcis.EQUIPOS) {
                if (perfil == Perfil.PLANIFICADOR) EquiposPlanificador(viewModel.catalogoEquipos(visibles, catastro),
                    onCrear = { local, equipo -> viewModel.prepararCreacion(local, equipo); nav.navigate(RutasAcis.CREAR) },
                    onNuevo = { if (catastroViewModel.prepararEquipo()) nav.navigate("equipo/nuevo") },
                    onEditar = { id -> if (catastroViewModel.prepararEquipo(id)) nav.navigate("equipo/$id") },
                    onClientes = { nav.navigate(RutasAcis.CLIENTES) },
                    onLocales = { nav.navigate(RutasAcis.LOCALES) },
                    mensaje = catastroViewModel.mensajeEquipo)
            }
            composable(RutasAcis.EQUIPO) { destino ->
                val id = destino.arguments?.getString("equipoId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && catastroViewModel.datosEquipo.id != id) catastroViewModel.prepararEquipo(id)
                    }
                    if (id == "nuevo" || catastroViewModel.datosEquipo.id == id) FormularioEquipo(catastroViewModel) { nav.popBackStack() }
                    else Text("El equipo no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.CLIENTES) {
                if (perfil == Perfil.PLANIFICADOR) ListaClientes(catastro,
                    onNuevo = { if (catastroViewModel.prepararCliente()) nav.navigate("cliente/nuevo") },
                    onEditar = { id -> if (catastroViewModel.prepararCliente(id)) nav.navigate("cliente/$id") },
                    mensaje = catastroViewModel.mensajeCliente)
            }
            composable(RutasAcis.LOCALES) {
                if (perfil == Perfil.PLANIFICADOR) ListaLocales(catastro,
                    onNuevo = { if (catastroViewModel.prepararLocal()) nav.navigate("local/nuevo") },
                    onEditar = { id -> if (catastroViewModel.prepararLocal(id)) nav.navigate("local/$id") },
                    mensaje = catastroViewModel.mensajeLocal)
            }
            composable(RutasAcis.CLIENTE) { destino ->
                val id = destino.arguments?.getString("clienteId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && catastroViewModel.datosCliente.id != id) catastroViewModel.prepararCliente(id)
                    }
                    if (id == "nuevo" || catastroViewModel.datosCliente.id == id) FormularioCliente(catastroViewModel) { nav.popBackStack() }
                    else Text("El cliente no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.LOCAL) { destino ->
                val id = destino.arguments?.getString("localId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && catastroViewModel.datosLocal.id != id) catastroViewModel.prepararLocal(id)
                    }
                    if (id == "nuevo" || catastroViewModel.datosLocal.id == id) FormularioLocal(catastroViewModel) { nav.popBackStack() }
                    else Text("El local no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.REPORTES) {
                if (perfil == Perfil.PLANIFICADOR) ReportePlanificador(viewModel.resumenOperativo(visibles))
            }
            composable(RutasAcis.PERFIL) {
                PerfilActual(perfil, viewModel.tecnicoActual,
                    onCambiar = { nav.navigate(RutasAcis.ACCESO) { popUpTo(RutasAcis.ACCESO) { inclusive = true } } },
                    onDemo = if (perfil == Perfil.PLANIFICADOR && ordenes.isEmpty()) ({ viewModel.cargarDemostracion() }) else null,
                    mensajeDemo = viewModel.mensajeDemo)

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
                    icon = { Icon(painterResource(R.drawable.ic_acis_home), null) }, label = { Text(if (perfil == Perfil.TECNICO) "Jornada" else "Inicio") })
                NavigationBarItem(selected = ruta == RutasAcis.REGISTROS || ruta == RutasAcis.DETALLE || ruta == RutasAcis.ASIGNAR,
                    onClick = { irPrincipal(RutasAcis.REGISTROS) },
                    icon = { Icon(painterResource(R.drawable.ic_acis_records), null) }, label = { Text(when (perfil) { Perfil.PLANIFICADOR -> "Órdenes"; Perfil.TECNICO -> "Historial"; Perfil.CLIENTE -> "Servicios" }) })
                if (perfil == Perfil.PLANIFICADOR) {
                    NavigationBarItem(selected = ruta in listOf(RutasAcis.EQUIPOS, RutasAcis.EQUIPO, RutasAcis.CLIENTES, RutasAcis.LOCALES, RutasAcis.CLIENTE, RutasAcis.LOCAL),
                        onClick = { irPrincipal(RutasAcis.EQUIPOS) },
                        icon = { Icon(painterResource(R.drawable.ic_acis_equipment), null) }, label = { Text("Equipos") })
                    NavigationBarItem(selected = ruta == RutasAcis.REPORTES,
                        onClick = { irPrincipal(RutasAcis.REPORTES) },
                        icon = { Icon(painterResource(R.drawable.ic_acis_reports), null) }, label = { Text("Reportes") })
                } else {
                    NavigationBarItem(selected = ruta == RutasAcis.PERFIL, onClick = {
                        irPrincipal(RutasAcis.PERFIL)
                    }, icon = { Icon(painterResource(R.drawable.ic_acis_profile), contentDescription = null) },
                        label = { Text("Perfil") }, alwaysShowLabel = true)
                }
            }
        }
    }
}

@Composable
private fun PerfilActual(perfil: Perfil, tecnico: String, onCambiar: () -> Unit,
    onDemo: (() -> Unit)? = null, mensajeDemo: String = "") {
    var confirmarDemo by rememberSaveable { mutableStateOf(false) }
    if (confirmarDemo && onDemo != null) AlertDialog(onDismissRequest = { confirmarDemo = false },
        title = { Text("¿Cargar órdenes de ejemplo?") },
        text = { Text("Se crearán seis órdenes de demostración para recorrer los perfiles. Solo está disponible cuando no hay órdenes guardadas.") },
        confirmButton = { TextButton(onClick = { onDemo(); confirmarDemo = false }) { Text("Cargar ejemplos") } },
        dismissButton = { TextButton(onClick = { confirmarDemo = false }) { Text("Cancelar") } })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
            Text("Acceso de demostración", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_acis_profile), contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Text(if (perfil == Perfil.TECNICO) tecnico else perfil.etiqueta,
                        style = MaterialTheme.typography.titleLarge)
                    Text(descripcionPerfil(perfil), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("Almacenamiento local", style = MaterialTheme.typography.titleMedium)
            Text("Las órdenes, borradores y fotos se guardan en este dispositivo. Este acceso permite explorar las funciones de cada rol.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onDemo != null) item {
            OutlinedButton(onClick = { confirmarDemo = true }, modifier = Modifier.fillMaxWidth()) { Text("Cargar órdenes de demostración") }
        }
        if (mensajeDemo.isNotBlank()) item { Text(mensajeDemo, style = MaterialTheme.typography.bodyMedium) }
        item { OutlinedButton(onClick = onCambiar, modifier = Modifier.fillMaxWidth()) { Text("Cambiar de perfil") } }
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
private fun InicioPerfil(perfil: Perfil, ordenes: List<OrdenTrabajo>, viewModel: OrdenesViewModel, onRegistros: () -> Unit,
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
        confirmButton = { TextButton(onClick = { ordenRechazo?.let(viewModel::rechazar); ordenRechazo = null }) { Text("Rechazar") } },
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
                                Button(onClick = { viewModel.aceptar(orden.id) }, modifier = Modifier.weight(1f)) { Text("Aceptar") }
                            }
                        }
                        if (perfil == Perfil.TECNICO && orden.id == siguienteVisita?.id) {
                            OutlinedButton(onClick = {
                                errorMapa = if (buscarLocalEnMapa(context, com.example.proyecto_acis.domain.DestinoMapa.consulta(orden, catastro))) "" else "No hay una aplicación disponible para abrir el mapa."
                            }, modifier = Modifier.fillMaxWidth()) { Text("Buscar local en Maps") }
                            Text(com.example.proyecto_acis.domain.DestinoMapa.descripcion(orden, catastro),
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
