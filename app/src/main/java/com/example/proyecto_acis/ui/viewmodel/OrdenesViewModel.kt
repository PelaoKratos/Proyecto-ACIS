package com.example.proyecto_acis.ui.viewmodel

import android.app.Application
import android.net.Uri
import com.example.proyecto_acis.data.local.ImagenNoValida
import com.example.proyecto_acis.ui.state.EstadoPreferencias
import java.io.IOException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.data.repository.OrdenesRepository
import com.example.proyecto_acis.domain.model.Catastro
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.EventoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.domain.rules.OrdenTecnico
import com.example.proyecto_acis.domain.service.MaterialInforme
import com.example.proyecto_acis.domain.service.MaterialesInforme
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.domain.service.ReporteSla
import com.example.proyecto_acis.domain.service.ResumenOrdenes
import com.example.proyecto_acis.domain.service.TiempoVisita
import com.example.proyecto_acis.domain.validation.DatosCrearOt
import com.example.proyecto_acis.domain.validation.ErroresAsignacion
import com.example.proyecto_acis.domain.validation.ErroresInforme
import com.example.proyecto_acis.domain.validation.ValidadorOt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import com.example.proyecto_acis.ui.state.EstadoCrearOrden
import com.example.proyecto_acis.ui.state.EstadoDetalleOrden
import com.example.proyecto_acis.ui.state.FormularioDetalle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FiltroOt { TODAS, SIN_ASIGNAR, EN_VISITA, OBSERVADAS }

class OrdenesViewModel(application: Application) : AndroidViewModel(application) {
    private val acciones = OperacionesPantalla(viewModelScope)
    val operacion = acciones.estado
    fun ejecutar(accion: suspend () -> Unit) = acciones.ejecutar(accion)
    fun limpiarError() = acciones.limpiarError()

    private val dependencias = application as com.example.proyecto_acis.AcisApplication
    private val repository = dependencias.repository
    private val sesion = dependencias.sesion
    val carga = repository.carga
    fun reintentarCarga() { viewModelScope.launch { repository.inicializar() } }
    val ordenes = repository.ordenes
    val eventos = repository.eventos
    val catastro = repository.catastro
    // Envío de prueba
    val sincronizacion = repository.sync.registros
    var simulandoEnvio by mutableStateOf(false)
        private set
    var mensajeSync by mutableStateOf("")
        private set
    fun simularEnvio(ids: Set<String>, fallar: Boolean) {
        if (simulandoEnvio) return
        val permitidos = ordenesParaPerfil(ordenes.value, perfil).map { it.id }.toSet().intersect(ids)
        if (permitidos.isEmpty()) return
        simulandoEnvio = true
        mensajeSync = ""
        viewModelScope.launch {
            try {
                val resultado = repository.sync.simular(permitidos, fallar)
                mensajeSync = "Simulación: ${resultado.enviados} completadas, ${resultado.errores} con error, ${resultado.modificados} sin confirmar. No se enviaron datos a un servidor."
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                mensajeSync = "No se pudo completar la simulación. Tus órdenes siguen guardadas en el dispositivo."
            } finally { simulandoEnvio = false }
        }
    }

    // Acceso de demostración
    var mensajeDemo by mutableStateOf("")
        private set
    suspend fun cargarDemostracion() {
        if (perfil != Perfil.PLANIFICADOR) return
        mensajeDemo = if (repository.cargarDemostracion()) "Se cargaron seis órdenes de demostración."
            else "No se cargaron ejemplos. Revisa que no existan órdenes y vuelve a intentar."
    }
    val tecnicos = OrdenesRepository.TECNICOS
    val tecnicoActual get() = sesion.tecnico

