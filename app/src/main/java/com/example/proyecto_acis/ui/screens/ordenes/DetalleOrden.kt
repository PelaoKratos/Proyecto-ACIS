package com.example.proyecto_acis.ui.screens.ordenes

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.proyecto_acis.device.location.obtenerUbicacionCheckIn
import com.example.proyecto_acis.device.maps.abrirUbicacionVisita
import com.example.proyecto_acis.device.maps.buscarLocalEnMapa
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.service.DestinoMapa
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.ui.components.CampoDetalle
import com.example.proyecto_acis.ui.components.CampoFormulario
import com.example.proyecto_acis.ui.components.ErrorSeleccion
import com.example.proyecto_acis.ui.components.EstadoBadge
import com.example.proyecto_acis.ui.components.FotoMiniatura
import com.example.proyecto_acis.ui.components.HistorialEventos
import com.example.proyecto_acis.ui.components.PrioridadBadge
import com.example.proyecto_acis.ui.components.SeccionFormulario
import com.example.proyecto_acis.ui.components.SyncStatusPill
import com.example.proyecto_acis.ui.components.TituloSeccion
import com.example.proyecto_acis.ui.components.WeatherBanner
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DetalleOrden(
    orden: OrdenTrabajo,
    perfil: Perfil,
    viewModel: OrdenesViewModel,
    climaViewModel: ClimaViewModel,
    onVolver: () -> Unit,
    onEditar: () -> Unit = {},
    asignacionInicial: Boolean = false,
) {
    val registrosSync by viewModel.sincronizacion.collectAsState()
    val estadoDetalle by viewModel.detalle.collectAsState()
    LaunchedEffect(orden.id) { viewModel.prepararDetalle(orden) }
    var escribiendoInforme by rememberSaveable(orden.id) { mutableStateOf(false) }
    var seccionInforme by rememberSaveable(orden.id) { mutableStateOf("inicio") }
    var asignando by rememberSaveable(orden.id) { mutableStateOf(asignacionInicial && orden.estado == EstadoOt.CREADA && perfil == Perfil.PLANIFICADOR) }
    var tecnicoElegido by rememberSaveable(orden.id) { mutableStateOf("") }
    var horaAsignada by rememberSaveable(orden.id) { mutableStateOf(orden.fechaProgramada.substringAfter(" ", "")) }
    var errorAsignacion by rememberSaveable(orden.id) { mutableStateOf("") }
    val erroresAsignacion = estadoDetalle.erroresAsignacion[orden.id] ?: com.example.proyecto_acis.domain.validation.ErroresAsignacion()
    if (asignando) AlertDialog(onDismissRequest = { asignando = false },
        title = { Text("Asignar técnico") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${orden.id} · ${orden.equipo}")
                Text(orden.fechaProgramada)
                viewModel.tecnicos.forEach { tecnico ->
                    OutlinedButton(onClick = { tecnicoElegido = tecnico; errorAsignacion = ""; viewModel.limpiarErroresAsignacion(orden.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${if (tecnicoElegido == tecnico) "✓ " else ""}$tecnico · ${viewModel.cargaTecnico(tecnico, orden.fechaProgramada)} visitas")
                    }
                }
                erroresAsignacion.tecnico?.let { ErrorSeleccion(it) }
                Text("Horarios sugeridos", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(orden.fechaProgramada.substringAfter(" ", ""), "09:00", "11:00", "15:00")).filter { it.isNotBlank() }.distinct().forEach { hora ->
                        FilterChip(selected = horaAsignada == hora, onClick = {
                            horaAsignada = hora; errorAsignacion = ""; viewModel.limpiarErroresAsignacion(orden.id)
                        }, label = { Text(hora) })
                    }
                }
                erroresAsignacion.hora?.let { ErrorSeleccion(it) }
                Text("Hora elegida: $horaAsignada. La carga del día no garantiza disponibilidad en ese horario.",
                    style = MaterialTheme.typography.bodySmall)
                if (errorAsignacion.isNotBlank()) Text(errorAsignacion, color = MaterialTheme.colorScheme.error)
                Text("La asignación se guarda en este dispositivo.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.ejecutar {
            if (viewModel.asignar(orden.id, tecnicoElegido, horaAsignada)) asignando = false
            else errorAsignacion = viewModel.mensaje(orden.id)
         } }) { Text("Asignar") } },
        dismissButton = { TextButton(onClick = { asignando = false }) { Text("Más tarde") } })
    val catastro by viewModel.catastro.collectAsState()
    val context = LocalContext.current
    val scopeUbicacion = rememberCoroutineScope()
    var buscandoUbicacion by remember(orden.id) { mutableStateOf(false) }
    var errorMapa by rememberSaveable(orden.id) { mutableStateOf("") }
    var accionConfirmar by remember { mutableStateOf("") }
    var mensajeUbicacion by remember { mutableStateOf("") }
    if (accionConfirmar.isNotBlank()) AlertDialog(
        onDismissRequest = { accionConfirmar = "" },
        title = { Text(if (accionConfirmar == "eliminar") "¿Eliminar esta orden?" else "¿Rechazar esta orden?") },
        text = { Text(if (accionConfirmar == "eliminar") "La orden se eliminará del dispositivo. Esta acción no se puede deshacer."
            else "La orden volverá al planificador para que pueda asignarla nuevamente.") },
        confirmButton = { TextButton(onClick = { viewModel.ejecutar {
            val ok = if (accionConfirmar == "eliminar") viewModel.eliminar(orden.id) else viewModel.rechazar(orden.id)
            accionConfirmar = ""
            if (ok) onVolver() else mensajeUbicacion = "No se pudo completar la acción. Intenta nuevamente."
         } }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { accionConfirmar = "" }) { Text("Cancelar") } })
    var confirmarAprobacion by rememberSaveable(orden.id) { mutableStateOf(false) }
    if (confirmarAprobacion) {
        AlertDialog(onDismissRequest = { confirmarAprobacion = false },
            title = { Text("¿Aprobar el trabajo?") },
            text = { Text("Al aprobar, la orden quedará cerrada.") },
            confirmButton = { TextButton(onClick = { viewModel.ejecutar { viewModel.aprobar(orden.id); confirmarAprobacion = false  } }) { Text("Sí, aprobar") } },
            dismissButton = { TextButton(onClick = { confirmarAprobacion = false }) { Text("Volver") } })
    }
    var actualizarUbicacion by rememberSaveable(orden.id) { mutableStateOf(false) }
    val iniciarConUbicacion: () -> Unit = {
        buscandoUbicacion = true
        val actualizar = actualizarUbicacion
        scopeUbicacion.launch {
            try {
                val ubicacion = obtenerUbicacionCheckIn(context)
                if (ubicacion == null) mensajeUbicacion = "No pudimos obtener la ubicación. Revisa la ubicación del teléfono y vuelve a intentar."
                else if (actualizar) {
                    mensajeUbicacion = if (viewModel.guardarUbicacion(orden.id, ubicacion)) "Ubicación guardada. Puedes verla en Google Maps."
                        else "No se pudo guardar la ubicación."
                } else {
                    viewModel.iniciarVisita(orden.id, ubicacion)
                    mensajeUbicacion = ""
                }
            } finally {
                buscandoUbicacion = false
            }
        }
    }
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) iniciarConUbicacion()
        else mensajeUbicacion = "Permiso de ubicación no concedido. Puedes iniciar la visita sin ubicación o habilitar el permiso desde los ajustes."
    }
    var tipoFoto by rememberSaveable(orden.id) { mutableStateOf("Antes") }
    var fotoEliminar by rememberSaveable(orden.id) { mutableStateOf("") }
    if (fotoEliminar.isNotBlank()) AlertDialog(onDismissRequest = { fotoEliminar = "" },
        title = { Text("¿Eliminar esta foto?") }, text = { Text("Se eliminará la evidencia de esta orden.") },
        confirmButton = { TextButton(onClick = { viewModel.ejecutar { viewModel.eliminarFoto(orden.id, fotoEliminar); fotoEliminar = ""  } }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { fotoEliminar = "" }) { Text("Cancelar") } })
    var fotoPendiente by rememberSaveable(orden.id) { mutableStateOf("") }
    var errorFoto by rememberSaveable(orden.id) { mutableStateOf("") }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        viewModel.ejecutar {
            val nombre = fotoPendiente
            fotoPendiente = ""
            if (nombre.isNotBlank()) {
                val archivo = File(File(context.filesDir, "evidencias"), nombre)
                if (guardada && viewModel.guardarFoto(orden.id, nombre)) {
                    errorFoto = ""
                } else {
                    archivo.delete()
                    if (guardada) errorFoto = "No se pudo guardar la foto."
                }
            }
        }
    }
    var tipoGaleria by rememberSaveable(orden.id) { mutableStateOf("Antes") }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.ejecutar {
            errorFoto = if (viewModel.importarFoto(orden.id, uri, tipoGaleria)) "" else viewModel.mensaje(orden.id)
        }
    }
    val elegirFoto: () -> Unit = {
        tipoGaleria = tipoFoto
        errorFoto = ""
        try {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) {
            errorFoto = "No hay un selector de imágenes disponible en este dispositivo."
        } catch (_: SecurityException) {
            errorFoto = "No se pudo abrir la galería. Intenta nuevamente."
        }
    }
    val tomarFoto: () -> Unit = {
                    var archivoNuevo: File? = null
                    try {
                        val carpeta = File(context.filesDir, "evidencias")
                        if (!carpeta.exists()) carpeta.mkdirs()
                        val archivo = File(carpeta, "${orden.id}_${tipoFoto}_${UUID.randomUUID()}.jpg")
                        archivo.createNewFile()
                        archivoNuevo = archivo
                        fotoPendiente = archivo.name
                        errorFoto = ""
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", archivo,
                        )
                        camara.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorFoto = "No hay una cámara disponible en este dispositivo."
                    } catch (_: Exception) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorFoto = "No se pudo abrir la cámara."
                    }
    }
    val todosLosEventos by viewModel.eventos.collectAsState()
    val eventosOrden = viewModel.eventosDeOrden(orden.id, todosLosEventos)
    val formulario = estadoDetalle.formularios[orden.id] ?: viewModel.formularioDetalle(orden)
    val ultimoFormulario by rememberUpdatedState(formulario)
    DisposableEffect(orden.id, perfil) {
        onDispose { if (perfil == Perfil.TECNICO) viewModel.autoguardarAlSalir(orden.id, ultimoFormulario) }
    }
    val erroresInforme = viewModel.erroresInforme(orden.id, formulario.diagnostico, formulario.actividad)
    val errorObservacion = viewModel.errorObservacion(orden.id, formulario.observacion)
    val errorRespuesta = viewModel.errorRespuesta(orden.id, formulario.respuesta)
    val mensaje = viewModel.mensaje(orden.id)
    var reloj by remember(orden.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(orden.id, orden.estado) {
        while (orden.estado == EstadoOt.EN_VISITA) {
            reloj = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val minutosEnSitio = TiempoVisita.minutos(orden.checkIn, orden.checkOut, reloj)


    val listaDetalle = rememberLazyListState()
    LaunchedEffect(escribiendoInforme, seccionInforme, orden.estado) {
        // Cabecera, título y aviso preceden las secciones del informe.
        val destino = if (escribiendoInforme && orden.estado == EstadoOt.EN_VISITA) when (seccionInforme) {
            "nota" -> 3
            "materiales" -> 4
            "fotos" -> 5
            else -> 0
        } else 0
        listaDetalle.scrollToItem(destino)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
    LazyColumn(state = listaDetalle, modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${orden.id} / ${if (perfil == Perfil.CLIENTE) "Revisión" else if (escribiendoInforme && orden.estado == EstadoOt.EN_VISITA) "Informe" else if (orden.estado == EstadoOt.EN_VISITA) "Visita" else "Detalle"}",
                    style = MaterialTheme.typography.headlineMedium)
                Text(orden.estado.etiqueta, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.proyecto_acis.ui.components.SyncStatusPill(registrosSync.firstOrNull { it.otId == orden.id })
            }
        }
        if (!escribiendoInforme || orden.estado != EstadoOt.EN_VISITA) {
        if (eventosOrden.isNotEmpty() || viewModel.hitosVisita(orden).isNotEmpty()) item {
            HistorialEventos(eventosOrden, hitosAnteriores =
                if (eventosOrden.isEmpty()) viewModel.hitosVisita(orden)
                else emptyList())
        }
        if (minutosEnSitio != null) item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()) {
                Text("${if (orden.estado == EstadoOt.EN_VISITA) "Tiempo en visita" else "Duración de la visita"}: $minutosEnSitio min",
                    modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado in setOf(EstadoOt.ASIGNADA, EstadoOt.ACEPTADA, EstadoOt.EN_VISITA, EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.CERRADA)) item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("✓ Asignada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(if (orden.estado in setOf(EstadoOt.EN_VISITA, EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.CERRADA)) "✓ En visita" else "○ En visita",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (orden.estado in setOf(EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.CERRADA)) "✓ Revisión" else "○ Revisión",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (mensajeUbicacion.isNotBlank()) item { Text(mensajeUbicacion, color = MaterialTheme.colorScheme.error) }
        if (perfil == Perfil.PLANIFICADOR && orden.estado in setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA)) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text("Editar datos de la orden") }
                TextButton(onClick = { accionConfirmar = "eliminar" }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar orden") }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado in setOf(EstadoOt.ACEPTADA, EstadoOt.EN_VISITA)) item {
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    actualizarUbicacion = true
                    iniciarConUbicacion()
                } else {
                    actualizarUbicacion = true
                    permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }, enabled = !buscandoUbicacion, modifier = Modifier.fillMaxWidth()) {
                Text(if (buscandoUbicacion) "Buscando ubicación…" else "Registrar mi ubicación en esta visita")
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("OT / FICHA DE SERVICIO", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EstadoBadge(orden.estado)
                        PrioridadBadge(orden.prioridad)
                    }
                    Text(orden.equipo, style = MaterialTheme.typography.headlineSmall)
                    Text(orden.local, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(orden.tipo.etiqueta, style = MaterialTheme.typography.bodyMedium)
                    CampoDetalle("Fecha programada", orden.fechaProgramada)
                    orden.acuerdoSla?.let { acuerdo ->
                        val inicio = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL")).format(java.util.Date(acuerdo.inicioMillis))
                        CampoDetalle("Plazos acordados al crear", "${acuerdo.clienteNombre}: llegada ${acuerdo.horasLlegada} h · resolución ${acuerdo.horasResolucion} h. Desde $inicio.")
                    }
                    if (perfil != Perfil.CLIENTE) WeatherBanner(ReglasClima.orden(orden, catastro), climaViewModel)
                    if (perfil != Perfil.CLIENTE) {
                        Text(com.example.proyecto_acis.domain.service.DestinoMapa.descripcion(orden, catastro),
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            errorMapa = if (buscarLocalEnMapa(context, com.example.proyecto_acis.domain.service.DestinoMapa.consulta(orden, catastro))) ""
                                else "No hay una aplicación disponible para abrir el mapa."
                        }, modifier = Modifier.fillMaxWidth()) { Text("Cómo llegar al local") }
                        if (errorMapa.isNotBlank()) Text(errorMapa, color = MaterialTheme.colorScheme.error)
                    }
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TRABAJO SOLICITADO", style = MaterialTheme.typography.labelSmall)
                            Text(orden.descripcion, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (orden.tecnico.isNotBlank()) CampoDetalle("Técnico", orden.tecnico)
                    if (orden.checkIn.isNotBlank() || orden.checkOut.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (orden.checkIn.isNotBlank()) CampoDetalle(if (orden.ubicacionCheckIn.isNotBlank()) "Llegada verificada" else "Llegada registrada sin ubicación", orden.checkIn)
                    if (orden.ubicacionCheckIn.isNotBlank() && perfil != Perfil.CLIENTE) {
                        Text("Ubicación de la visita disponible", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            errorMapa = if (abrirUbicacionVisita(context, orden.ubicacionCheckIn)) ""
                                else "No se pudo abrir la ubicación de esta visita."
                        }, modifier = Modifier.fillMaxWidth()) { Text("Ver ubicación en Google Maps") }
                        if (errorMapa.isNotBlank()) Text(errorMapa, color = MaterialTheme.colorScheme.error)
                    }
                    if (orden.checkOut.isNotBlank()) CampoDetalle("Salida registrada", orden.checkOut)
                    if (orden.observacionCliente.isNotBlank()) CampoDetalle("Observación del cliente", orden.observacionCliente)
                    if (orden.respuestaTecnico.isNotBlank()) CampoDetalle("Respuesta del técnico", orden.respuestaTecnico)
                    if (orden.fechaConformidad.isNotBlank()) CampoDetalle("Conformidad", orden.fechaConformidad)

                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { seccionInforme = "inicio"; escribiendoInforme = true },
                    modifier = Modifier.fillMaxWidth()) { Text("Escribir informe") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        seccionInforme = "fotos"; escribiendoInforme = true; tomarFoto()
                    }, modifier = Modifier.weight(1f)) { Text("Foto") }
                    OutlinedButton(onClick = {
                        seccionInforme = "materiales"; escribiendoInforme = true
                    }, modifier = Modifier.weight(1f)) { Text("Materiales") }
                    OutlinedButton(onClick = {
                        seccionInforme = "nota"; escribiendoInforme = true
                    }, modifier = Modifier.weight(1f)) { Text("Nota") }
                }
            }
        }
        if (perfil == Perfil.TECNICO) item {
            SeccionFormulario("Historial del equipo") {
                val anteriores = viewModel.historialEquipo(orden)
                if (anteriores.isEmpty()) Text("Sin visitas anteriores.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                anteriores.forEach { visita ->
                    CampoDetalle("${visita.id} · ${visita.checkOut}", visita.actividad)
                }
            }
        }
        if (orden.diagnostico.isNotBlank() || orden.actividad.isNotBlank() || orden.materiales.isNotBlank()) {
            item { TituloSeccion("Informe del técnico", "Detalle del trabajo realizado") }
            if (orden.diagnostico.isNotBlank()) item {
                SeccionFormulario("01 / DIAGNÓSTICO") { Text(orden.diagnostico, style = MaterialTheme.typography.bodyMedium) }
            }
            if (orden.actividad.isNotBlank()) item {
                SeccionFormulario("02 / ACTIVIDAD REALIZADA") { Text(orden.actividad, style = MaterialTheme.typography.bodyMedium) }
            }
            if (orden.materiales.isNotBlank()) item {
                SeccionFormulario("03 / MATERIALES") { Text(orden.materiales, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (perfil == Perfil.PLANIFICADOR && orden.estado == EstadoOt.CREADA) {
            item { Button(onClick = { asignando = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Asignar técnico")
            } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
            item { Button(onClick = { viewModel.ejecutar { viewModel.aceptar(orden.id)  } }, modifier = Modifier.fillMaxWidth()) {
                Text("Aceptar OT")
            } }
            item {
                OutlinedButton(onClick = { viewModel.ejecutar {
                    val hoy = Calendar.getInstance()
                    DatePickerDialog(context, { _, anio, mes, dia ->
                        TimePickerDialog(context, { _, hora, minuto ->
                            val fecha = String.format(Locale.US, "%02d/%02d/%04d %02d:%02d", dia, mes + 1, anio, hora, minuto)
                            viewModel.ejecutar { viewModel.postergar(orden.id, fecha) }
                            mensajeUbicacion = viewModel.mensaje(orden.id)
                        }, hoy.get(Calendar.HOUR_OF_DAY), hoy.get(Calendar.MINUTE), true).show()
                    }, hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                 } }, modifier = Modifier.fillMaxWidth()) { Text("Postergar visita") }
                TextButton(onClick = { accionConfirmar = "rechazar" }, modifier = Modifier.fillMaxWidth()) { Text("Rechazar orden") }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compartir ubicación es opcional.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        actualizarUbicacion = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) iniciarConUbicacion()
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, enabled = !buscandoUbicacion, modifier = Modifier.fillMaxWidth()) {
                        Text(if (buscandoUbicacion) "Buscando ubicación..." else "Iniciar con ubicación")
                    }
                    OutlinedButton(onClick = { viewModel.ejecutar { viewModel.iniciarVisita(orden.id, null)  } },
                        enabled = !buscandoUbicacion, modifier = Modifier.fillMaxWidth()) {
                        Text("Iniciar sin ubicación")
                    }
                }
            }
        }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA && escribiendoInforme) {
            item { TituloSeccion("Informe de visita", "Registra el trabajo realizado en esta visita") }
            item {
                AnimatedVisibility(visible = viewModel.estadoBorrador(orden.id).isNotBlank()) {
                    Text(viewModel.estadoBorrador(orden.id), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SeccionFormulario("01 / DIAGNÓSTICO Y ACTIVIDAD") {
                    Text("Diagnósticos frecuentes", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (viewModel.diagnosticosFrecuentes + "Otro").forEach { diagnostico ->
                            FilterChip(selected = if (diagnostico == "Otro") formulario.diagnosticoLibre else !formulario.diagnosticoLibre && formulario.diagnostico == diagnostico,
                                onClick = { viewModel.elegirDiagnostico(orden.id, diagnostico) }, label = { Text(diagnostico) })
                        }
                    }
                    if (formulario.diagnosticoLibre) {
                        CampoFormulario(formulario.diagnostico, { viewModel.cambiarDiagnostico(orden.id, it) }, "Describe el diagnóstico", erroresInforme.diagnostico, minLines = 2)
                    } else if (erroresInforme.diagnostico != null) ErrorSeleccion(erroresInforme.diagnostico)
                    Text("Actividades frecuentes", style = MaterialTheme.typography.bodySmall)
                    listOf("Revisión de conexiones", "Limpieza de filtros", "Ajuste del termostato").forEach { actividad ->
                        FilterChip(selected = actividad in formulario.actividad.lines(),
                            onClick = { viewModel.alternarActividad(orden.id, actividad) },
                            label = { Text(if (actividad in formulario.actividad.lines()) "✓ $actividad" else actividad) })
                    }
                    CampoFormulario(formulario.actividad, { viewModel.cambiarActividad(orden.id, it) }, "Actividad realizada", erroresInforme.actividad, minLines = 2)
                }
            }
            item { SeccionFormulario("02 / MATERIALES") {
                if (formulario.materialesAnteriores.isNotBlank()) Text(formulario.materialesAnteriores,
                    style = MaterialTheme.typography.bodyMedium)
                formulario.listaMateriales.forEachIndexed { indice, material ->
                    Text("Material ${indice + 1}", style = MaterialTheme.typography.titleSmall)
                    CampoFormulario(material.nombre, { viewModel.cambiarMaterial(orden.id, indice, nombre = it) },
                        "Nombre", viewModel.errorMaterial(orden.id, indice, false), ejemplo = "Ej. Filtro de aire")
                    CampoFormulario(material.cantidad, { viewModel.cambiarMaterial(orden.id, indice, cantidad = it) },
                        "Cantidad", viewModel.errorMaterial(orden.id, indice, true), ejemplo = "Ej. 1 o 0,5")
                    TextButton(onClick = { viewModel.quitarMaterial(orden.id, indice) }) { Text("Quitar material") }
                }
                OutlinedButton(onClick = { viewModel.agregarMaterial(orden.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Agregar material")
                }
                Text("Materiales opcionales.", style = MaterialTheme.typography.bodySmall)
            } }
            item {
                SeccionFormulario("03 / EVIDENCIA FOTOGRÁFICA") {
                Text("Agrega una fotografía del trabajo realizado.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Antes", "Después", "Otra").forEach { tipo ->
                        FilterChip(selected = tipoFoto == tipo, onClick = { tipoFoto = tipo }, label = { Text(tipo) })
                    }
                }
                OutlinedButton(onClick = tomarFoto, modifier = Modifier.fillMaxWidth()) {
                    Text("Tomar foto · $tipoFoto")
                }
                OutlinedButton(onClick = elegirFoto, modifier = Modifier.fillMaxWidth()) {
                    Text("Elegir de galería · $tipoFoto")
                }
                Text("La foto queda guardada en esta orden.",
                    style = MaterialTheme.typography.bodySmall)
                }
            }
            if (errorFoto.isNotBlank()) item { ErrorSeleccion(errorFoto) }
        }
        if (orden.fotosEvidencia.isNotEmpty()) item {
            SeccionFormulario("Evidencia del trabajo") {
                orden.fotosEvidencia.chunked(2).forEachIndexed { fila, nombres ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        nombres.forEachIndexed { columna, nombre ->
                            androidx.compose.runtime.key(nombre) {
                                Column(Modifier.weight(1f)) {
                                    val etiqueta = "Foto ${fila * 2 + columna + 1} · ${when {
                                        nombre.contains("_Antes_") -> "Antes"
                                        nombre.contains("_Después_") -> "Después"
                                        else -> "Otra"
                                    }}"
                                    com.example.proyecto_acis.ui.components.FotoMiniatura(nombre, etiqueta,
                                        onEliminar = if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA)
                                            ({ fotoEliminar = nombre }) else null)
                                }
                            }
                        }
                        if (nombres.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (perfil == Perfil.CLIENTE && orden.estado == EstadoOt.PENDIENTE_CONFORMIDAD) {
            item { TituloSeccion("¿Todo está conforme?", "Aprueba o indica qué falta revisar.") }
            item { CampoFormulario(formulario.observacion, { viewModel.cambiarObservacion(orden.id, it) },
                "Tu observación", errorObservacion, minLines = 3, ejemplo = "Escribe aquí si algo necesita revisión") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirmarAprobacion = true }, modifier = Modifier.fillMaxWidth()) { Text("Aprobar trabajo") }
                    OutlinedButton(onClick = { viewModel.ejecutar {
                        viewModel.observar(orden.id, formulario.observacion)
                     } }, modifier = Modifier.fillMaxWidth()) { Text("Dejar observación") }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.OBSERVADA) {
            item { TituloSeccion("Responder observación", "El cliente dejó un comentario sobre la visita") }
            item { CampoFormulario(formulario.respuesta, { viewModel.cambiarRespuesta(orden.id, it) }, "Respuesta al cliente",
                errorRespuesta, minLines = 2) }
            item { Button(onClick = { viewModel.ejecutar {
                viewModel.responderObservacion(orden.id, formulario.respuesta)
             } }, modifier = Modifier.fillMaxWidth()) { Text("Enviar respuesta") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA && escribiendoInforme) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.ejecutar {
                        viewModel.guardarBorrador(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                        escribiendoInforme = false
                    } }, modifier = Modifier.weight(1f)) { Text("Volver a la visita") }
                    OutlinedButton(onClick = { viewModel.ejecutar {
                        viewModel.guardarBorrador(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                    } }, modifier = Modifier.weight(1f)) { Text("Guardar borrador") }
                }
                Button(onClick = { viewModel.ejecutar {
                    viewModel.finalizarVisita(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                } }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar visita") }
            }
        }
        if (mensaje.isNotBlank()) item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium) {
                Text(mensaje, modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
    }
}
