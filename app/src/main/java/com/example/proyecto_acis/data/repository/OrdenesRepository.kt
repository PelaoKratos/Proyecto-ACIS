package com.example.proyecto_acis.data.repository

import android.content.Context
import android.net.Uri
import com.example.proyecto_acis.data.local.AlmacenEvidencias
import com.example.proyecto_acis.data.demo.DatosDemostracion
import com.example.proyecto_acis.data.local.OrdenesDb
import com.example.proyecto_acis.data.sync.SyncRepositorySimulado
import com.example.proyecto_acis.domain.model.Cliente
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.EventoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoEvento
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.domain.rules.ReglasEstadoOt
import com.example.proyecto_acis.domain.validation.DatosCliente
import com.example.proyecto_acis.domain.validation.DatosCrearOt
import com.example.proyecto_acis.domain.validation.DatosEquipo
import com.example.proyecto_acis.domain.validation.DatosLocal
import com.example.proyecto_acis.domain.validation.ValidadorCatastro
import com.example.proyecto_acis.domain.validation.ValidadorEquipo
import com.example.proyecto_acis.domain.validation.ValidadorOt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class CargaDatos { CARGANDO, LISTO, ERROR }

class OrdenesRepository(
    context: Context,
    private val database: OrdenesDb = OrdenesDb(context),
    nombrePreferencias: String = "acis_ordenes_locales",
) {
    private val preferences = context.getSharedPreferences(nombrePreferencias, Context.MODE_PRIVATE)
    private val fotos = File(context.filesDir, "evidencias")
    private val almacenFotos = AlmacenEvidencias(context.applicationContext)
    private val _ordenes = MutableStateFlow<List<OrdenTrabajo>>(emptyList())
    val ordenes = _ordenes.asStateFlow()
    private val _eventos = MutableStateFlow<List<EventoOt>>(emptyList())
    val eventos = _eventos.asStateFlow()
    private val _catastro = MutableStateFlow(com.example.proyecto_acis.domain.model.Catastro())
    val catastro = _catastro.asStateFlow()
    val sync = SyncRepositorySimulado(database)

    private val mutex = Mutex()
    private val _carga = MutableStateFlow(CargaDatos.CARGANDO)
    val carga = _carga.asStateFlow()

    suspend fun inicializar() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_carga.value == CargaDatos.LISTO) return@withLock
            _carga.value = CargaDatos.CARGANDO
            try {
                cargarOrdenes()
                recargarDatos()
                _carga.value = CargaDatos.LISTO
            } catch (cancelacion: kotlinx.coroutines.CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                _carga.value = CargaDatos.ERROR
            }
        }
    }

    private suspend fun <T> ejecutar(operacion: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { operacion() }
    }

    suspend fun guardarCliente(datos: DatosCliente): Boolean = ejecutar {
        if (ValidadorCatastro.cliente(datos, _catastro.value) != null || !database.guardarCliente(datos)) return@ejecutar false
        recargarDatos()
        return@ejecutar true
    }

    suspend fun guardarLocal(datos: DatosLocal): Boolean = ejecutar {
        if (ValidadorCatastro.local(datos, _catastro.value).hayErrores || !database.guardarLocal(datos)) return@ejecutar false
        recargarDatos()
        return@ejecutar true
    }

    suspend fun guardarEquipo(datos: DatosEquipo): Boolean = ejecutar {
        val vinculados = _ordenes.value.mapNotNull { it.equipoId }.toSet()
        if (ValidadorEquipo.validar(datos, _catastro.value, vinculados).hayErrores) return@ejecutar false
        if (!database.guardarEquipo(datos)) return@ejecutar false
        recargarDatos()
        return@ejecutar true
    }

    suspend fun cargarDemostracion(): Boolean = ejecutar {
        if (_ordenes.value.isNotEmpty()) return@ejecutar false
        val primero = preferences.getInt("ultimoNumeroOt", 1000) + 1
        val ejemplos = DatosDemostracion.crear(primero, System.currentTimeMillis())
        if (!database.guardarTodas(ejemplos)) return@ejecutar false
        preferences.edit().putInt("ultimoNumeroOt", primero + ejemplos.size - 1)
            .putBoolean("migracionSQLiteCompleta", true).apply()
        recargarDatos()
        return@ejecutar true
    }

    suspend fun crear(
        local: String,
        equipo: String,
        descripcion: String,
        fecha: String,
        tipo: TipoOt,
        prioridad: PrioridadOt,
    ): String? = ejecutar {
        if (listOf(local, equipo, descripcion, fecha).any { it.isBlank() }) return@ejecutar null
        if (tipo == TipoOt.SIN_ESPECIFICAR || prioridad == PrioridadOt.SIN_ESPECIFICAR) return@ejecutar null
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
        if (!fechaValida(fecha)) return@ejecutar null
        if (!database.insertar(nueva, evento(nueva, TipoEvento.CREACION))) return@ejecutar null
        preferences.edit().putInt("ultimoNumeroOt", siguiente).apply()
        recargarDatos()
        return@ejecutar id
    }

    suspend fun asignar(id: String, tecnico: String = TECNICO_DEMO, hora: String? = null): Boolean = ejecutar {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return@ejecutar false
        val horaElegida = hora ?: actual.fechaProgramada.substringAfter(" ", "")
        if (ValidadorOt.asignacion(tecnico, TECNICOS, horaElegida).hayErrores) return@ejecutar false
        val fecha = "${actual.fechaProgramada.substringBefore(" ")} $horaElegida"
        if (!fechaValida(fecha)) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.CREADA, TipoEvento.ASIGNACION) { it.copy(estado = EstadoOt.ASIGNADA, tecnico = tecnico, fechaProgramada = fecha) }
    }

    suspend fun aceptar(id: String): Boolean = ejecutar {
        cambiar(id, EstadoOt.ASIGNADA, TipoEvento.ACEPTACION) {
            it.copy(estado = EstadoOt.ACEPTADA)
        }
    }

    suspend fun rechazar(id: String): Boolean = ejecutar {
        cambiar(id, EstadoOt.ASIGNADA, TipoEvento.RECHAZO) {
            it.copy(estado = EstadoOt.CREADA, tecnico = "")
        }
    }

    suspend fun postergar(id: String, fecha: String): Boolean = ejecutar {
        if (ValidadorOt.postergacion(fecha) != null) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.ASIGNADA, TipoEvento.POSTERGACION) { it.copy(fechaProgramada = fecha.trim()) }
    }

    suspend fun actualizarDatos(id: String, datos: DatosCrearOt): Boolean = ejecutar {
        if (ValidadorOt.crear(datos).hayErrores || !fechaValida("${datos.fecha} ${datos.hora}")) return@ejecutar false
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in estadosEditables } ?: return@ejecutar false
        return@ejecutar cambiar(id, actual.estado, TipoEvento.EDICION) {
            it.copy(local = datos.local.trim(), equipo = datos.equipo.trim(), descripcion = datos.descripcion.trim(),
                tipo = datos.tipo!!, prioridad = datos.prioridad!!, fechaProgramada = "${datos.fecha} ${datos.hora}",
                equipoId = if (it.local == datos.local.trim() && it.equipo == datos.equipo.trim()) it.equipoId else null)
        }
    }

    suspend fun eliminar(id: String): Boolean = ejecutar {
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in estadosEditables } ?: return@ejecutar false
        if (!database.eliminar(id, actual.estado)) return@ejecutar false
        _ordenes.value = _ordenes.value.filterNot { it.id == id }
        _eventos.value = database.leerEventos()
        sync.recargar()
        actual.fotosEvidencia.forEach { File(fotos, it).delete() }
        return@ejecutar true
    }

    suspend fun iniciarVisita(id: String, ubicacion: String?): Boolean = ejecutar {
        cambiar(id, EstadoOt.ACEPTADA, TipoEvento.LLEGADA) {
            it.copy(estado = EstadoOt.EN_VISITA, checkIn = ahora(), ubicacionCheckIn = ubicacion.orEmpty())
        }
    }

    suspend fun guardarUbicacion(id: String, ubicacion: String): Boolean = ejecutar {
        if (ubicacion.isBlank()) return@ejecutar false
        val actual = _ordenes.value.firstOrNull { it.id == id && it.estado in setOf(EstadoOt.ACEPTADA, EstadoOt.EN_VISITA) } ?: return@ejecutar false
        return@ejecutar cambiar(id, actual.estado) { it.copy(ubicacionCheckIn = ubicacion) }
    }

    suspend fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String): Boolean = ejecutar {
        cambiar(id, EstadoOt.EN_VISITA) {
                it.copy(diagnostico = diagnostico.trim(), actividad = actividad.trim(), materiales = materiales.trim())
            }
    }

    suspend fun guardarFoto(id: String, nombre: String): Boolean = ejecutar {
        val archivo = File(fotos, nombre)
        if (nombre != File(nombre).name || !archivo.isFile || archivo.length() == 0L) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(fotoEvidencia = (it.fotosEvidencia + nombre).distinct().joinToString("\n"))
        }
    }

    suspend fun importarFoto(id: String, uri: Uri, tipo: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_ordenes.value.none { it.id == id && it.estado == EstadoOt.EN_VISITA }) return@withLock false
            val contexto = currentCoroutineContext()
            val archivo = almacenFotos.importar(uri, id, tipo) { contexto.ensureActive() }
            var guardada = false
            try {
                contexto.ensureActive()
                // Se termina la asociación antes de atender una cancelación.
                withContext(NonCancellable) {
                    guardada = cambiar(id, EstadoOt.EN_VISITA) {
                        it.copy(fotoEvidencia = (it.fotosEvidencia + archivo.name).distinct().joinToString("\n"))
                    }
                }
                guardada
            } finally {
                if (!guardada) archivo.delete()
            }
        }
    }

    suspend fun eliminarFoto(id: String, nombre: String): Boolean = ejecutar {
        val actual = _ordenes.value.firstOrNull { it.id == id } ?: return@ejecutar false
        if (nombre !in actual.fotosEvidencia) return@ejecutar false
        val guardado = cambiar(id, EstadoOt.EN_VISITA) {
            it.copy(fotoEvidencia = it.fotosEvidencia.filterNot { foto -> foto == nombre }.joinToString("\n"))
        }
        if (guardado) File(fotos, nombre).delete()
        return@ejecutar guardado
    }

    suspend fun enviarInforme(id: String, diagnostico: String, actividad: String, materiales: String): Boolean = ejecutar {
        if (diagnostico.isBlank() || actividad.isBlank()) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.EN_VISITA, TipoEvento.INFORME) {
            it.copy(
                estado = EstadoOt.PENDIENTE_CONFORMIDAD,
                diagnostico = diagnostico.trim(),
                actividad = actividad.trim(),
                materiales = materiales.trim(),
                checkOut = ahora(),
            )
        }
    }

    suspend fun observar(id: String, observacion: String): Boolean = ejecutar {
        if (observacion.isBlank()) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD, TipoEvento.OBSERVACION) {
            it.copy(estado = EstadoOt.OBSERVADA, observacionCliente = observacion.trim())
        }
    }

    suspend fun responderObservacion(id: String, respuesta: String): Boolean = ejecutar {
        if (respuesta.isBlank()) return@ejecutar false
        return@ejecutar cambiar(id, EstadoOt.OBSERVADA, TipoEvento.RESPUESTA) {
            it.copy(estado = EstadoOt.PENDIENTE_CONFORMIDAD, respuestaTecnico = respuesta.trim())
        }
    }

    suspend fun aprobar(id: String): Boolean = ejecutar {
        cambiar(id, EstadoOt.PENDIENTE_CONFORMIDAD, TipoEvento.CONFORMIDAD) {
            it.copy(estado = EstadoOt.CERRADA, fechaConformidad = ahora())
        }
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
        sync.recargar()
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
        check(anteriores.isEmpty() || database.guardarTodas(anteriores))
        preferences.edit().putBoolean("migracionSQLiteCompleta", true).apply()
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
