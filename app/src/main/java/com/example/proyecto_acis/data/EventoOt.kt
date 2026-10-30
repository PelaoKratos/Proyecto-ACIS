package com.example.proyecto_acis.data

enum class TipoEvento(val titulo: String, val perfil: Perfil? = null) {
    REGISTRO_PREVIO("Registro previo conservado"),
    CREACION("Orden creada", Perfil.PLANIFICADOR),
    ASIGNACION("Técnico asignado", Perfil.PLANIFICADOR),
    EDICION("Datos de la orden actualizados", Perfil.PLANIFICADOR),
    ACEPTACION("Orden aceptada", Perfil.TECNICO),
    RECHAZO("Orden rechazada", Perfil.TECNICO),
    POSTERGACION("Visita postergada", Perfil.TECNICO),
    LLEGADA("Llegada registrada", Perfil.TECNICO),
    INFORME("Salida e informe registrados", Perfil.TECNICO),
    OBSERVACION("Observación del cliente", Perfil.CLIENTE),
    RESPUESTA("Respuesta del técnico", Perfil.TECNICO),
    CONFORMIDAD("Trabajo aprobado", Perfil.CLIENTE),
}

data class EventoOt(
    val id: Long = 0,
    val otId: String,
    val tipo: TipoEvento,
    val actor: String,
    val fechaMillis: Long,
    val detalle: String = "",
    val rolActor: String = tipo.perfil?.name ?: "SISTEMA",
)

fun detalleRegistroPrevio(estado: String, llegada: String, salida: String, conformidad: String): String =
    buildList {
        add("Orden conservada desde registros locales. Estado al incorporarla: $estado.")
        if (llegada.isNotBlank()) add("Llegada anterior: $llegada")
        if (salida.isNotBlank()) add("Salida e informe anteriores: $salida")
        if (conformidad.isNotBlank()) add("Conformidad anterior: $conformidad")
    }.joinToString("\n")
