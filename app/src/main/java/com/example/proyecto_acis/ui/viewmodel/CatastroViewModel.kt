package com.example.proyecto_acis.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.validation.DatosCliente
import com.example.proyecto_acis.domain.validation.DatosEquipo
import com.example.proyecto_acis.domain.validation.DatosLocal
import com.example.proyecto_acis.domain.validation.ErroresCliente
import com.example.proyecto_acis.domain.validation.ErroresEquipo
import com.example.proyecto_acis.domain.validation.ErroresLocal
import com.example.proyecto_acis.domain.validation.ValidadorCatastro
import com.example.proyecto_acis.domain.validation.ValidadorEquipo

import com.example.proyecto_acis.ui.state.EstadoCatastro
import com.example.proyecto_acis.ui.state.EstadoCliente
import com.example.proyecto_acis.ui.state.EstadoEquipo
import com.example.proyecto_acis.ui.state.EstadoLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CatastroViewModel(application: Application) : AndroidViewModel(application) {
    private val acciones = OperacionesPantalla(viewModelScope)
    val operacion = acciones.estado
    fun ejecutar(accion: suspend () -> Unit) = acciones.ejecutar(accion)
    fun limpiarError() = acciones.limpiarError()

    private val dependencias = application as AcisApplication
    private val repository = dependencias.repository
    private val perfil get() = dependencias.sesion.perfil
    val catastro = repository.catastro
    private val ordenes = repository.ordenes
    private val _estado = MutableStateFlow(EstadoCatastro())
    val estado = _estado.asStateFlow()

    val datosEquipo get() = estado.value.equipo.datos
    val mensajeEquipo get() = estado.value.equipo.mensaje
    val erroresEquipo get() = estado.value.equipo.errores

    fun prepararEquipo(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        val equipo = id?.let { elegido -> catastro.value.equipos.firstOrNull { it.id == elegido } ?: return false }
        cambiarEquipo { EstadoEquipo(datos = equipo?.let {
            DatosEquipo(it.id, it.localId, it.nombre, it.tipo, it.ubicacion, it.esExterior)
        } ?: DatosEquipo()) }
        return true
    }
    fun cambiarLocalEquipo(id: String) { cambiarEquipo { it.copy(datos = it.datos.copy(localId = id), mensaje = "") } }
    fun cambiarNombreEquipo(valor: String) { cambiarEquipo { it.copy(datos = it.datos.copy(nombre = valor), mensaje = "") } }
    fun cambiarTipoEquipo(valor: String) { cambiarEquipo { it.copy(datos = it.datos.copy(tipo = valor), mensaje = "") } }
    fun cambiarUbicacionEquipo(valor: String) { cambiarEquipo { it.copy(datos = it.datos.copy(ubicacion = valor), mensaje = "") } }
    fun cambiarExteriorEquipo(valor: Boolean) { cambiarEquipo { it.copy(datos = it.datos.copy(esExterior = valor), mensaje = "") } }
    suspend fun guardarEquipo(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        cambiarEquipo { it.copy(mostrarErrores = true) }
        if (erroresEquipo.hayErrores) return false
        val guardado = repository.guardarEquipo(datosEquipo)
        cambiarEquipo { it.copy(mostrarErrores = !guardado, mensaje = if (guardado) "Equipo guardado en el dispositivo." else "No se pudo guardar el equipo. Intenta nuevamente.") }
        return guardado
    }


    val datosCliente get() = estado.value.cliente.datos
    val datosLocal get() = estado.value.local.datos
    val mensajeCliente get() = estado.value.cliente.mensaje
    val mensajeLocal get() = estado.value.local.mensaje
    val mensajeUbicacion get() = estado.value.local.mensajeUbicacion
    val erroresCliente get() = estado.value.cliente.errores
    val errorCliente get() = erroresCliente.nombre
    val erroresLocal get() = estado.value.local.errores

    fun prepararCliente(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR || id == ValidadorCatastro.CLIENTE_PENDIENTE) return false
        val cliente = id?.let { elegido -> catastro.value.clientes.firstOrNull { it.id == elegido } ?: return false }
        cambiarCliente { EstadoCliente(datos = cliente?.let {
            DatosCliente(it.id, it.nombre, it.horasLlegada?.toString().orEmpty(), it.horasResolucion?.toString().orEmpty())
        } ?: DatosCliente()) }
        return true
    }
    fun cambiarNombreCliente(valor: String) { cambiarCliente { it.copy(datos = it.datos.copy(nombre = valor), mensaje = "") } }
    fun cambiarHorasLlegada(valor: String) { cambiarCliente { it.copy(datos = it.datos.copy(horasLlegada = valor), mensaje = "") } }
    fun cambiarHorasResolucion(valor: String) { cambiarCliente { it.copy(datos = it.datos.copy(horasResolucion = valor), mensaje = "") } }
    suspend fun guardarCliente(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        cambiarCliente { it.copy(mostrarErrores = true) }
        if (erroresCliente.hayErrores) return false
        val guardado = repository.guardarCliente(datosCliente)
        cambiarCliente { it.copy(mostrarErrores = !guardado, mensaje = if (guardado) "Cliente guardado." else "No se pudo guardar. Intenta nuevamente.") }
        return guardado
    }
    fun prepararLocal(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        val local = id?.let { elegido -> catastro.value.locales.firstOrNull { it.id == elegido } ?: return false }
        cambiarLocal { EstadoLocal(datos = local?.let {
            DatosLocal(it.id, it.clienteId, it.nombre, it.direccion, it.latitud, it.longitud)
        } ?: DatosLocal()) }
        return true
    }
    fun cambiarClienteLocal(id: String) { cambiarLocal { it.copy(datos = it.datos.copy(clienteId = id), mensaje = "") } }
    fun cambiarNombreLocal(valor: String) { cambiarLocal { it.copy(datos = it.datos.copy(nombre = valor), mensaje = "") } }
    fun cambiarDireccionLocal(valor: String) { cambiarLocal { it.copy(datos = it.datos.copy(direccion = valor), mensaje = "") } }
    fun registrarUbicacion(texto: String) {
        val coordenadas = ValidadorCatastro.leerUbicacion(texto)
        if (coordenadas == null) {
            cambiarLocal { it.copy(mensajeUbicacion = "No se obtuvo la ubicación. Puedes continuar sin ella.") }
        } else {
            cambiarLocal { it.copy(datos = it.datos.copy(latitud = coordenadas.first, longitud = coordenadas.second), mensaje = "") }
            cambiarLocal { it.copy(mensajeUbicacion = "Ubicación registrada. Guarda el local para conservarla.") }
        }
    }
    fun informarUbicacion(mensaje: String) { cambiarLocal { it.copy(mensajeUbicacion = mensaje) } }
    fun quitarUbicacion() {
        cambiarLocal { it.copy(datos = it.datos.copy(latitud = null, longitud = null), mensaje = "") }
        cambiarLocal { it.copy(mensajeUbicacion = "El local se guardará sin ubicación.") }
    }
    suspend fun guardarLocal(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        cambiarLocal { it.copy(mostrarErrores = true) }
        if (erroresLocal.hayErrores) return false
        val guardado = repository.guardarLocal(datosLocal)
        cambiarLocal { it.copy(mostrarErrores = !guardado, mensaje = if (guardado) "Local guardado." else "No se pudo guardar. Intenta nuevamente.") }
        return guardado
    }
    private fun cambiarEquipo(cambio: (EstadoEquipo) -> EstadoEquipo) {
        _estado.update {
            val nuevo = cambio(it.equipo)
            val errores = if (nuevo.mostrarErrores) ValidadorEquipo.validar(nuevo.datos,
                catastro.value, ordenes.value.mapNotNull { orden -> orden.equipoId }.toSet()) else ErroresEquipo()
            it.copy(equipo = nuevo.copy(errores = errores))
        }
    }
    private fun cambiarCliente(cambio: (EstadoCliente) -> EstadoCliente) {
        _estado.update {
            val nuevo = cambio(it.cliente)
            val errores = if (nuevo.mostrarErrores) ValidadorCatastro.erroresCliente(nuevo.datos, catastro.value) else ErroresCliente()
            it.copy(cliente = nuevo.copy(errores = errores))
        }
    }
    private fun cambiarLocal(cambio: (EstadoLocal) -> EstadoLocal) {
        _estado.update {
            val nuevo = cambio(it.local)
            val errores = if (nuevo.mostrarErrores) ValidadorCatastro.local(nuevo.datos, catastro.value) else ErroresLocal()
            it.copy(local = nuevo.copy(errores = errores))
        }
    }

}
