package com.example.proyecto_acis.ui.state

import com.example.proyecto_acis.domain.model.PreferenciasUsuario

data class EstadoPreferencias(
    val datos: PreferenciasUsuario = PreferenciasUsuario(),
    val cargando: Boolean = true,
    val mensaje: String = "",
)
