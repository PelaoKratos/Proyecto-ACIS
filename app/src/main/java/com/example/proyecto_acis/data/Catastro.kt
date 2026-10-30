package com.example.proyecto_acis.data

data class Cliente(val id: String, val nombre: String, val esDemo: Boolean = false)

data class Local(
    val id: String,
    val clienteId: String,
    val nombre: String,
    val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
)

data class Equipo(
    val id: String,
    val localId: String,
    val nombre: String,
    val tipo: String,
    val ubicacion: String = "",
    val esExterior: Boolean = false,
    val ultimaMantencionMillis: Long? = null,
)

data class Catastro(
    val clientes: List<Cliente> = emptyList(),
    val locales: List<Local> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
)
