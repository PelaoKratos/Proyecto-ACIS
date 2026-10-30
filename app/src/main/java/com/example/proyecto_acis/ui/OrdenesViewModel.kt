package com.example.proyecto_acis.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.proyecto_acis.data.EventoOt
import com.example.proyecto_acis.data.Catastro
import com.example.proyecto_acis.data.EstadoOt
import com.example.proyecto_acis.data.OrdenTrabajo
import com.example.proyecto_acis.data.OrdenesRepository
import com.example.proyecto_acis.data.Perfil
import com.example.proyecto_acis.data.PrioridadOt
import com.example.proyecto_acis.data.TipoOt
import com.example.proyecto_acis.domain.ResumenOrdenes
import com.example.proyecto_acis.domain.DatosCrearOt
import com.example.proyecto_acis.domain.ErroresCrearOt
import com.example.proyecto_acis.domain.ErroresAsignacion
import com.example.proyecto_acis.domain.ErroresInforme
import com.example.proyecto_acis.domain.TiempoVisita
import com.example.proyecto_acis.domain.ValidadorOt
import com.example.proyecto_acis.domain.MaterialInforme
import com.example.proyecto_acis.domain.MaterialesInforme

enum class FiltroOt { TODAS, SIN_ASIGNAR, EN_VISITA, OBSERVADAS }

class OrdenesViewModel(application: Application) : AndroidViewModel(application) {
    private val dependencias = application as com.example.proyecto_acis.AcisApplication
    private val repository = dependencias.repository
    private val sesion = dependencias.sesion
    val ordenes = repository.ordenes
    val eventos = repository.eventos
    val catastro = repository.catastro
    var mensajeDemo by mutableStateOf("")
        private set
    fun cargarDemostracion() {
        if (perfil != Perfil.PLANIFICADOR) return
        mensajeDemo = if (repository.cargarDemostracion()) "Se cargaron seis órdenes de demostración."
            else "No se cargaron ejemplos. Revisa que no existan órdenes y vuelve a intentar."
    }
    val tecnicos = OrdenesRepository.TECNICOS
    val tecnicoActual get() = sesion.tecnico

    fun seleccionarTecnico(nombre: String) {
        if (nombre in tecnicos) {
            seleccionarPerfil(Perfil.TECNICO)
            sesion.tecnico = nombre
        }
    }

    fun cargaTecnico(nombre: String, fecha: String): Int = ordenes.value.count {
        it.tecnico == nombre && it.fechaProgramada.substringBefore(" ") == fecha.substringBefore(" ") && it.estado != EstadoOt.CERRADA
    }

    val perfil get() = sesion.perfil
    var datosCrear by mutableStateOf(DatosCrearOt())
        private set
    var errorGuardarCrear by mutableStateOf(false)
        private set
    private var mostrarErroresCrear by mutableStateOf(false)
    private var ordenInformeConErrores by mutableStateOf<String?>(null)
    private var ordenObservacionConError by mutableStateOf<String?>(null)
    private var ordenRespuestaConError by mutableStateOf<String?>(null)
    private var mensajeDetalle by mutableStateOf<Pair<String, String>?>(null)
    private val erroresAsignacion = mutableStateMapOf<String, ErroresAsignacion>()
    fun erroresAsignacion(id: String): ErroresAsignacion = erroresAsignacion[id] ?: ErroresAsignacion()
    fun limpiarErroresAsignacion(id: String) { erroresAsignacion.remove(id) }
    val diagnosticosFrecuentes = listOf("Falla eléctrica", "Gas bajo", "Filtro sucio", "Termostato")
    private val estadosBorrador = mutableStateMapOf<String, String>()
    fun estadoBorrador(id: String): String = estadosBorrador[id].orEmpty()
    private val formulariosDetalle = mutableStateMapOf<String, FormularioDetalle>()

    data class FormularioDetalle(
        val diagnostico: String = "",
        val diagnosticoLibre: Boolean = false,
        val actividad: String = "",
        val materiales: String = "",
        val observacion: String = "",
        val respuesta: String = "",
        val materialesAnteriores: String = "",
        val listaMateriales: List<MaterialInforme> = emptyList(),
    )

