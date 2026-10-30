package com.example.proyecto_acis.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.proyecto_acis.domain.DatosEquipo
import com.example.proyecto_acis.domain.ValidadorEquipo
import com.example.proyecto_acis.domain.DatosCrearOt
import com.example.proyecto_acis.domain.ValidadorOt
import com.example.proyecto_acis.domain.ReglasEstadoOt

class OrdenesRepository(
    context: Context,
    private val database: OrdenesDb = OrdenesDb(context),
    nombrePreferencias: String = "acis_ordenes_locales",
) {
    private val preferences = context.getSharedPreferences(nombrePreferencias, Context.MODE_PRIVATE)
    private val fotos = File(context.filesDir, "evidencias")
    private val _ordenes = MutableStateFlow(cargarOrdenes())
    val ordenes = _ordenes.asStateFlow()
    private val _eventos = MutableStateFlow(database.leerEventos())
    val eventos = _eventos.asStateFlow()
    private val _catastro = MutableStateFlow(database.leerCatastro())
    val catastro = _catastro.asStateFlow()

    fun guardarEquipo(datos: DatosEquipo): Boolean {
        val vinculados = _ordenes.value.mapNotNull { it.equipoId }.toSet()
        if (ValidadorEquipo.validar(datos, _catastro.value, vinculados).hayErrores) return false
        if (!database.guardarEquipo(datos)) return false
        recargarDatos()
        return true
    }

    fun cargarDemostracion(): Boolean {
        if (_ordenes.value.isNotEmpty()) return false
        val primero = preferences.getInt("ultimoNumeroOt", 1000) + 1
        val ejemplos = DatosDemostracion.crear(primero, System.currentTimeMillis())
        if (!database.guardarTodas(ejemplos)) return false
        preferences.edit().putInt("ultimoNumeroOt", primero + ejemplos.size - 1)
            .putBoolean("migracionSQLiteCompleta", true).apply()
        recargarDatos()
        return true
    }

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
        val ultimo = _ordenes.value.mapNotNull { it.id.removePrefix("OT-").toIntOrNull() }.maxOrNull() ?: 1000
        val siguiente = maxOf(ultimo, preferences.getInt("ultimoNumeroOt", 1000)) + 1
        val id = "OT-$siguiente"
        val nueva = OrdenTrabajo(
            id = id,
            local = local.trim(),
            equipo = equipo.trim(),
            descripcion = descripcion.trim(),
            fechaProgramada = fecha.trim(),
            tipo = tipo,
            prioridad = prioridad,
            ultimaActualizacion = siguienteMarca(),
        )
        if (!fechaValida(fecha)) return null
        if (!database.insertar(nueva, evento(nueva, TipoEvento.CREACION))) return null
        preferences.edit().putInt("ultimoNumeroOt", siguiente).apply()
        recargarDatos()
        return id
    }

    fun asignar(id: String, tecnico: String = TECNICO_DEMO, hora: String? = null): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return false
        val horaElegida = hora ?: actual.fechaProgramada.substringAfter(" ", "")
        if (ValidadorOt.asignacion(tecnico, TECNICOS, horaElegida).hayErrores) return false
        val fecha = "${actual.fechaProgramada.substringBefore(" ")} $horaElegida"
        if (!fechaValida(fecha)) return false
        return cambiar(id, EstadoOt.CREADA, TipoEvento.ASIGNACION) { it.copy(estado = EstadoOt.ASIGNADA, tecnico = tecnico, fechaProgramada = fecha) }
    }

    fun aceptar(id: String): Boolean = cambiar(id, EstadoOt.ASIGNADA, TipoEvento.ACEPTACION) {
        it.copy(estado = EstadoOt.ACEPTADA)
    }

    fun rechazar(id: String): Boolean = cambiar(id, EstadoOt.ASIGNADA, TipoEvento.RECHAZO) {
        it.copy(estado = EstadoOt.CREADA, tecnico = "")
    }

    fun postergar(id: String, fecha: String): Boolean {
        if (ValidadorOt.postergacion(fecha) != null) return false
        return cambiar(id, EstadoOt.ASIGNADA, TipoEvento.POSTERGACION) { it.copy(fechaProgramada = fecha.trim()) }
    }

    fun actualizarDatos(id: String, datos: DatosCrearOt): Boolean {
        if (ValidadorOt.crear(datos).hayErrores || !fechaValida("${datos.fecha} ${datos.hora}")) return false
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in estadosEditables } ?: return false
        return cambiar(id, actual.estado, TipoEvento.EDICION) {
            it.copy(local = datos.local.trim(), equipo = datos.equipo.trim(), descripcion = datos.descripcion.trim(),
                tipo = datos.tipo!!, prioridad = datos.prioridad!!, fechaProgramada = "${datos.fecha} ${datos.hora}",
                equipoId = if (it.local == datos.local.trim() && it.equipo == datos.equipo.trim()) it.equipoId else null)
        }
    }

    fun eliminar(id: String): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in estadosEditables } ?: return false
        if (!database.eliminar(id, actual.estado)) return false
        _ordenes.value = _ordenes.value.filterNot { it.id == id }
        _eventos.value = database.leerEventos()
        actual.fotosEvidencia.forEach { File(fotos, it).delete() }
        return true
    }

    fun iniciarVisita(id: String, ubicacion: String?): Boolean = cambiar(id, EstadoOt.ACEPTADA, TipoEvento.LLEGADA) {
        it.copy(estado = EstadoOt.EN_VISITA, checkIn = ahora(), ubicacionCheckIn = ubicacion.orEmpty())
    }

    fun guardarUbicacion(id: String, ubicacion: String): Boolean {
        if (ubicacion.isBlank()) return false
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in setOf(EstadoOt.ACEPTADA, EstadoOt.EN_VISITA) } ?: return false
        return cambiar(id, actual.estado) { it.copy(ubicacionCheckIn = ubicacion) }
    }

    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String): Boolean =
        cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(diagnostico = diagnostico.trim(), actividad = actividad.trim(), materiales = materiales.trim())
        }

    fun guardarFoto(id: String, nombre: String): Boolean {
        val archivo = File(fotos, nombre)
        if (nombre != File(nombre).name || !archivo.isFile || archivo.length() == 0L) return false
        return cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(fotoEvidencia = (it.fotosEvidencia + nombre).distinct().joinToString("\n"))
        }
    }

    fun eliminarFoto(id: String, nombre: String): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return false
        if (nombre !in actual.fotosEvidencia) return false
        val guardado = cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(fotoEvidencia = it.fotosEvidencia.filterNot { foto -> foto == nombre }.joinToString("\n"))
        }
        if (guardado) File(fotos, nombre).delete()
        return guardado
    }

    fun enviarInforme(id: String, diagnostico: String, actividad: String, materiales: String): Boolean {
        if (diagnostico.isBlank() || actividad.isBlank()) return false
        return cambiar(id, EstadoOt.EN_VISITA, TipoEvento.INFORME) {
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
        return cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD, TipoEvento.OBSERVACION) {
            it.copy(estado = EstadoOt.OBSERVADA, observacionCliente = observacion.trim())
        }
    }

    fun responderObservacion(id: String, respuesta: String): Boolean {
        if (respuesta.isBlank()) return false
        return cambiar(id, EstadoOt.OBSERVADA, TipoEvento.RESPUESTA) {
            it.copy(estado = EstadoOt.PENDIENTE_CONFORMIDAD, respuestaTecnico = respuesta.trim())
        }
    }

    fun aprobar(id: String): Boolean = cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD, TipoEvento.CONFORMIDAD) {
        it.copy(estado = EstadoOt.CERRADA, fechaConformidad = ahora())
    }

    private fun cambiar(id: String, estadoRequerido: EstadoOt, tipoEvento: TipoEvento? = null, cambio: (OrdenTrabajo) -> OrdenTrabajo): Boolean {
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado == estadoRequerido } ?: return false
        val nueva = cambio(actual).copy(ultimaActualizacion = siguienteMarca())
        if (!ReglasEstadoOt.permite(actual.estado, nueva.estado)) return false
        val registro = tipoEvento?.let { evento(nueva, it, actual) }
        if (!database.actualizar(nueva, estadoRequerido, registro)) return false
        recargarDatos()
        return true
    }

    private fun recargarDatos() {
        _ordenes.value = database.leer()
        _eventos.value = database.leerEventos()
        _catastro.value = database.leerCatastro()
    }

    private fun evento(orden: OrdenTrabajo, tipo: TipoEvento, anterior: OrdenTrabajo? = null): EventoOt {
        val actor = when (tipo.perfil) {
            Perfil.PLANIFICADOR -> "Planificador demo"
            Perfil.CLIENTE -> "Cliente demo"
            Perfil.TECNICO -> (anterior?.tecnico ?: orden.tecnico).ifBlank { "Técnico sin identificar" }
            null -> "Sistema local"
        }
        val detalle = when (tipo) {
            TipoEvento.CREACION -> "Visita programada para ${orden.fechaProgramada}."
            TipoEvento.ASIGNACION -> "${orden.tecnico} · ${orden.fechaProgramada}"
            TipoEvento.RECHAZO -> "La orden queda disponible para reasignar."
            TipoEvento.POSTERGACION -> "Nueva programación: ${orden.fechaProgramada}"
            TipoEvento.EDICION -> "Se actualizaron los datos de la orden."
            TipoEvento.LLEGADA -> if (orden.ubicacionCheckIn.isBlank()) "Sin ubicación compartida." else "Ubicación registrada en el dispositivo."
            TipoEvento.INFORME -> "Informe disponible para revisión."
            TipoEvento.OBSERVACION -> orden.observacionCliente
            TipoEvento.RESPUESTA -> orden.respuestaTecnico
            else -> ""
        }
        return EventoOt(otId = orden.id, tipo = tipo, actor = actor,
            fechaMillis = orden.ultimaActualizacion, detalle = detalle)
    }

    private fun cargarOrdenes(): List<OrdenTrabajo> {
        val actuales = database.leer()
        if (actuales.isNotEmpty()) {
            preferences.edit().putBoolean("migracionSQLiteCompleta", true).apply()
            return actuales
        }
        if (preferences.getBoolean("migracionSQLiteCompleta", false)) return emptyList()
        val anteriores = leerOrdenesAnteriores()
        if (anteriores.isEmpty() || database.guardarTodas(anteriores)) {
            preferences.edit().putBoolean("migracionSQLiteCompleta", true).apply()
        }
        return anteriores
    }

    private fun siguienteMarca(): Long = maxOf(
        System.currentTimeMillis(),
        (_ordenes.value.maxOfOrNull { it.ultimaActualizacion } ?: 0L) + 1,
    )

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
        private val estadosEditables = setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA)
        private fun fechaValida(texto: String): Boolean = try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL")).apply { isLenient = false }
            val fecha = formato.parse(texto)
            fecha != null && formato.format(fecha) == texto
        } catch (_: Exception) { false }
        const val TECNICO_DEMO = "Técnico demo"
        val TECNICOS = listOf(TECNICO_DEMO, "Brian Bravo")

        private fun ahora(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"))
            .format(Date())
    }
}