    private val _preferencias = MutableStateFlow(EstadoPreferencias())
    val preferencias = _preferencias.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val datos = dependencias.preferencias.leer()
                if (datos.recordarPerfil && datos.perfil != null) {
                    sesion.perfil = datos.perfil
                    sesion.tecnico = datos.tecnico.takeIf { it in tecnicos } ?: OrdenesRepository.TECNICO_DEMO
                }
                _preferencias.value = EstadoPreferencias(datos, cargando = false)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                _preferencias.value = EstadoPreferencias(cargando = false,
                    mensaje = "No se pudieron leer las preferencias. Puedes elegir tu perfil y continuar.")
            }
        }
    }

    suspend fun recordarPerfil(activar: Boolean) {
        val datos = dependencias.preferencias.recordar(activar, perfil, tecnicoActual)
        _preferencias.value = EstadoPreferencias(datos, cargando = false)
    }

    suspend fun seleccionarTecnico(nombre: String) {
        if (nombre in tecnicos) {
            seleccionarPerfil(Perfil.TECNICO, nombre)
        }
    }

    fun cargaTecnico(nombre: String, fecha: String): Int = ordenes.value.count {
        it.tecnico == nombre && it.fechaProgramada.substringBefore(" ") == fecha.substringBefore(" ") && it.estado != EstadoOt.CERRADA
    }

    val perfil get() = sesion.perfil
    private val _creacion = MutableStateFlow(EstadoCrearOrden())
    val creacion = _creacion.asStateFlow()
    private var datosCrear: DatosCrearOt
        get() = creacion.value.datos
        set(valor) { _creacion.update { it.copy(datos = valor, errorGuardado = false) } }
    private var errorGuardarCrear: Boolean
        get() = creacion.value.errorGuardado
        set(valor) { _creacion.update { it.copy(errorGuardado = valor) } }
    private var mostrarErroresCrear: Boolean
        get() = creacion.value.mostrarErrores
        set(valor) { _creacion.update { it.copy(mostrarErrores = valor) } }

    private val _detalle = MutableStateFlow(EstadoDetalleOrden())
    val detalle = _detalle.asStateFlow()
    private var ordenInformeConErrores: String?
        get() = detalle.value.informeConErrores
        set(valor) { _detalle.update { it.copy(informeConErrores = valor) } }
    private var ordenObservacionConError: String?
        get() = detalle.value.observacionConError
        set(valor) { _detalle.update { it.copy(observacionConError = valor) } }
    private var ordenRespuestaConError: String?
        get() = detalle.value.respuestaConError
        set(valor) { _detalle.update { it.copy(respuestaConError = valor) } }
    private var mensajeDetalle: Pair<String, String>?
        get() = detalle.value.mensaje
        set(valor) { _detalle.update { it.copy(mensaje = valor) } }
    private val estadosBorrador get() = detalle.value.borradores
    private val formulariosDetalle get() = detalle.value.formularios
    fun limpiarErroresAsignacion(id: String) { _detalle.update { it.copy(erroresAsignacion = it.erroresAsignacion - id) } }
    // Datos del informe y borrador
    val diagnosticosFrecuentes = listOf("Falla eléctrica", "Gas bajo", "Filtro sucio", "Termostato")
    fun estadoBorrador(id: String): String = estadosBorrador[id].orEmpty()

    fun formularioDetalle(orden: OrdenTrabajo): FormularioDetalle =
        formulariosDetalle[orden.id] ?: FormularioDetalle(
            diagnostico = orden.diagnostico,
            diagnosticoLibre = orden.diagnostico.isNotBlank() && orden.diagnostico !in diagnosticosFrecuentes,
            actividad = orden.actividad,
            materiales = orden.materiales,
            materialesAnteriores = MaterialesInforme.leer(orden.materiales).first,
            listaMateriales = MaterialesInforme.leer(orden.materiales).second,
        )

    fun prepararDetalle(orden: OrdenTrabajo) {
        val formulario = formularioDetalle(orden)
        _detalle.update {
            if (orden.id in it.formularios) it
            else it.copy(formularios = it.formularios + (orden.id to formulario))
        }
    }

    private fun informarBorrador(id: String, mensaje: String) {
        _detalle.update { it.copy(borradores = it.borradores + (id to mensaje)) }
    }

    fun elegirDiagnostico(id: String, opcion: String) = cambiarDetalle(id) {
        if (opcion == "Otro") it.copy(diagnostico = "", diagnosticoLibre = true)
        else if (opcion in diagnosticosFrecuentes) it.copy(diagnostico = opcion, diagnosticoLibre = false)
        else it
    }

    fun cambiarDiagnostico(id: String, valor: String) = cambiarDetalle(id) { it.copy(diagnostico = valor) }
    fun cambiarActividad(id: String, valor: String) = cambiarDetalle(id) { it.copy(actividad = valor) }
    fun cambiarMateriales(id: String, valor: String) = cambiarDetalle(id) { it.copy(materiales = valor) }
    fun agregarMaterial(id: String) = cambiarListaMateriales(id) { it + MaterialInforme() }
    fun quitarMaterial(id: String, indice: Int) = cambiarListaMateriales(id) { lista -> lista.filterIndexed { i, _ -> i != indice } }
    fun cambiarMaterial(id: String, indice: Int, nombre: String? = null, cantidad: String? = null) =
        cambiarListaMateriales(id) { lista -> lista.mapIndexed { i, material ->
            if (i == indice) material.copy(nombre = nombre ?: material.nombre, cantidad = cantidad ?: material.cantidad) else material
        } }

    private fun cambiarListaMateriales(id: String, cambio: (List<MaterialInforme>) -> List<MaterialInforme>) {
        cambiarDetalle(id) { datos ->
            val lista = cambio(datos.listaMateriales)
            datos.copy(listaMateriales = lista, materiales = MaterialesInforme.escribir(datos.materialesAnteriores, lista))
        }
    }

    fun errorMaterial(id: String, indice: Int, cantidad: Boolean): String? {
        if (ordenInformeConErrores != id) return null
        val material = formulariosDetalle[id]?.listaMateriales?.getOrNull(indice) ?: return null
        return if (cantidad) MaterialesInforme.errorCantidad(material.cantidad) else MaterialesInforme.errorNombre(material.nombre)
    }

    fun alternarActividad(id: String, actividad: String) {
        val actual = formulariosDetalle[id] ?: return
        val lineas = actual.actividad.lines().filter { it.isNotBlank() }
        cambiarActividad(id, (if (actividad in lineas) lineas - actividad else lineas + actividad).joinToString("\n"))
    }

    suspend fun autoguardar(id: String, datos: FormularioDetalle): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val orden = ordenes.value.firstOrNull { it.id == id } ?: return false
        if (orden.estado != EstadoOt.EN_VISITA) return true
        val sinCambios = orden.diagnostico == datos.diagnostico && orden.actividad == datos.actividad && orden.materiales == datos.materiales
        val guardado = sinCambios || repository.guardarBorrador(id, datos.diagnostico, datos.actividad, datos.materiales)
        informarBorrador(id, if (guardado) "Borrador guardado en el dispositivo" else "No se pudo guardar. Reintenta antes de salir.")
        return guardado
    }

    fun cambiarObservacion(id: String, valor: String) = cambiarDetalle(id) { it.copy(observacion = valor) }
    fun cambiarRespuesta(id: String, valor: String) = cambiarDetalle(id) { it.copy(respuesta = valor) }

    private fun cambiarDetalle(id: String, cambio: (FormularioDetalle) -> FormularioDetalle) {
        formulariosDetalle[id]?.let { anterior ->
            val nuevo = cambio(anterior)
            _detalle.update { it.copy(formularios = it.formularios + (id to nuevo)) }
            if (nuevo.diagnostico != anterior.diagnostico || nuevo.actividad != anterior.actividad || nuevo.materiales != anterior.materiales)
                informarBorrador(id, "Cambios sin guardar")
        }
    }

    // Validaciones del formulario
    fun erroresInforme(id: String, diagnostico: String, actividad: String): ErroresInforme =
        if (ordenInformeConErrores == id) ValidadorOt.informe(diagnostico, actividad) else ErroresInforme()

    fun errorObservacion(id: String, texto: String): String? =
        if (ordenObservacionConError == id) ValidadorOt.observacion(texto) else null

    fun errorRespuesta(id: String, texto: String): String? =
        if (ordenRespuestaConError == id) ValidadorOt.respuesta(texto) else null

    fun mensaje(ordenId: String): String = mensajeDetalle?.takeIf { it.first == ordenId }?.second.orEmpty()

    suspend fun seleccionarPerfil(nuevo: Perfil, tecnicoElegido: String = tecnicoActual) {
        val pendientes = if (perfil == Perfil.TECNICO) formulariosDetalle.filter { (id, datos) -> !autoguardar(id, datos) } else emptyMap()
        if (_preferencias.value.datos.recordarPerfil) {
            val datos = dependencias.preferencias.actualizarAcceso(nuevo, tecnicoElegido)
            _preferencias.value = EstadoPreferencias(datos, cargando = false)
        }
        sesion.perfil = nuevo
        sesion.tecnico = tecnicoElegido.takeIf { it in tecnicos } ?: OrdenesRepository.TECNICO_DEMO
        _detalle.value = EstadoDetalleOrden(formularios = pendientes,
            borradores = estadosBorrador.filterKeys { it in pendientes })
    }
    fun proximasExteriores(lista: List<OrdenTrabajo>, datos: Catastro): List<OrdenTrabajo> {
        val ahora = System.currentTimeMillis()
        return lista.filter { ReglasClima.orden(it, datos) != null &&
            it.estado in setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA, EstadoOt.EN_VISITA) }
            .sortedWith(compareBy<OrdenTrabajo> { (TiempoVisita.fecha(it.fechaProgramada) ?: 0) < ahora }
                .thenBy {
                    val fecha = TiempoVisita.fecha(it.fechaProgramada) ?: 0
                    if (fecha >= ahora) fecha else -fecha
                }).take(3)
    }

    fun localesDisponibles(datos: Catastro): List<String> = datos.locales.map { it.nombre }
    fun equiposDisponibles(datos: Catastro): List<String> {
        val local = datos.locales.firstOrNull { it.nombre.equals(datosCrear.local, ignoreCase = true) } ?: return emptyList()
        return datos.equipos.filter { it.localId == local.id }.map { it.nombre }
    }

    fun ultimaMantencion(datos: Catastro): String? {
        val local = datos.locales.firstOrNull { it.nombre.equals(datosCrear.local, ignoreCase = true) } ?: return null
        val equipo = datos.equipos.firstOrNull { it.localId == local.id && it.nombre.equals(datosCrear.equipo, ignoreCase = true) }
        return equipo?.ultimaMantencionMillis?.let {
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.forLanguageTag("es-CL")).format(java.util.Date(it))
        }
    }

    fun ultimasMantenciones(lista: List<OrdenTrabajo>): List<OrdenTrabajo> = ResumenOrdenes.ultimasMantenciones(lista)

    fun cambiarLocal(valor: String) {
        if (valor != datosCrear.local) datosCrear = datosCrear.copy(local = valor, equipo = "")
    }
    fun cambiarEquipo(valor: String) { datosCrear = datosCrear.copy(equipo = valor) }
    fun cambiarDescripcion(valor: String) { datosCrear = datosCrear.copy(descripcion = valor) }
    fun cambiarTipo(valor: TipoOt) { datosCrear = datosCrear.copy(tipo = valor) }
    fun cambiarPrioridad(valor: PrioridadOt) { datosCrear = datosCrear.copy(prioridad = valor) }
    fun cambiarFecha(valor: String) { datosCrear = datosCrear.copy(fecha = valor) }
    fun cambiarHora(valor: String) { datosCrear = datosCrear.copy(hora = valor) }

    fun validarPasoCrear(): Boolean {
        val valido = creacion.value.validarPaso()
        _creacion.update { it.copy(mostrarErrores = !valido) }
        return valido
    }

    fun avanzarPasoCrear() {
        if (validarPasoCrear()) _creacion.update { it.copy(paso = (it.paso + 1).coerceAtMost(3)) }
    }

    fun retrocederPasoCrear() {
        _creacion.update { it.copy(paso = (it.paso - 1).coerceAtLeast(1), mostrarErrores = false) }
    }

    // Creacion y edicion de ordenes
    fun prepararCreacion(local: String = "", equipo: String = "") {
        _creacion.value = EstadoCrearOrden(datos = DatosCrearOt(local = local, equipo = equipo))
    }

    fun prepararEdicion(id: String): Boolean {
        if (!puedeActuar(id, Perfil.PLANIFICADOR)) return false
        val orden = ordenes.value.firstOrNull { it.id == id && it.estado in setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA) } ?: return false
        val partes = orden.fechaProgramada.split(" ")
        _creacion.value = EstadoCrearOrden(datos = DatosCrearOt(
            orden.local, orden.equipo, orden.descripcion, orden.tipo, orden.prioridad,
            partes.getOrElse(0) { "" }, partes.getOrElse(1) { "" }))
        return true
    }

    suspend fun actualizarDatos(id: String, datos: DatosCrearOt): Boolean =
        puedeActuar(id, Perfil.PLANIFICADOR) && repository.actualizarDatos(id, datos)

    suspend fun guardarEdicion(id: String): Boolean {
        mostrarErroresCrear = true
        errorGuardarCrear = false
        if (ValidadorOt.crear(datosCrear).hayErrores) return false
        val guardado = actualizarDatos(id, datosCrear)
        errorGuardarCrear = !guardado
        if (guardado) prepararCreacion()
        return guardado
    }

    suspend fun eliminar(id: String): Boolean = puedeActuar(id, Perfil.PLANIFICADOR) && repository.eliminar(id)

    suspend fun rechazar(id: String): Boolean = puedeActuar(id, Perfil.TECNICO) && repository.rechazar(id)

    suspend fun postergar(id: String, fecha: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        ValidadorOt.postergacion(fecha)?.let { error ->
            mensajeDetalle = id to error
            return false
        }
        val guardado = repository.postergar(id, fecha)
        mensajeDetalle = id to if (guardado) "Fecha de visita actualizada." else "No se pudo cambiar la fecha de visita."
        return guardado
    }

    suspend fun guardarUbicacion(id: String, ubicacion: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val guardado = repository.guardarUbicacion(id, ubicacion)
        mensajeDetalle = id to if (guardado) "Ubicación guardada." else "No se pudo guardar la ubicación."
        return guardado
    }

    suspend fun crear(): String? {
        if (perfil != Perfil.PLANIFICADOR) return null
        mostrarErroresCrear = true
        errorGuardarCrear = false
        val datos = datosCrear
        if (ValidadorOt.crear(datos).hayErrores) return null
        val tipo = datos.tipo ?: return null
        val prioridad = datos.prioridad ?: return null
        val id = repository.crear(
            datos.local, datos.equipo, datos.descripcion,
            "${datos.fecha} ${datos.hora}", tipo, prioridad,
        )
        if (id != null) {
            prepararCreacion()
        } else {
            errorGuardarCrear = true
        }
        return id
    }

    fun autoguardarAlSalir(id: String, datos: FormularioDetalle) {
        dependencias.tareas.launch { autoguardar(id, datos) }
    }

    // Listados y reportes
    fun ordenesParaPerfil(lista: List<OrdenTrabajo>, perfilVisible: Perfil): List<OrdenTrabajo> {
        val filtradas = lista.filter { orden ->
            when (perfilVisible) {
                Perfil.PLANIFICADOR -> true
                Perfil.TECNICO -> orden.tecnico == tecnicoActual
                Perfil.CLIENTE -> orden.estado in setOf(
                    EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA,
                )
            }
        }
        if (perfilVisible == Perfil.TECNICO) return com.example.proyecto_acis.domain.rules.OrdenTecnico.recientes(filtradas)
        return filtradas.sortedWith(
            compareByDescending<OrdenTrabajo> { it.ultimaActualizacion }
                .thenByDescending { it.id.removePrefix("OT-").toIntOrNull() ?: 0 },
        )
    }

    var mesReporte by mutableStateOf(0)
        private set
    fun cambiarMesReporte(cambio: Int) { mesReporte = (mesReporte + cambio).coerceIn(-120, 12) }
    fun reporteSla(lista: List<OrdenTrabajo>, historial: List<EventoOt>, ahora: Long) =
        com.example.proyecto_acis.domain.service.ReporteSla.mensual(lista, historial, ahora, mesReporte)

    fun resumenOperativo(lista: List<OrdenTrabajo>) = ResumenOrdenes.operativo(lista)
    fun catalogoEquipos(lista: List<OrdenTrabajo>, datos: Catastro) = ResumenOrdenes.equipos(lista, datos)

    fun filtrarOrdenes(lista: List<OrdenTrabajo>, filtro: FiltroOt): List<OrdenTrabajo> =
        lista.filter { orden ->
            when (filtro) {
                FiltroOt.TODAS -> true
                FiltroOt.SIN_ASIGNAR -> orden.estado == EstadoOt.CREADA
                FiltroOt.EN_VISITA -> orden.estado == EstadoOt.EN_VISITA
                FiltroOt.OBSERVADAS -> orden.estado == EstadoOt.OBSERVADA
            }
        }

    fun programadasVencidas(lista: List<OrdenTrabajo>, ahora: Long = System.currentTimeMillis()): Int =
        lista.count { it.estado != EstadoOt.CERRADA && TiempoVisita.minutos(it.fechaProgramada, ahora = ahora) != null }

    fun jornada(lista: List<OrdenTrabajo>): List<OrdenTrabajo> {
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.forLanguageTag("es-CL"))
        val hoy = formato.format(java.util.Date())
        return com.example.proyecto_acis.domain.rules.OrdenTecnico.recientes(
            lista.filter { it.fechaProgramada.substringBefore(" ") == hoy })
    }

    fun siguienteVisita(lista: List<OrdenTrabajo>): OrdenTrabajo? {
        return com.example.proyecto_acis.domain.rules.OrdenTecnico.siguiente(jornada(lista))
    }

    fun historialEquipo(orden: OrdenTrabajo): List<OrdenTrabajo> = ResumenOrdenes.historialEquipo(orden, ordenes.value)

    fun eventosDeOrden(id: String, lista: List<EventoOt>): List<EventoOt> =
        if (ordenesParaPerfil(ordenes.value, perfil).any { it.id == id }) lista.filter { it.otId == id }
        else emptyList()

    fun hitosVisita(orden: OrdenTrabajo): List<Pair<String, String>> = buildList {
        if (orden.checkIn.isNotBlank()) add((if (orden.ubicacionCheckIn.isNotBlank()) "Llegada verificada" else "Llegada registrada sin ubicación") to orden.checkIn)
        if (orden.checkOut.isNotBlank()) add("Salida e informe registrados" to orden.checkOut)
        if (orden.fechaConformidad.isNotBlank()) add("Trabajo aprobado" to orden.fechaConformidad)
    }

    fun primeraRevision(lista: List<OrdenTrabajo>): OrdenTrabajo? =
        lista.firstOrNull { it.estado == EstadoOt.PENDIENTE_CONFORMIDAD }

    private fun puedeActuar(id: String, requerido: Perfil): Boolean =
        perfil == requerido && ordenesParaPerfil(ordenes.value, perfil).any { it.id == id }

    suspend fun asignar(id: String, tecnico: String = tecnicoActual, hora: String? = null): Boolean {
        if (!puedeActuar(id, Perfil.PLANIFICADOR)) return false
        val actual = ordenes.value.firstOrNull { it.id == id } ?: return false
        val elegida = hora ?: actual.fechaProgramada.substringAfter(" ", "")
        val errores = ValidadorOt.asignacion(tecnico, tecnicos, elegida)
        _detalle.update { it.copy(erroresAsignacion = it.erroresAsignacion + (id to errores)) }
        if (errores.hayErrores) {
            mensajeDetalle = null
            return false
        }
        val guardado = repository.asignar(id, tecnico, elegida)
        mensajeDetalle = id to if (guardado) "Orden asignada a $tecnico en este dispositivo." else "No se pudo asignar la orden. Intenta nuevamente."
        return guardado
    }
    // Acciones de la visita
    suspend fun aceptar(id: String): Boolean = puedeActuar(id, Perfil.TECNICO) && repository.aceptar(id)
    suspend fun iniciarVisita(id: String, ubicacion: String?) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        val guardado = repository.iniciarVisita(id, ubicacion)
        mensajeDetalle = id to when {
            !guardado -> "No se pudo iniciar la visita."
            ubicacion == null -> "Check-in registrado sin ubicación."
            else -> "Check-in registrado con ubicación."
        }
    }
    suspend fun aprobar(id: String): Boolean = puedeActuar(id, Perfil.CLIENTE) && repository.aprobar(id)

    suspend fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        val guardado = repository.guardarBorrador(id, diagnostico, actividad, materiales)
        informarBorrador(id, if (guardado) "Borrador guardado en el dispositivo" else "No se pudo guardar. Reintenta antes de salir.")
        mensajeDetalle = id to if (guardado) "Borrador guardado en este dispositivo."
        else "No se pudo guardar el borrador."
    }

    suspend fun importarFoto(id: String, uri: Uri, tipo: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val error = try {
            if (repository.importarFoto(id, uri, tipo)) null else "La orden ya no permite adjuntar esta foto."
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (invalida: ImagenNoValida) {
            invalida.message ?: "Elige una imagen válida."
        } catch (_: SecurityException) {
            "No se pudo acceder a la foto. Vuelve a seleccionarla."
        } catch (_: IOException) {
            "No se pudo leer la foto. Prueba con otra imagen."
        }
        mensajeDetalle = id to (error ?: "Foto de galería guardada en esta OT.")
        return error == null
    }

    suspend fun eliminarFoto(id: String, nombre: String): Boolean =
        puedeActuar(id, Perfil.TECNICO) && repository.eliminarFoto(id, nombre)

    suspend fun guardarFoto(id: String, nombre: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val guardado = repository.guardarFoto(id, nombre)
        mensajeDetalle = id to if (guardado) "Foto guardada en esta OT."
        else "No se pudo guardar la foto."
        return guardado
    }

    suspend fun finalizarVisita(id: String, diagnostico: String, actividad: String, materiales: String) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        ordenInformeConErrores = id
        val materialesInvalidos = formulariosDetalle[id]?.listaMateriales.orEmpty().any {
            MaterialesInforme.errorNombre(it.nombre) != null || MaterialesInforme.errorCantidad(it.cantidad) != null
        }
        if (ValidadorOt.informe(diagnostico, actividad).hayErrores || materialesInvalidos) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.enviarInforme(id, diagnostico, actividad, materiales)
        mensajeDetalle = id to if (guardado) "Informe disponible para revisión en este dispositivo."
        else "No se pudo finalizar la visita."
        if (guardado) ordenInformeConErrores = null
    }

    suspend fun observar(id: String, observacion: String) {
        if (!puedeActuar(id, Perfil.CLIENTE)) return
        ordenObservacionConError = id
        if (ValidadorOt.observacion(observacion) != null) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.observar(id, observacion)
        mensajeDetalle = id to if (guardado) "Observación registrada." else "No se pudo registrar la observación."
        if (guardado) ordenObservacionConError = null
    }

    suspend fun responderObservacion(id: String, respuesta: String) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        ordenRespuestaConError = id
        if (ValidadorOt.respuesta(respuesta) != null) {
            mensajeDetalle = null
            return
        }
        val guardado = repository.responderObservacion(id, respuesta)
        mensajeDetalle = id to if (guardado) "Respuesta registrada; el cliente puede revisar nuevamente."
        else "No se pudo enviar la respuesta."
        if (guardado) ordenRespuestaConError = null
    }
}
