package com.example.proyecto_acis.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoOperacion(val enCurso: Boolean = false, val error: String = "")

internal class OperacionesPantalla(private val scope: CoroutineScope) {
    private val _estado = MutableStateFlow(EstadoOperacion())
    val estado = _estado.asStateFlow()

    fun ejecutar(accion: suspend () -> Unit) {
        if (estado.value.enCurso) return
        _estado.value = EstadoOperacion(enCurso = true)
        scope.launch {
            try {
                accion()
                _estado.value = EstadoOperacion()
            } catch (cancelacion: CancellationException) {
                _estado.value = EstadoOperacion()
                throw cancelacion
            } catch (_: Exception) {
                _estado.value = EstadoOperacion(error = "No se pudo completar la operación. Reintenta; no se eliminaron tus registros.")
            }
        }
    }

    fun limpiarError() { if (!estado.value.enCurso) _estado.value = EstadoOperacion() }
}
