package com.example.proyecto_acis.ui

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.*
import kotlinx.coroutines.launch

data class EstadoClima(val cargando: Boolean = false, val resultado: ResultadoClima? = null)

class ClimaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as AcisApplication).clima
    private val estados = mutableStateMapOf<SolicitudClima, EstadoClima>()
    fun estado(solicitud: SolicitudClima) = estados[solicitud] ?: EstadoClima()
    fun consultar(solicitud: SolicitudClima, forzar: Boolean = false) {
        if (estados[solicitud]?.cargando == true) return
        val anterior = estados[solicitud]?.resultado
        val actualizado = anterior?.actualizado
        if (!forzar && actualizado != null && System.currentTimeMillis() - actualizado in 0..ReglasClima.VIGENCIA) return
        estados[solicitud] = EstadoClima(cargando = true, resultado = estados[solicitud]?.resultado)
        viewModelScope.launch {
            val resultado = repository.consultar(solicitud, forzar)
            estados[solicitud] = EstadoClima(resultado = resultado)
        }
    }
}
