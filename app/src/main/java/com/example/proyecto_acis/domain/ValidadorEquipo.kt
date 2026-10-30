package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro

data class DatosEquipo(
    val id: String? = null,
    val localId: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val ubicacion: String = "",
    val esExterior: Boolean = false,
)

data class ErroresEquipo(val local: String? = null, val nombre: String? = null, val tipo: String? = null) {
    val hayErrores: Boolean get() = local != null || nombre != null || tipo != null
}

object ValidadorEquipo {
    fun validar(datos: DatosEquipo, catastro: Catastro, equiposConOrdenes: Set<String> = emptySet()): ErroresEquipo {
        val anterior = catastro.equipos.firstOrNull { it.id == datos.id }
        return ErroresEquipo(
            local = when {
                catastro.locales.none { it.id == datos.localId } -> "Selecciona un local del catálogo."
                anterior != null && anterior.localId != datos.localId && anterior.id in equiposConOrdenes ->
                    "Este equipo tiene órdenes registradas. Conserva su local."
                else -> null
            },
            nombre = when {
                datos.nombre.isBlank() -> "Ingresa el nombre del equipo."
                catastro.equipos.any { it.id != datos.id && it.localId == datos.localId && it.nombre.equals(datos.nombre.trim(), ignoreCase = true) } ->
                    "Ya existe un equipo con ese nombre en el local."
                else -> null
            },
            tipo = if (datos.tipo.isBlank()) "Indica el tipo de equipo, por ejemplo refrigeración." else null,
        )
    }
}
