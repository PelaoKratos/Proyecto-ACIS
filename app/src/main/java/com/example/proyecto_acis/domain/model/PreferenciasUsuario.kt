package com.example.proyecto_acis.domain.model

data class PreferenciasUsuario(
    val recordarPerfil: Boolean = false,
    val perfil: Perfil? = null,
    val tecnico: String = "",
)
