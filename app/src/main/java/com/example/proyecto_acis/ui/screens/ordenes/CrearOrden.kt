package com.example.proyecto_acis.ui.screens.ordenes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.ui.components.CampoDetalle
import com.example.proyecto_acis.ui.components.CampoFormulario
import com.example.proyecto_acis.ui.components.ErrorSeleccion
import com.example.proyecto_acis.ui.components.SeccionFormulario
import com.example.proyecto_acis.ui.components.SelectorConTexto
import com.example.proyecto_acis.ui.components.StepProgress
import com.example.proyecto_acis.ui.components.WeatherBanner
import com.example.proyecto_acis.ui.viewmodel.ClimaViewModel
import com.example.proyecto_acis.ui.viewmodel.OrdenesViewModel
import java.util.Calendar
import java.util.Locale

@Composable
internal fun CrearOrden(
    viewModel: OrdenesViewModel,
    climaViewModel: ClimaViewModel,
    onVolver: () -> Unit,
    onCrear: () -> Unit,
    editando: Boolean = false,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val catastro by viewModel.catastro.collectAsState()
    val estado by viewModel.creacion.collectAsState()
    val datos = estado.datos
    val errores = estado.errores
    val paso = estado.paso
    val listaPasos = rememberLazyListState()
    LaunchedEffect(paso) { listaPasos.scrollToItem(0) }
    Column(Modifier.fillMaxSize().imePadding()) {
    LazyColumn(state = listaPasos, modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (editando) "Editar orden" else "Nueva orden / OT", style = MaterialTheme.typography.headlineMedium)
                Text("Completa los datos para programar la visita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { StepProgress(paso, 3) }
        if (paso == 1) item {
            SeccionFormulario("01 / LOCAL Y EQUIPO") {
                Text("Selecciona un local y su equipo. También puedes registrar otro.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectorConTexto(datos.local, viewModel::cambiarLocal, "Local", viewModel.localesDisponibles(catastro), errores.local)
                SelectorConTexto(datos.equipo, viewModel::cambiarEquipo, "Equipo", viewModel.equiposDisponibles(catastro), errores.equipo,
                    clave = datos.local)
                Text(viewModel.ultimaMantencion(catastro)?.let { "Última mantención registrada: $it" }
                    ?: "Sin mantenciones cerradas registradas para este equipo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (paso == 2) item {
            SeccionFormulario("02 / TRABAJO SOLICITADO") {
                CampoFormulario(datos.descripcion, viewModel::cambiarDescripcion,
                    "Descripción del trabajo", errores.descripcion, minLines = 3, ejemplo = "Ej. Revisar porque no enfría")
                Text("Tipo de trabajo", style = MaterialTheme.typography.titleSmall)
                TipoOt.entries.filter { it != TipoOt.SIN_ESPECIFICAR }.forEach { tipo ->
                    FilterChip(selected = datos.tipo == tipo, modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.cambiarTipo(tipo) },
                        label = { Text(if (datos.tipo == tipo) "✓ ${tipo.etiqueta}" else tipo.etiqueta) })
                }
                if (errores.tipo != null) ErrorSeleccion(errores.tipo)
                Text("Prioridad", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PrioridadOt.entries.filter { it != PrioridadOt.SIN_ESPECIFICAR }.forEach { prioridad ->
                        FilterChip(selected = datos.prioridad == prioridad, onClick = { viewModel.cambiarPrioridad(prioridad) },
                            label = { Text(if (datos.prioridad == prioridad) "✓ ${prioridad.etiqueta}" else prioridad.etiqueta) })
                    }
                }
                if (errores.prioridad != null) ErrorSeleccion(errores.prioridad)
            }
        }
        if (paso == 3) item {
            SeccionFormulario("03 / PROGRAMAR VISITA") {
                Text("Selecciona la fecha y hora de la visita", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val fechaInicial = Calendar.getInstance().apply {
                        TiempoVisita.fecha("${datos.fecha} 00:00")?.let { timeInMillis = it }
                    }
                    DatePickerDialog(context, { _, anio, mes, dia ->
                        viewModel.cambiarFecha(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d/%02d/%04d", dia, mes + 1, anio,
                        ))
                    }, fechaInicial.get(Calendar.YEAR), fechaInicial.get(Calendar.MONTH),
                        fechaInicial.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (datos.fecha.isNotBlank()) datos.fecha else "Fecha") }
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val horaInicial = Calendar.getInstance().apply {
                        TiempoVisita.fecha("01/01/2000 ${datos.hora}")?.let { timeInMillis = it }
                    }
                    TimePickerDialog(context, { _, hora, minuto ->
                        viewModel.cambiarHora(String.format(
                            Locale.forLanguageTag("es-CL"), "%02d:%02d", hora, minuto,
                        ))
                    }, horaInicial.get(Calendar.HOUR_OF_DAY), horaInicial.get(Calendar.MINUTE), true).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (datos.hora.isNotBlank()) datos.hora else "Hora") }
                }
                if (errores.fecha != null) ErrorSeleccion(errores.fecha)
                if (errores.hora != null) ErrorSeleccion(errores.hora)
                Text("La asignación del técnico se realiza después de guardar la orden.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (paso == 3) item {
            WeatherBanner(
                ReglasClima.formulario(datos.local, datos.equipo,
                    "${datos.fecha} ${datos.hora}", catastro), climaViewModel)
        }
        if (estado.errorGuardado) item {
            Text("No se pudo guardar la OT.", color = MaterialTheme.colorScheme.error)
        }
        if (paso == 3) item {
            SeccionFormulario("Resumen") {
                CampoDetalle("Local y equipo", "${datos.local} · ${datos.equipo}")
                CampoDetalle("Trabajo", datos.descripcion)
                CampoDetalle("Tipo y prioridad", "${datos.tipo?.etiqueta.orEmpty()} · ${datos.prioridad?.etiqueta.orEmpty()}")
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = {
            focusManager.clearFocus()
            if (paso > 1) viewModel.retrocederPasoCrear() else onVolver()
        }, modifier = Modifier.weight(1f)) { Text(if (paso > 1) "Anterior" else "Cancelar") }
        Button(onClick = {
            focusManager.clearFocus()
            keyboardController?.hide()
            if (paso < 3) viewModel.avanzarPasoCrear()
            else if (viewModel.validarPasoCrear()) onCrear()
        }, modifier = Modifier.weight(1f)) {
            Text(if (paso < 3) "Siguiente" else if (editando) "Guardar cambios" else "Guardar OT")
        }
    }
    }
}
