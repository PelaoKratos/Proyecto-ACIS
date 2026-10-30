package com.example.proyecto_acis.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.data.Perfil
import com.example.proyecto_acis.domain.*

class CatastroViewModel(application: Application) : AndroidViewModel(application) {
    private val dependencias = application as AcisApplication
    private val repository = dependencias.repository
    private val perfil get() = dependencias.sesion.perfil
    val catastro = repository.catastro
    private val ordenes = repository.ordenes
    var datosEquipo by mutableStateOf(DatosEquipo())
        private set
    private var mostrarErroresEquipo by mutableStateOf(false)
    var mensajeEquipo by mutableStateOf("")
        private set
    val erroresEquipo: ErroresEquipo get() = if (mostrarErroresEquipo)
        ValidadorEquipo.validar(datosEquipo, catastro.value, ordenes.value.mapNotNull { it.equipoId }.toSet()) else ErroresEquipo()

    fun prepararEquipo(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        val equipo = id?.let { elegido -> catastro.value.equipos.firstOrNull { it.id == elegido } ?: return false }
        datosEquipo = equipo?.let { DatosEquipo(it.id, it.localId, it.nombre, it.tipo, it.ubicacion, it.esExterior) } ?: DatosEquipo()
        mostrarErroresEquipo = false
        mensajeEquipo = ""
        return true
    }
    fun cambiarLocalEquipo(id: String) { datosEquipo = datosEquipo.copy(localId = id) }
    fun cambiarNombreEquipo(valor: String) { datosEquipo = datosEquipo.copy(nombre = valor) }
    fun cambiarTipoEquipo(valor: String) { datosEquipo = datosEquipo.copy(tipo = valor) }
    fun cambiarUbicacionEquipo(valor: String) { datosEquipo = datosEquipo.copy(ubicacion = valor) }
    fun cambiarExteriorEquipo(valor: Boolean) { datosEquipo = datosEquipo.copy(esExterior = valor) }
    fun guardarEquipo(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        mostrarErroresEquipo = true
        if (erroresEquipo.hayErrores) return false
        val guardado = repository.guardarEquipo(datosEquipo)
        mensajeEquipo = if (guardado) "Equipo guardado en el dispositivo." else "No se pudo guardar el equipo. Intenta nuevamente."
        return guardado
    }


    var datosCliente by mutableStateOf(DatosCliente())
        private set
    var datosLocal by mutableStateOf(DatosLocal())
        private set
    private var validarCliente by mutableStateOf(false)
    private var validarLocal by mutableStateOf(false)
    var mensajeCliente by mutableStateOf("")
        private set
    var mensajeLocal by mutableStateOf("")
        private set
    var mensajeUbicacion by mutableStateOf("")
        private set
    val errorCliente get() = if (validarCliente) ValidadorCatastro.cliente(datosCliente, catastro.value) else null
    val erroresLocal get() = if (validarLocal) ValidadorCatastro.local(datosLocal, catastro.value) else ErroresLocal()

    fun prepararCliente(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR || id == ValidadorCatastro.CLIENTE_PENDIENTE) return false
        val cliente = id?.let { elegido -> catastro.value.clientes.firstOrNull { it.id == elegido } ?: return false }
        datosCliente = cliente?.let { DatosCliente(it.id, it.nombre) } ?: DatosCliente()
        validarCliente = false
        mensajeCliente = ""
        return true
    }
    fun cambiarNombreCliente(valor: String) { datosCliente = datosCliente.copy(nombre = valor) }
    fun guardarCliente(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        validarCliente = true
        if (errorCliente != null) return false
        val guardado = repository.guardarCliente(datosCliente)
        mensajeCliente = if (guardado) "Cliente guardado." else "No se pudo guardar. Intenta nuevamente."
        return guardado
    }
    fun prepararLocal(id: String? = null): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        val local = id?.let { elegido -> catastro.value.locales.firstOrNull { it.id == elegido } ?: return false }
        datosLocal = local?.let { DatosLocal(it.id, it.clienteId, it.nombre, it.direccion, it.latitud, it.longitud) } ?: DatosLocal()
        validarLocal = false
        mensajeLocal = ""
        mensajeUbicacion = ""
        return true
    }
    fun cambiarClienteLocal(id: String) { datosLocal = datosLocal.copy(clienteId = id) }
    fun cambiarNombreLocal(valor: String) { datosLocal = datosLocal.copy(nombre = valor) }
    fun cambiarDireccionLocal(valor: String) { datosLocal = datosLocal.copy(direccion = valor) }
    fun registrarUbicacion(texto: String) {
        val coordenadas = ValidadorCatastro.leerUbicacion(texto)
        if (coordenadas == null) {
            mensajeUbicacion = "No se obtuvo la ubicación. Puedes continuar sin ella."
        } else {
            datosLocal = datosLocal.copy(latitud = coordenadas.first, longitud = coordenadas.second)
            mensajeUbicacion = "Ubicación registrada. Guarda el local para conservarla."
        }
    }
    fun informarUbicacion(mensaje: String) { mensajeUbicacion = mensaje }
    fun quitarUbicacion() {
        datosLocal = datosLocal.copy(latitud = null, longitud = null)
        mensajeUbicacion = "El local se guardará sin ubicación."
    }
    fun guardarLocal(): Boolean {
        if (perfil != Perfil.PLANIFICADOR) return false
        validarLocal = true
        if (erroresLocal.hayErrores) return false
        val guardado = repository.guardarLocal(datosLocal)
        mensajeLocal = if (guardado) "Local guardado." else "No se pudo guardar. Intenta nuevamente."
        return guardado
    }
}
