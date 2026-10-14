package com.example.proyecto_acis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import com.example.proyecto_acis.data.OrdenesRepository
import com.example.proyecto_acis.data.Perfil

@Composable
fun AcisApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { OrdenesRepository(context.applicationContext) }
    var perfil by rememberSaveable { mutableStateOf(Perfil.PLANIFICADOR) }
    var ordenSeleccionadaId by rememberSaveable { mutableStateOf<String?>(null) }
    var creando by rememberSaveable { mutableStateOf(false) }
    val ordenSeleccionada = repository.ordenes.firstOrNull { it.id == ordenSeleccionadaId }

    BackHandler(enabled = creando || ordenSeleccionada != null) {
        creando = false
        ordenSeleccionadaId = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("ACIS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Primer recorrido · prototipo local", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Perfil.entries.forEach { opcion ->
                    FilterChip(
                        selected = perfil == opcion,
                        onClick = {
                            perfil = opcion
                            creando = false
                            ordenSeleccionadaId = null
                        },
                        label = { Text(opcion.etiqueta) },
                    )
                }
            }
        }
        HorizontalDivider()
        when {
            creando -> CrearOrden(
                onVolver = { creando = false },
                onCrear = { local, equipo, descripcion, fecha ->
                    val id = repository.crear(local, equipo, descripcion, fecha)
                    if (id != null) {
                        creando = false
                        ordenSeleccionadaId = id
                    }
                    id != null
                },
            )
            ordenSeleccionada != null -> DetalleOrden(
                orden = ordenSeleccionada,
                perfil = perfil,
                repository = repository,
                onVolver = { ordenSeleccionadaId = null },
            )
            else -> ListaOrdenes(
                ordenes = repository.ordenes.filter { orden ->
                    when (perfil) {
                        Perfil.PLANIFICADOR -> true
                        Perfil.TECNICO -> orden.tecnico == OrdenesRepository.TECNICO_DEMO
                        Perfil.CLIENTE -> orden.estado in setOf(
                            EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA,
                        )
                    }
                },
                perfil = perfil,
                onCrear = { creando = true },
                onSeleccionar = { ordenSeleccionadaId = it },
            )
        }
    }
}

