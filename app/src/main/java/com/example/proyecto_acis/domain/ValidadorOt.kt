package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.PrioridadOt
import com.example.proyecto_acis.data.TipoOt

data class DatosCrearOt(
    val local: String = "",
    val equipo: String = "",
    val descripcion: String = "",
    val tipo: TipoOt? = null,
    val prioridad: PrioridadOt? = null,
    val fecha: String = "",
    val hora: String = "",
)

data class ErroresCrearOt(
    val local: String? = null,
    val equipo: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    val prioridad: String? = null,
    val fecha: String? = null,
    val hora: String? = null,
) {
    val hayErrores: Boolean
        get() = listOf(local, equipo, descripcion, tipo, prioridad, fecha, hora).any { it != null }
}

data class ErroresInforme(
    val diagnostico: String? = null,
    val actividad: String? = null,
) {
    val hayErrores: Boolean get() = diagnostico != null || actividad != null
}

data class ErroresAsignacion(val tecnico: String? = null, val hora: String? = null) {
    val hayErrores: Boolean get() = tecnico != null || hora != null
}

object ValidadorOt {
    fun asignacion(tecnico: String, disponibles: List<String>, hora: String) = ErroresAsignacion(
        tecnico = if (tecnico !in disponibles) "Selecciona un técnico de la lista." else null,
        hora = if (!formatoValido(hora, "HH:mm")) "Selecciona una hora válida." else null,
    )

    fun crear(datos: DatosCrearOt) = ErroresCrearOt(
        local = if (datos.local.isBlank()) "Ingresa el local" else null,
        equipo = if (datos.equipo.isBlank()) "Ingresa el equipo" else null,
        descripcion = if (datos.descripcion.isBlank()) "Describe el trabajo" else null,
        tipo = if (datos.tipo == null || datos.tipo == TipoOt.SIN_ESPECIFICAR) "Selecciona un tipo" else null,
        prioridad = if (datos.prioridad == null || datos.prioridad == PrioridadOt.SIN_ESPECIFICAR) "Selecciona una prioridad" else null,
        fecha = when {
            datos.fecha.isBlank() -> "Selecciona una fecha"
            !formatoValido(datos.fecha, "dd/MM/yyyy") -> "Selecciona una fecha válida"
            else -> null
        },
        hora = when {
            datos.hora.isBlank() -> "Selecciona una hora"
            !formatoValido(datos.hora, "HH:mm") -> "Selecciona una hora válida"
            else -> null
        },
    )

    fun informe(diagnostico: String, actividad: String) = ErroresInforme(
        diagnostico = if (diagnostico.isBlank()) "Ingresa el diagnóstico" else null,
        actividad = if (actividad.isBlank()) "Ingresa la actividad" else null,
    )

    fun observacion(texto: String): String? = if (texto.isBlank()) "Escribe una observación" else null

    fun respuesta(texto: String): String? = if (texto.isBlank()) "Escribe una respuesta" else null

    fun postergacion(texto: String, ahora: Long = System.currentTimeMillis()): String? {
        if (!formatoValido(texto, "dd/MM/yyyy HH:mm")) return "Selecciona una fecha y hora válidas."
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.forLanguageTag("es-CL"))
        val fecha = formato.parse(texto) ?: return "Selecciona una fecha y hora válidas."
        return if (fecha.time <= ahora) "Elige una fecha y hora posteriores al momento actual." else null
    }

    private fun formatoValido(texto: String, patron: String): Boolean {
        val formato = java.text.SimpleDateFormat(patron, java.util.Locale.forLanguageTag("es-CL")).apply { isLenient = false }
        val posicion = java.text.ParsePosition(0)
        val fecha = formato.parse(texto, posicion) ?: return false
        return posicion.index == texto.length && formato.format(fecha) == texto
    }
}
