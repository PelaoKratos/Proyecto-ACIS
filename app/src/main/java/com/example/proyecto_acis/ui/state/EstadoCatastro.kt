package com.example.proyecto_acis.ui.state

import com.example.proyecto_acis.domain.validation.DatosCliente
import com.example.proyecto_acis.domain.validation.DatosEquipo
import com.example.proyecto_acis.domain.validation.DatosLocal
import com.example.proyecto_acis.domain.validation.ErroresCliente
import com.example.proyecto_acis.domain.validation.ErroresEquipo
import com.example.proyecto_acis.domain.validation.ErroresLocal

data class EstadoCliente(
    val datos: DatosCliente = DatosCliente(),
    val mostrarErrores: Boolean = false,
    val errores: ErroresCliente = ErroresCliente(),
    val mensaje: String = "",
)

data class EstadoLocal(
    val datos: DatosLocal = DatosLocal(),
    val mostrarErrores: Boolean = false,
    val errores: ErroresLocal = ErroresLocal(),
    val mensaje: String = "",
    val mensajeUbicacion: String = "",
)

data class EstadoEquipo(
    val datos: DatosEquipo = DatosEquipo(),
    val mostrarErrores: Boolean = false,
    val errores: ErroresEquipo = ErroresEquipo(),
    val mensaje: String = "",
)

data class EstadoCatastro(
    val cliente: EstadoCliente = EstadoCliente(),
    val local: EstadoLocal = EstadoLocal(),
    val equipo: EstadoEquipo = EstadoEquipo(),
)
