package com.example.proyecto_acis.ui.state

import com.example.proyecto_acis.domain.service.MaterialInforme
import com.example.proyecto_acis.domain.validation.ErroresAsignacion

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

data class EstadoDetalleOrden(
    val formularios: Map<String, FormularioDetalle> = emptyMap(),
    val borradores: Map<String, String> = emptyMap(),
    val erroresAsignacion: Map<String, ErroresAsignacion> = emptyMap(),
    val informeConErrores: String? = null,
    val observacionConError: String? = null,
    val respuestaConError: String? = null,
    val mensaje: Pair<String, String>? = null,
)
