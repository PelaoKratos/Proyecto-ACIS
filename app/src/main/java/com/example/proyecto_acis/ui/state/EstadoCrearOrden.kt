package com.example.proyecto_acis.ui.state

import com.example.proyecto_acis.domain.validation.DatosCrearOt
import com.example.proyecto_acis.domain.validation.ErroresCrearOt
import com.example.proyecto_acis.domain.validation.ValidadorOt

data class EstadoCrearOrden(
    val datos: DatosCrearOt = DatosCrearOt(),
    val paso: Int = 1,
    val mostrarErrores: Boolean = false,
    val errorGuardado: Boolean = false,
) {
    val errores: ErroresCrearOt
        get() = if (mostrarErrores) ValidadorOt.crear(datos) else ErroresCrearOt()

    fun validarPaso(): Boolean {
        val errores = ValidadorOt.crear(datos)
        return when (paso) {
            1 -> errores.local == null && errores.equipo == null
            2 -> errores.descripcion == null && errores.tipo == null && errores.prioridad == null
            else -> !errores.hayErrores
        }
    }
}