    fun formularioDetalle(orden: OrdenTrabajo): FormularioDetalle =
        formulariosDetalle.getOrPut(orden.id) {
            FormularioDetalle(
                diagnostico = orden.diagnostico,
                diagnosticoLibre = orden.diagnostico.isNotBlank() && orden.diagnostico !in diagnosticosFrecuentes,
                actividad = orden.actividad,
                materiales = orden.materiales,
                materialesAnteriores = MaterialesInforme.leer(orden.materiales).first,
                listaMateriales = MaterialesInforme.leer(orden.materiales).second,
            )
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

    fun autoguardar(id: String, datos: FormularioDetalle): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val orden = ordenes.value.firstOrNull { it.id == id } ?: return false
        if (orden.estado != EstadoOt.EN_VISITA) return true
        val sinCambios = orden.diagnostico == datos.diagnostico && orden.actividad == datos.actividad && orden.materiales == datos.materiales
        val guardado = sinCambios || repository.guardarBorrador(id, datos.diagnostico, datos.actividad, datos.materiales)
        estadosBorrador[id] = if (guardado) "Borrador guardado en el dispositivo" else "No se pudo guardar. Reintenta antes de salir."
        return guardado
    }

    fun cambiarObservacion(id: String, valor: String) = cambiarDetalle(id) { it.copy(observacion = valor) }
    fun cambiarRespuesta(id: String, valor: String) = cambiarDetalle(id) { it.copy(respuesta = valor) }

    private fun cambiarDetalle(id: String, cambio: (FormularioDetalle) -> FormularioDetalle) {
        formulariosDetalle[id]?.let { anterior ->
            val nuevo = cambio(anterior)
            formulariosDetalle[id] = nuevo
            if (nuevo.diagnostico != anterior.diagnostico || nuevo.actividad != anterior.actividad || nuevo.materiales != anterior.materiales)
                estadosBorrador[id] = "Cambios sin guardar"
        }
    }

    val erroresCrear: ErroresCrearOt
        get() = if (mostrarErroresCrear) ValidadorOt.crear(datosCrear) else ErroresCrearOt()

    fun erroresInforme(id: String, diagnostico: String, actividad: String): ErroresInforme =
        if (ordenInformeConErrores == id) ValidadorOt.informe(diagnostico, actividad) else ErroresInforme()

    fun errorObservacion(id: String, texto: String): String? =
        if (ordenObservacionConError == id) ValidadorOt.observacion(texto) else null

    fun errorRespuesta(id: String, texto: String): String? =
        if (ordenRespuestaConError == id) ValidadorOt.respuesta(texto) else null

    fun mensaje(ordenId: String): String = mensajeDetalle?.takeIf { it.first == ordenId }?.second.orEmpty()

    fun seleccionarPerfil(nuevo: Perfil) {
        val pendientes = if (perfil == Perfil.TECNICO) formulariosDetalle.filter { (id, datos) -> !autoguardar(id, datos) } else emptyMap()
        sesion.perfil = nuevo
        formulariosDetalle.clear()
        formulariosDetalle.putAll(pendientes)
        mensajeDetalle = null
        ordenInformeConErrores = null
        ordenObservacionConError = null
        ordenRespuestaConError = null
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

    fun validarPasoCrear(paso: Int): Boolean {
        mostrarErroresCrear = true
        val errores = ValidadorOt.crear(datosCrear)
        val valido = when (paso) {
            1 -> errores.local == null && errores.equipo == null
            2 -> errores.descripcion == null && errores.tipo == null && errores.prioridad == null
            else -> !errores.hayErrores
        }
        if (valido) mostrarErroresCrear = false
        return valido
    }

    fun prepararCreacion(local: String = "", equipo: String = "") {
        datosCrear = DatosCrearOt(local = local, equipo = equipo)
        mostrarErroresCrear = false
        errorGuardarCrear = false
    }

    fun prepararEdicion(id: String): Boolean {
        if (!puedeActuar(id, Perfil.PLANIFICADOR)) return false
        val orden = ordenes.value.firstOrNull { it.id == id && it.estado in setOf(EstadoOt.CREADA, EstadoOt.ASIGNADA, EstadoOt.ACEPTADA) } ?: return false
        val partes = orden.fechaProgramada.split(" ")
        datosCrear = DatosCrearOt(orden.local, orden.equipo, orden.descripcion, orden.tipo, orden.prioridad,
            partes.getOrElse(0) { "" }, partes.getOrElse(1) { "" })
        mostrarErroresCrear = false
        errorGuardarCrear = false
        return true
    }

    fun actualizarDatos(id: String, datos: DatosCrearOt): Boolean =
        puedeActuar(id, Perfil.PLANIFICADOR) && repository.actualizarDatos(id, datos)

    fun guardarEdicion(id: String): Boolean {
        mostrarErroresCrear = true
        errorGuardarCrear = false
        if (ValidadorOt.crear(datosCrear).hayErrores) return false
        val guardado = actualizarDatos(id, datosCrear)
        errorGuardarCrear = !guardado
        if (guardado) prepararCreacion()
        return guardado
    }

    fun eliminar(id: String): Boolean = puedeActuar(id, Perfil.PLANIFICADOR) && repository.eliminar(id)

    fun rechazar(id: String): Boolean = puedeActuar(id, Perfil.TECNICO) && repository.rechazar(id)

    fun postergar(id: String, fecha: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        ValidadorOt.postergacion(fecha)?.let { error ->
            mensajeDetalle = id to error
            return false
        }
        val guardado = repository.postergar(id, fecha)
        mensajeDetalle = id to if (guardado) "Fecha de visita actualizada." else "No se pudo cambiar la fecha de visita."
        return guardado
    }

    fun guardarUbicacion(id: String, ubicacion: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val guardado = repository.guardarUbicacion(id, ubicacion)
        mensajeDetalle = id to if (guardado) "Ubicación guardada." else "No se pudo guardar la ubicación."
        return guardado
    }

    fun crear(): String? {
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
            datosCrear = DatosCrearOt()
            mostrarErroresCrear = false
        } else {
            errorGuardarCrear = true
        }
        return id
    }

