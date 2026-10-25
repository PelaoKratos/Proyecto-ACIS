package com.example.proyecto_acis.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdenesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("acis_ordenes_locales", Context.MODE_PRIVATE)
    private val database = OrdenesDb(context)
    private val _ordenes = MutableStateFlow(cargarOrdenes())
    val ordenes = _ordenes.asStateFlow()

    fun crear(
        local: String,
        equipo: String,
        descripcion: String,
        fecha: String,
        tipo: TipoOt,
        prioridad: PrioridadOt,
    ): String? {
        if (listOf(local, equipo, descripcion, fecha).any { it.isBlank() }) return null
        if (tipo == TipoOt.SIN_ESPECIFICAR || prioridad == PrioridadOt.SIN_ESPECIFICAR) return null
        val siguiente = (_ordenes.value.mapNotNull { it.id.removePrefix("OT-").toIntOrNull() }.maxOrNull() ?: 1000) + 1
        val id = "OT-$siguiente"
        val nueva = OrdenTrabajo(
            id = id,
            local = local.trim(),
            equipo = equipo.trim(),
            descripcion = descripcion.trim(),
            fechaProgramada = fecha.trim(),
            tipo = tipo,
            prioridad = prioridad,
        )
        if (!database.insertar(nueva)) return null
        _ordenes.value = _ordenes.value + nueva
        return id
    }

    fun asignar(id: String): Boolean = cambiar(id, EstadoOt.CREADA) {
        it.copy(estado = EstadoOt.ASIGNADA, tecnico = TECNICO_DEMO)
    }

    fun aceptar(id: String): Boolean = cambiar(id, EstadoOt.ASIGNADA) {
        it.copy(estado = EstadoOt.ACEPTADA)
    }

    fun iniciarVisita(id: String): Boolean = cambiar(id, EstadoOt.ACEPTADA) {
        it.copy(estado = EstadoOt.EN_VISITA, checkIn = ahora())
    }

    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String): Boolean =
        cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(diagnostico = diagnostico.trim(), actividad = actividad.trim(), materiales = materiales.trim())
        }

    fun enviarInforme(id: String, diagnostico: String, actividad: String, materiales: String): Boolean {
        if (diagnostico.isBlank() || actividad.isBlank()) return false
        return cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(
                estado = EstadoOt.PENDIENTE_CONFORMIDAD,
                diagnostico = diagnostico.trim(),
                actividad = actividad.trim(),
                materiales = materiales.trim(),
                checkOut = ahora(),
            )
        }
    }

    fun observar(id: String, observacion: String): Boolean {
        if (observacion.isBlank()) return false
        return cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD) {
            it.copy(estado = EstadoOt.OBSERVADA, observacionCliente = observacion.trim())
        }
    }

    fun responderObservacion(id: String, respuesta: String): Boolean {
        if (respuesta.isBlank()) return false
        return cambiar(id, EstadoOt.OBSERVADA) {
            it.copy(estado = EstadoOt.PENDIENTE_CONFORMIDAD, respuestaTecnico = respuesta.trim())
        }
    }

    fun aprobar(id: String): Boolean = cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD) {
        it.copy(estado = EstadoOt.CERRADA, fechaConformidad = ahora())
    }

    private fun cambiar(id: String, estadoRequerido: EstadoOt, cambio: (OrdenTrabajo) -> OrdenTrabajo): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado == estadoRequerido } ?: return false
        val nueva = cambio(actual)
        if (!database.actualizar(nueva, estadoRequerido)) return false
        _ordenes.value = _ordenes.value.map { if (it.id == id) nueva else it }
        return true
    }

    private fun cargarOrdenes(): List<OrdenTrabajo> {
        val actuales = database.leer()
        if (actuales.isNotEmpty()) return actuales
        val anteriores = leerOrdenesAnteriores()
        if (anteriores.isNotEmpty()) database.guardarTodas(anteriores)
        return anteriores
    }

    private fun leerOrdenesAnteriores(): List<OrdenTrabajo> = try {
        val json = JSONArray(preferences.getString("ordenes", "[]"))
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            OrdenTrabajo(
                id = item.getString("id"),
                local = item.optString("local"),
                equipo = item.optString("equipo"),
                descripcion = item.optString("descripcion"),
                fechaProgramada = item.optString("fechaProgramada"),
                tipo = TipoOt.entries.firstOrNull { it.name == item.optString("tipo") }
                    ?: TipoOt.SIN_ESPECIFICAR,
                prioridad = PrioridadOt.entries.firstOrNull { it.name == item.optString("prioridad") }
                    ?: PrioridadOt.SIN_ESPECIFICAR,
                estado = EstadoOt.valueOf(item.getString("estado")),
                tecnico = item.optString("tecnico"),
                checkIn = item.optString("checkIn"),
                checkOut = item.optString("checkOut"),
                diagnostico = item.optString("diagnostico"),
                actividad = item.optString("actividad"),
                materiales = item.optString("materiales"),
                observacionCliente = item.optString("observacionCliente"),
                respuestaTecnico = item.optString("respuestaTecnico"),
                fechaConformidad = item.optString("fechaConformidad"),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        const val TECNICO_DEMO = "Técnico demo"

        private fun ahora(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
            .format(Date())
    }
}