@Composable
private fun ListaOrdenes(
    ordenes: List<OrdenTrabajo>,
    perfil: Perfil,
    onCrear: () -> Unit,
    onSeleccionar: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (perfil) {
                        Perfil.PLANIFICADOR -> "Órdenes de trabajo"
                        Perfil.TECNICO -> "Mis órdenes asignadas"
                        Perfil.CLIENTE -> "Órdenes para revisión"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (perfil == Perfil.PLANIFICADOR) Button(onClick = onCrear) { Text("Crear OT") }
                Text("Los perfiles comparten datos en este dispositivo. Aún no hay cuentas ni sincronización.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (ordenes.isEmpty()) {
            item {
                Text(
                    when (perfil) {
                        Perfil.PLANIFICADOR -> "Todavía no hay órdenes. Crea la primera OT."
                        Perfil.TECNICO -> "No tienes órdenes asignadas. Cambia a Planificador y asigna una OT."
                        Perfil.CLIENTE -> "No hay informes pendientes de conformidad."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(ordenes, key = { it.id }) { orden ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(orden.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${orden.id} · ${orden.estado.etiqueta}", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text(orden.local, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(orden.equipo, style = MaterialTheme.typography.bodyMedium)
                    Text(orden.fechaProgramada, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CrearOrden(
    onVolver: () -> Unit,
    onCrear: (String, String, String, String) -> Boolean,
) {
    var local by rememberSaveable { mutableStateOf("") }
    var equipo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onVolver) { Text("← Volver") } }
        item { Text("Nueva orden de trabajo", style = MaterialTheme.typography.headlineSmall) }
        item { OutlinedTextField(local, { local = it }, label = { Text("Local") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(equipo, { equipo = it }, label = { Text("Equipo") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción del trabajo") },
            minLines = 3, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha y hora programada") },
            placeholder = { Text("Ej.: 15/10/2026 09:00") }, modifier = Modifier.fillMaxWidth()) }
        if (error) item { Text("Completa todos los campos para crear la OT.", color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = { error = !onCrear(local, equipo, descripcion, fecha) }) { Text("Guardar OT") } }
    }
}

@Composable
private fun DetalleOrden(
    orden: OrdenTrabajo,
    perfil: Perfil,
    repository: OrdenesRepository,
    onVolver: () -> Unit,
) {
    var diagnostico by rememberSaveable(orden.id) { mutableStateOf(orden.diagnostico) }
    var actividad by rememberSaveable(orden.id) { mutableStateOf(orden.actividad) }
    var materiales by rememberSaveable(orden.id) { mutableStateOf(orden.materiales) }
    var observacion by rememberSaveable(orden.id) { mutableStateOf("") }
    var respuesta by rememberSaveable(orden.id) { mutableStateOf("") }
    var mensaje by rememberSaveable(orden.id) { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onVolver) { Text("← Volver a órdenes") } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(orden.id, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(orden.estado.etiqueta, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoDetalle("Local", orden.local)
                    CampoDetalle("Equipo", orden.equipo)
                    CampoDetalle("Fecha programada", orden.fechaProgramada)
                    CampoDetalle("Trabajo solicitado", orden.descripcion)
                    if (orden.tecnico.isNotBlank()) CampoDetalle("Técnico", orden.tecnico)
                    if (orden.checkIn.isNotBlank()) CampoDetalle("Check-in", orden.checkIn)
                    if (orden.checkOut.isNotBlank()) CampoDetalle("Check-out", orden.checkOut)
                    if (orden.diagnostico.isNotBlank()) CampoDetalle("Diagnóstico", orden.diagnostico)
                    if (orden.actividad.isNotBlank()) CampoDetalle("Actividad", orden.actividad)
                    if (orden.materiales.isNotBlank()) CampoDetalle("Materiales", orden.materiales)
                    if (orden.observacionCliente.isNotBlank()) CampoDetalle("Observación del cliente", orden.observacionCliente)
                    if (orden.respuestaTecnico.isNotBlank()) CampoDetalle("Respuesta del técnico", orden.respuestaTecnico)
                    if (orden.fechaConformidad.isNotBlank()) CampoDetalle("Conformidad", orden.fechaConformidad)
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR && orden.estado == EstadoOt.CREADA) {
            item { Button(onClick = { repository.asignar(orden.id) }) { Text("Asignar a técnico demo") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
            item { Button(onClick = { repository.aceptar(orden.id) }) { Text("Aceptar OT") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
            item { Button(onClick = { repository.iniciarVisita(orden.id) }) { Text("Iniciar visita y registrar check-in") } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA) {
            item { Text("Informe técnico", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(diagnostico, { diagnostico = it }, label = { Text("Diagnóstico") },
                minLines = 2, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(actividad, { actividad = it }, label = { Text("Actividad realizada") },
                minLines = 2, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(materiales, { materiales = it }, label = { Text("Materiales usados (opcional)") },
                modifier = Modifier.fillMaxWidth()) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        mensaje = if (repository.guardarBorrador(orden.id, diagnostico, actividad, materiales))
                            "Borrador guardado en este dispositivo." else "No se pudo guardar el borrador."
                    }) { Text("Guardar borrador") }
                    Button(onClick = {
                        mensaje = if (repository.enviarInforme(orden.id, diagnostico, actividad, materiales))
                            "Informe disponible para revisión en este dispositivo." else
                            "Completa el diagnóstico y la actividad."
                    }) { Text("Finalizar visita") }
                }
            }
        }
        if (perfil == Perfil.CLIENTE && orden.estado == EstadoOt.PENDIENTE_CONFORMIDAD) {
            item { Text("Revisar conformidad", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(observacion, { observacion = it },
                label = { Text("Observación si no estás conforme") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { repository.aprobar(orden.id) }) { Text("Aprobar") }
                    OutlinedButton(onClick = {
                        mensaje = if (repository.observar(orden.id, observacion)) "Observación registrada."
                        else "Escribe una observación antes de enviarla."
                    }) { Text("Observar") }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.OBSERVADA) {
            item { Text("Responder observación", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(respuesta, { respuesta = it }, label = { Text("Respuesta al cliente") },
                minLines = 2, modifier = Modifier.fillMaxWidth()) }
            item { Button(onClick = {
                mensaje = if (repository.responderObservacion(orden.id, respuesta))
                    "Respuesta registrada; el cliente puede revisar nuevamente." else "Escribe una respuesta."
            }) { Text("Enviar respuesta") } }
        }
        if (mensaje.isNotBlank()) item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun CampoDetalle(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
