package com.example.proyecto_acis.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.proyecto_acis.R
import com.example.proyecto_acis.data.repository.CargaDatos
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.ui.screens.catalogo.EquiposPlanificador
import com.example.proyecto_acis.ui.screens.catalogo.FormularioCliente
import com.example.proyecto_acis.ui.screens.catalogo.FormularioEquipo
import com.example.proyecto_acis.ui.screens.catalogo.FormularioLocal
import com.example.proyecto_acis.ui.screens.catalogo.ListaClientes
import com.example.proyecto_acis.ui.screens.catalogo.ListaLocales
import com.example.proyecto_acis.ui.screens.inicio.InicioPerfil
import com.example.proyecto_acis.ui.screens.ordenes.CrearOrden
import com.example.proyecto_acis.ui.screens.ordenes.DetalleOrden
import com.example.proyecto_acis.ui.screens.ordenes.ListaOrdenes
import com.example.proyecto_acis.ui.screens.perfil.PerfilActual
import com.example.proyecto_acis.ui.screens.perfil.SeleccionPerfil
import com.example.proyecto_acis.ui.screens.reportes.ReportePlanificador
import com.example.proyecto_acis.ui.viewmodel.CatastroViewModel
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import kotlinx.coroutines.delay

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
fun AcisApp(viewModel: OrdenesViewModel, catastroViewModel: CatastroViewModel, climaViewModel: ClimaViewModel, modifier: Modifier = Modifier) {
    val carga by viewModel.carga.collectAsState()
    val preferencias by viewModel.preferencias.collectAsState()
    if (carga != CargaDatos.LISTO || preferencias.cargando) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (carga == CargaDatos.CARGANDO || preferencias.cargando) {
                CircularProgressIndicator()
                Text("Abriendo los registros…", Modifier.padding(top = 16.dp))
            } else {
                Text("No se pudieron abrir los registros. Tus datos no se han eliminado.")
                Button(onClick = viewModel::reintentarCarga) { Text("Reintentar") }
            }
        }
        return
    }
    val operacionOrden by viewModel.operacion.collectAsState()
    val operacionCatastro by catastroViewModel.operacion.collectAsState()
    if (operacionOrden.enCurso || operacionCatastro.enCurso) {
        AlertDialog(onDismissRequest = {}, title = { Text("Guardando") },
            text = { Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Espera un momento…")
            } }, confirmButton = {})
    } else if (operacionOrden.error.isNotBlank() || operacionCatastro.error.isNotBlank()) {
        AlertDialog(onDismissRequest = { viewModel.limpiarError(); catastroViewModel.limpiarError() },
            title = { Text("No se pudo completar") },
            text = { Text(operacionOrden.error.ifBlank { operacionCatastro.error }) },
            confirmButton = { TextButton(onClick = { viewModel.limpiarError(); catastroViewModel.limpiarError() }) { Text("Entendido") } })
    }
    val nav = rememberNavController()
    var accesoInicialAplicado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!accesoInicialAplicado) {
            accesoInicialAplicado = true
            if (preferencias.datos.recordarPerfil && preferencias.datos.perfil != null) {
                nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
            }
        }
    }
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: RutasAcis.ACCESO
    val ordenes by viewModel.ordenes.collectAsState()
    val catastro by viewModel.catastro.collectAsState()
    val eventos by viewModel.eventos.collectAsState()
    val formulariosCatastro by catastroViewModel.estado.collectAsState()
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
                    OutlinedButton(onClick = { viewModel.ejecutar {
                        viewModel.seleccionarTecnico(tecnico)
                        elegirTecnico = false
                        nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
                     } }, modifier = Modifier.fillMaxWidth()) { Text(tecnico) }
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
                    else viewModel.ejecutar {
                        viewModel.seleccionarPerfil(seleccionado)
                        nav.navigate(RutasAcis.INICIO) { launchSingleTop = true }
                    }
                }
            }
            composable(RutasAcis.INICIO) {
                InicioPerfil(perfil, visibles, viewModel, climaViewModel, onRegistros = { irPrincipal(RutasAcis.REGISTROS) },
                    onCrear = { viewModel.prepararCreacion(); nav.navigate(RutasAcis.CREAR) },
                    onSeleccionar = { nav.navigate(RutasAcis.detalle(it)) })
            }
            composable(RutasAcis.REGISTROS) {
                ListaOrdenes(visibles, perfil, onCrear = { viewModel.prepararCreacion(); nav.navigate(RutasAcis.CREAR) },
                    onSeleccionar = { nav.navigate(RutasAcis.detalle(it)) })
            }
            composable(RutasAcis.CREAR) {
                if (perfil == Perfil.PLANIFICADOR) CrearOrden(viewModel, climaViewModel,
                    onVolver = { nav.popBackStack() }, onCrear = { viewModel.ejecutar {
                        viewModel.crear()?.let { id ->
                            nav.navigate("asignar/$id") {
                                popUpTo(RutasAcis.CREAR) { inclusive = true }
                            }
                        }
                    } })
            }
            composable(RutasAcis.DETALLE) { destino ->
                val orden = visibles.firstOrNull { it.id == destino.arguments?.getString("ordenId") }
                if (orden != null) DetalleOrden(orden, perfil, viewModel, climaViewModel, onVolver = { nav.popBackStack() },
                    onEditar = { if (viewModel.prepararEdicion(orden.id)) nav.navigate("editar/${orden.id}") })
                else Text("La orden no está disponible para este perfil.", Modifier.padding(20.dp))
            }
            composable(RutasAcis.ASIGNAR) { destino ->
                val orden = visibles.firstOrNull { it.id == destino.arguments?.getString("ordenId") }
                if (orden != null) DetalleOrden(orden, perfil, viewModel, climaViewModel,
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
                    mensaje = formulariosCatastro.equipo.mensaje)
            }
            composable(RutasAcis.EQUIPO) { destino ->
                val id = destino.arguments?.getString("equipoId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && formulariosCatastro.equipo.datos.id != id) catastroViewModel.prepararEquipo(id)
                    }
                    if (id == "nuevo" || formulariosCatastro.equipo.datos.id == id) FormularioEquipo(catastroViewModel) { nav.popBackStack() }
                    else Text("El equipo no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.CLIENTES) {
                if (perfil == Perfil.PLANIFICADOR) ListaClientes(catastro,
                    onNuevo = { if (catastroViewModel.prepararCliente()) nav.navigate("cliente/nuevo") },
                    onEditar = { id -> if (catastroViewModel.prepararCliente(id)) nav.navigate("cliente/$id") },
                    mensaje = formulariosCatastro.cliente.mensaje)
            }
            composable(RutasAcis.LOCALES) {
                if (perfil == Perfil.PLANIFICADOR) ListaLocales(catastro,
                    onNuevo = { if (catastroViewModel.prepararLocal()) nav.navigate("local/nuevo") },
                    onEditar = { id -> if (catastroViewModel.prepararLocal(id)) nav.navigate("local/$id") },
                    mensaje = formulariosCatastro.local.mensaje)
            }
            composable(RutasAcis.CLIENTE) { destino ->
                val id = destino.arguments?.getString("clienteId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && formulariosCatastro.cliente.datos.id != id) catastroViewModel.prepararCliente(id)
                    }
                    if (id == "nuevo" || formulariosCatastro.cliente.datos.id == id) FormularioCliente(catastroViewModel) { nav.popBackStack() }
                    else Text("El cliente no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.LOCAL) { destino ->
                val id = destino.arguments?.getString("localId").orEmpty()
                if (perfil == Perfil.PLANIFICADOR) {
                    LaunchedEffect(id) {
                        if (id != "nuevo" && formulariosCatastro.local.datos.id != id) catastroViewModel.prepararLocal(id)
                    }
                    if (id == "nuevo" || formulariosCatastro.local.datos.id == id) FormularioLocal(catastroViewModel) { nav.popBackStack() }
                    else Text("El local no está disponible.", Modifier.padding(20.dp))
                }
            }
            composable(RutasAcis.REPORTES) {
                if (perfil == Perfil.PLANIFICADOR) {
                    var reloj by remember { mutableStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) { while (true) { reloj = System.currentTimeMillis(); delay(30_000) } }
                    ReportePlanificador(viewModel.resumenOperativo(visibles), viewModel.reporteSla(visibles, eventos, reloj),
                        onMes = viewModel::cambiarMesReporte,
                        onSeleccionar = { nav.navigate(RutasAcis.detalle(it)) })
                }
            }
            composable(RutasAcis.PERFIL) {
                PerfilActual(perfil, viewModel.tecnicoActual, viewModel, visibles.map { it.id }.toSet(),
                    onCambiar = { nav.navigate(RutasAcis.ACCESO) { popUpTo(RutasAcis.ACCESO) { inclusive = true } } },
                    onDemo = if (perfil == Perfil.PLANIFICADOR && ordenes.isEmpty()) ({ viewModel.ejecutar { viewModel.cargarDemostracion() } }) else null,
                    mensajeDemo = viewModel.mensajeDemo)

            }
            composable(RutasAcis.EDITAR) { destino ->
                val id = destino.arguments?.getString("ordenId").orEmpty()
                CrearOrden(viewModel, climaViewModel, onVolver = { nav.popBackStack() },
                    onCrear = { viewModel.ejecutar { if (viewModel.guardarEdicion(id)) nav.popBackStack() } }, editando = true)
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
                        icon = { Icon(painterResource(R.drawable.ic_acis_equipment), null) }, label = { Text("Catálogo") })
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
