package com.example.proyecto_acis.ui.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_acis.AcisApplication
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.domain.service.ResultadoClima
import com.example.proyecto_acis.domain.service.SolicitudClima
import kotlinx.coroutines.launch

data class EstadoClima(val cargando: Boolean = false, val resultado: ResultadoClima? = null)

class ClimaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as AcisApplication).clima
    private val _estados = MutableStateFlow<Map<SolicitudClima, EstadoClima>>(emptyMap())
    val estados = _estados.asStateFlow()
    fun consultar(solicitud: SolicitudClima, forzar: Boolean = false) {
        if (estados.value[solicitud]?.cargando == true) return
        val anterior = estados.value[solicitud]?.resultado
        val actualizado = anterior?.actualizado
        if (!forzar && actualizado != null && System.currentTimeMillis() - actualizado in 0..ReglasClima.VIGENCIA) return
        _estados.update { it + (solicitud to EstadoClima(cargando = true, resultado = it[solicitud]?.resultado)) }
        viewModelScope.launch {
            try {
                val resultado = repository.consultar(solicitud, forzar)
                _estados.update { it + (solicitud to EstadoClima(resultado = resultado)) }
            } finally {
                _estados.update { it + (solicitud to (it[solicitud] ?: EstadoClima()).copy(cargando = false)) }
            }
        }
    }
}
