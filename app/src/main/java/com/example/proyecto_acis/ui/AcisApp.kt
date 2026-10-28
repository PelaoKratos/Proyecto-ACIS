package com.example.proyecto_acis.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import com.example.proyecto_acis.data.Perfil
import com.example.proyecto_acis.data.PrioridadOt
import com.example.proyecto_acis.data.TipoOt
import com.example.proyecto_acis.data.obtenerUbicacionCheckIn
import java.util.Calendar
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun ListaOrdenes(
    ordenes: List<OrdenTrabajo>,
    perfil: Perfil,
    onCrear: () -> Unit,
    onSeleccionar: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (perfil) {
                                Perfil.PLANIFICADOR -> "Órdenes de trabajo"
                                Perfil.TECNICO -> "Mis órdenes"
                                Perfil.CLIENTE -> "Revisión de trabajos"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text("Actividad más reciente primero", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.medium) {
                        Text("${ordenes.size}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (perfil == Perfil.PLANIFICADOR) {
                    Button(onClick = onCrear, modifier = Modifier.fillMaxWidth()) { Text("Crear nueva OT") }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium) {
                    Text("Los perfiles comparten datos en este dispositivo. Aún no hay cuentas ni sincronización.",
                        modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (ordenes.isEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sin órdenes por ahora", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (perfil) {
                                Perfil.PLANIFICADOR -> "Crea la primera orden para comenzar."
                                Perfil.TECNICO -> "Cuando te asignen una OT, aparecerá aquí."
                                Perfil.CLIENTE -> "Los informes pendientes de conformidad aparecerán aquí."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        items(ordenes, key = { it.id }) { orden ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(orden.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(orden.id, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        AnimatedContent(targetState = orden.estado, label = "estado de OT") { estado ->
                            EstadoBadge(estado)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(orden.local, style = MaterialTheme.typography.titleLarge)
                        Text(orden.equipo, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(orden.tipo.etiqueta, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(orden.fechaProgramada, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        PrioridadBadge(orden.prioridad)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CrearOrden(
    viewModel: OrdenesViewModel,
    onVolver: () -> Unit,
    onCrear: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val ahora = remember { Calendar.getInstance() }
    val datos = viewModel.datosCrear
    val errores = viewModel.erroresCrear
    var dia by rememberSaveable { mutableStateOf(ahora.get(Calendar.DAY_OF_MONTH)) }
    var mes by rememberSaveable { mutableStateOf(ahora.get(Calendar.MONTH)) }
    var anio by rememberSaveable { mutableStateOf(ahora.get(Calendar.YEAR)) }
    var hora by rememberSaveable { mutableStateOf(ahora.get(Calendar.HOUR_OF_DAY)) }
    var minuto by rememberSaveable { mutableStateOf(ahora.get(Calendar.MINUTE)) }

    LazyColumn(modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nueva orden de trabajo", style = MaterialTheme.typography.headlineSmall)
                Text("Completa los datos para programar la visita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TituloSeccion("Ubicación y equipo", "Identifica dónde se realizará el trabajo") }
        item { CampoFormulario(datos.local, viewModel::cambiarLocal, "Local", errores.local) }
        item { CampoFormulario(datos.equipo, viewModel::cambiarEquipo, "Equipo", errores.equipo) }
        item { TituloSeccion("Trabajo solicitado", "Describe lo que necesita el cliente") }
        item { CampoFormulario(datos.descripcion, viewModel::cambiarDescripcion,
            "Descripción del trabajo", errores.descripcion, minLines = 3) }
        item { Text("Tipo de trabajo", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TipoOt.entries.filter { it != TipoOt.SIN_ESPECIFICAR }.forEach { tipo ->
                    FilterChip(selected = datos.tipo == tipo,
                        onClick = { viewModel.cambiarTipo(tipo) }, label = { Text(tipo.etiqueta) })
                }
            }
        }
        if (errores.tipo != null) item { ErrorSeleccion(errores.tipo) }
        item { Text("Prioridad", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PrioridadOt.entries.filter { it != PrioridadOt.SIN_ESPECIFICAR }.forEach { prioridad ->
                    FilterChip(selected = datos.prioridad == prioridad,
                        onClick = { viewModel.cambiarPrioridad(prioridad) }, label = { Text(prioridad.etiqueta) })
                }
            }
        }
        if (errores.prioridad != null) item { ErrorSeleccion(errores.prioridad) }
        item { TituloSeccion("Programación", "Elige una fecha y hora para la visita") }
        item {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    DatePickerDialog(context, { _, nuevoAnio, nuevoMes, nuevoDia ->
                        anio = nuevoAnio
                        mes = nuevoMes
                        dia = nuevoDia
                        viewModel.cambiarFecha(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d/%02d/%04d", dia, mes + 1, anio,
                        ))
                    }, anio, mes, dia).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (datos.fecha.isNotBlank()) "Fecha: ${datos.fecha}" else "Seleccionar fecha") }
        }
        if (errores.fecha != null) item { ErrorSeleccion(errores.fecha) }
        item {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    TimePickerDialog(context, { _, nuevaHora, nuevoMinuto ->
                        hora = nuevaHora
                        minuto = nuevoMinuto
                        viewModel.cambiarHora(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d:%02d", hora, minuto,
                        ))
                    }, hora, minuto, true).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (datos.hora.isNotBlank()) "Hora: ${datos.hora}" else "Seleccionar hora") }
        }
        if (errores.hora != null) item { ErrorSeleccion(errores.hora) }
        if (viewModel.errorGuardarCrear) item {
            Text("No se pudo guardar la OT.", color = MaterialTheme.colorScheme.error)
        }
        item {
            Button(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onCrear()
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar OT") }
        }
    }
}

@Composable
internal fun DetalleOrden(
    orden: OrdenTrabajo,
    perfil: Perfil,
    viewModel: OrdenesViewModel,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    val scopeUbicacion = rememberCoroutineScope()
    var buscandoUbicacion by rememberSaveable(orden.id) { mutableStateOf(false) }
    val iniciarConUbicacion: () -> Unit = {
        buscandoUbicacion = true
        scopeUbicacion.launch {
            val ubicacion = obtenerUbicacionCheckIn(context)
            viewModel.iniciarVisita(orden.id, ubicacion)
            buscandoUbicacion = false
        }
    }
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) iniciarConUbicacion()
        else viewModel.iniciarVisita(orden.id, null)
    }
    var fotoPendiente by rememberSaveable(orden.id) { mutableStateOf("") }
    var errorCamara by rememberSaveable(orden.id) { mutableStateOf("") }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        val nombre = fotoPendiente
        fotoPendiente = ""
        if (nombre.isNotBlank()) {
            val archivo = File(File(context.filesDir, "evidencias"), nombre)
            if (guardada && viewModel.guardarFoto(orden.id, nombre)) {
                errorCamara = ""
            } else {
                archivo.delete()
                if (guardada) errorCamara = "No se pudo guardar la foto."
            }
        }
    }
    val formulario = viewModel.formularioDetalle(orden)
    val erroresInforme = viewModel.erroresInforme(orden.id, formulario.diagnostico, formulario.actividad)
    val errorObservacion = viewModel.errorObservacion(orden.id, formulario.observacion)
    val errorRespuesta = viewModel.errorRespuesta(orden.id, formulario.respuesta)
    val mensaje = viewModel.mensaje(orden.id)

    LazyColumn(modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(orden.id, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(orden.local, style = MaterialTheme.typography.headlineMedium)
                Text(orden.equipo, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedContent(targetState = orden.estado, label = "estado del detalle") { estado ->
                    EstadoBadge(estado)
                }
            }
        }
        item { TituloSeccion("Datos de la orden", "Información y avance de la visita") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CampoDetalle("Equipo", orden.equipo)
                    CampoDetalle("Tipo de trabajo", orden.tipo.etiqueta)
                    PrioridadBadge(orden.prioridad)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CampoDetalle("Fecha programada", orden.fechaProgramada)
                    CampoDetalle("Trabajo solicitado", orden.descripcion)
                    if (orden.tecnico.isNotBlank()) CampoDetalle("Técnico", orden.tecnico)
                    if (orden.checkIn.isNotBlank() || orden.checkOut.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (orden.checkIn.isNotBlank()) CampoDetalle("Check-in", orden.checkIn)
                    if (orden.ubicacionCheckIn.isNotBlank()) CampoDetalle("Ubicación del check-in", orden.ubicacionCheckIn)
                    if (orden.checkOut.isNotBlank()) CampoDetalle("Check-out", orden.checkOut)
                    if (orden.diagnostico.isNotBlank()) CampoDetalle("Diagnóstico", orden.diagnostico)
                    if (orden.actividad.isNotBlank()) CampoDetalle("Actividad", orden.actividad)
                    if (orden.materiales.isNotBlank()) CampoDetalle("Materiales", orden.materiales)
                    if (orden.observacionCliente.isNotBlank()) CampoDetalle("Observación del cliente", orden.observacionCliente)
                    if (orden.respuestaTecnico.isNotBlank()) CampoDetalle("Respuesta del técnico", orden.respuestaTecnico)
                    if (orden.fechaConformidad.isNotBlank()) CampoDetalle("Conformidad", orden.fechaConformidad)
                    if (orden.fotoEvidencia.isNotBlank()) {
                        val archivo = File(File(context.filesDir, "evidencias"), orden.fotoEvidencia)
                        val foto = remember(orden.fotoEvidencia) {
                            val opciones = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeFile(archivo.path, opciones)?.asImageBitmap()
                        }
                        if (foto != null) {
                            Text("Foto de evidencia", style = MaterialTheme.typography.titleSmall)
                            Image(foto, contentDescription = "Evidencia de ${orden.id}",
                                modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
                        } else {
                            Text("Foto de evidencia no disponible", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (perfil == Perfil.PLANIFICADOR && orden.estado == EstadoOt.CREADA) {
            item { Button(onClick = { viewModel.asignar(orden.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Asignar a técnico demo")
            } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ASIGNADA) {
            item { Button(onClick = { viewModel.aceptar(orden.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Aceptar OT")
            } }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.ACEPTADA) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se pedirá ubicación aproximada para el check-in. Puedes continuar si no la permites.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) iniciarConUbicacion()
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, enabled = !buscandoUbicacion, modifier = Modifier.fillMaxWidth()) {
                        Text(if (buscandoUbicacion) "Buscando ubicación..." else "Iniciar visita y registrar check-in")
                    }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.EN_VISITA) {
            item { TituloSeccion("Informe técnico", "Registra el trabajo realizado en esta visita") }
            item {
                OutlinedButton(onClick = {
                    var archivoNuevo: File? = null
                    try {
                        val carpeta = File(context.filesDir, "evidencias")
                        if (!carpeta.exists()) carpeta.mkdirs()
                        val archivo = File(carpeta, "${orden.id}_${UUID.randomUUID()}.jpg")
                        archivo.createNewFile()
                        archivoNuevo = archivo
                        fotoPendiente = archivo.name
                        errorCamara = ""
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", archivo,
                        )
                        camara.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorCamara = "No hay una cámara disponible en este dispositivo."
                    } catch (_: Exception) {
                        archivoNuevo?.delete()
                        fotoPendiente = ""
                        errorCamara = "No se pudo abrir la cámara."
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (orden.fotoEvidencia.isBlank()) "Tomar foto de evidencia" else "Cambiar foto de evidencia")
                }
            }
            if (errorCamara.isNotBlank()) item { Text(errorCamara, color = MaterialTheme.colorScheme.error) }
            item { CampoFormulario(formulario.diagnostico, { viewModel.cambiarDiagnostico(orden.id, it) }, "Diagnóstico",
                erroresInforme.diagnostico, minLines = 2) }
            item { CampoFormulario(formulario.actividad, { viewModel.cambiarActividad(orden.id, it) }, "Actividad realizada",
                erroresInforme.actividad, minLines = 2) }
            item { CampoFormulario(formulario.materiales, { viewModel.cambiarMateriales(orden.id, it) }, "Materiales usados (opcional)") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        viewModel.guardarBorrador(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Guardar borrador") }
                    Button(onClick = {
                        viewModel.finalizarVisita(orden.id, formulario.diagnostico, formulario.actividad, formulario.materiales)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar visita") }
                }
            }
        }
        if (perfil == Perfil.CLIENTE && orden.estado == EstadoOt.PENDIENTE_CONFORMIDAD) {
            item { TituloSeccion("Revisar conformidad", "Confirma o comenta el trabajo realizado") }
            item { CampoFormulario(formulario.observacion, { viewModel.cambiarObservacion(orden.id, it) },
                "Observación si no estás conforme", errorObservacion) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.aprobar(orden.id) }, modifier = Modifier.fillMaxWidth()) { Text("Aprobar") }
                    OutlinedButton(onClick = {
                        viewModel.observar(orden.id, formulario.observacion)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Observar") }
                }
            }
        }
        if (perfil == Perfil.TECNICO && orden.estado == EstadoOt.OBSERVADA) {
            item { TituloSeccion("Responder observación", "El cliente dejó un comentario sobre la visita") }
            item { CampoFormulario(formulario.respuesta, { viewModel.cambiarRespuesta(orden.id, it) }, "Respuesta al cliente",
                errorRespuesta, minLines = 2) }
            item { Button(onClick = {
                viewModel.responderObservacion(orden.id, formulario.respuesta)
            }, modifier = Modifier.fillMaxWidth()) { Text("Enviar respuesta") } }
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

@Composable
private fun TituloSeccion(titulo: String, descripcion: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(descripcion, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String? = null,
    minLines: Int = 1,
) {
    val traerAlFrente = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(capitalizarOraciones(it)) },
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        isError = error != null,
        trailingIcon = if (error != null) {{
            Icon(painterResource(android.R.drawable.ic_dialog_alert), contentDescription = "Error en $etiqueta",
                tint = MaterialTheme.colorScheme.error)
        }} else null,
        supportingText = error?.let { mensaje -> { Text(mensaje) } },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth()
            .bringIntoViewRequester(traerAlFrente)
            .onFocusChanged { foco ->
                if (foco.isFocused) scope.launch { traerAlFrente.bringIntoView() }
            },
    )
}

private fun capitalizarOraciones(texto: String): String = buildString {
    var inicio = true
    texto.forEach { letra ->
        append(if (inicio && letra.isLetter()) letra.uppercaseChar() else letra)
        if (letra.isLetter()) inicio = false
        if (letra == '.' || letra == '!' || letra == '?' || letra == '\n') inicio = true
    }
}

@Composable
private fun ErrorSeleccion(mensaje: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(android.R.drawable.ic_dialog_alert), contentDescription = null,
            tint = MaterialTheme.colorScheme.error)
        Text(mensaje, color = MaterialTheme.colorScheme.error)
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

@Composable
private fun EstadoBadge(estado: EstadoOt) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (estado) {
        EstadoOt.CREADA -> colores.surfaceVariant to colores.onSurfaceVariant
        EstadoOt.ASIGNADA, EstadoOt.ACEPTADA, EstadoOt.EN_VISITA ->
            colores.primaryContainer to colores.onPrimaryContainer
        EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA ->
            colores.tertiaryContainer to colores.onTertiaryContainer
        EstadoOt.CERRADA -> colores.secondaryContainer to colores.onSecondaryContainer
    }
    BadgeTexto(estado.etiqueta, fondo, texto)
}

@Composable
private fun PrioridadBadge(prioridad: PrioridadOt) {
    val colores = MaterialTheme.colorScheme
    val (fondo, texto) = when (prioridad) {
        PrioridadOt.ALTA -> colores.errorContainer to colores.onErrorContainer
        PrioridadOt.MEDIA -> colores.tertiaryContainer to colores.onTertiaryContainer
        PrioridadOt.BAJA -> colores.secondaryContainer to colores.onSecondaryContainer
        PrioridadOt.SIN_ESPECIFICAR -> colores.surfaceVariant to colores.onSurfaceVariant
    }
    BadgeTexto("Prioridad ${prioridad.etiqueta.lowercase()}", fondo, texto)
}

@Composable
private fun BadgeTexto(etiqueta: String, fondo: Color, texto: Color) {
    Surface(
        color = fondo,
        contentColor = texto,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            etiqueta,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