    fun ordenesParaPerfil(lista: List<OrdenTrabajo>, perfilVisible: Perfil): List<OrdenTrabajo> =
        lista.filter { orden ->
            when (perfilVisible) {
                Perfil.PLANIFICADOR -> true
                Perfil.TECNICO -> orden.tecnico == tecnicoActual
                Perfil.CLIENTE -> orden.estado in setOf(
                    EstadoOt.PENDIENTE_CONFORMIDAD, EstadoOt.OBSERVADA, EstadoOt.CERRADA,
                )
            }
        }.sortedWith(
            compareByDescending<OrdenTrabajo> { it.ultimaActualizacion }
                .thenByDescending { it.id.removePrefix("OT-").toIntOrNull() ?: 0 },
        )

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
        return lista.filter { it.fechaProgramada.substringBefore(" ") == hoy }
            .sortedBy { it.fechaProgramada.substringAfter(" ") }
    }

    fun siguienteVisita(lista: List<OrdenTrabajo>): OrdenTrabajo? {
        val hoy = jornada(lista)
        return hoy.firstOrNull { it.estado == EstadoOt.EN_VISITA }
            ?: hoy.firstOrNull { it.estado == EstadoOt.ACEPTADA || it.estado == EstadoOt.ASIGNADA }
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

    fun asignar(id: String, tecnico: String = tecnicoActual, hora: String? = null): Boolean {
        if (!puedeActuar(id, Perfil.PLANIFICADOR)) return false
        val actual = ordenes.value.firstOrNull { it.id == id } ?: return false
        val elegida = hora ?: actual.fechaProgramada.substringAfter(" ", "")
        val errores = ValidadorOt.asignacion(tecnico, tecnicos, elegida)
        erroresAsignacion[id] = errores
        if (errores.hayErrores) {
            mensajeDetalle = null
            return false
        }
        val guardado = repository.asignar(id, tecnico, elegida)
        mensajeDetalle = id to if (guardado) "Orden asignada a $tecnico en este dispositivo." else "No se pudo asignar la orden. Intenta nuevamente."
        return guardado
    }
    fun aceptar(id: String): Boolean = puedeActuar(id, Perfil.TECNICO) && repository.aceptar(id)
    fun iniciarVisita(id: String, ubicacion: String?) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        val guardado = repository.iniciarVisita(id, ubicacion)
        mensajeDetalle = id to when {
            !guardado -> "No se pudo iniciar la visita."
            ubicacion == null -> "Check-in registrado sin ubicación."
            else -> "Check-in registrado con ubicación."
        }
    }
    fun aprobar(id: String): Boolean = puedeActuar(id, Perfil.CLIENTE) && repository.aprobar(id)

    fun guardarBorrador(id: String, diagnostico: String, actividad: String, materiales: String) {
        if (!puedeActuar(id, Perfil.TECNICO)) return
        val guardado = repository.guardarBorrador(id, diagnostico, actividad, materiales)
        estadosBorrador[id] = if (guardado) "Borrador guardado en el dispositivo" else "No se pudo guardar. Reintenta antes de salir."
        mensajeDetalle = id to if (guardado) "Borrador guardado en este dispositivo."
        else "No se pudo guardar el borrador."
    }

    fun eliminarFoto(id: String, nombre: String): Boolean =
        puedeActuar(id, Perfil.TECNICO) && repository.eliminarFoto(id, nombre)

    fun guardarFoto(id: String, nombre: String): Boolean {
        if (!puedeActuar(id, Perfil.TECNICO)) return false
        val guardado = repository.guardarFoto(id, nombre)
        mensajeDetalle = id to if (guardado) "Foto guardada en esta OT."
        else "No se pudo guardar la foto."
        return guardado
    }

    fun finalizarVisita(id: String, diagnostico: String, actividad: String, materiales: String) {
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

    fun observar(id: String, observacion: String) {
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

    fun responderObservacion(id: String, respuesta: String) {
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
