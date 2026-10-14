package com.example.proyecto_acis.data

enum class Perfil(val etiqueta: String) {
    PLANIFICADOR("Planificador"),
    TECNICO("Técnico"),
    CLIENTE("Cliente"),
}

enum class EstadoOt(val etiqueta: String) {
    CREADA("Creada"),
    ASIGNADA("Asignada"),
    ACEPTADA("Aceptada"),
    EN_VISITA("En visita"),
    PENDIENTE_CONFORMIDAD("Pendiente de conformidad"),
    OBSERVADA("Observada"),
    CERRADA("Cerrada"),
}

data class OrdenTrabajo(
    val id: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    val estado: EstadoOt = EstadoOt.CREADA,
    val tecnico: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val diagnostico: String = "",
    val actividad: String = "",
    val materiales: String = "",
    val observacionCliente: String = "",
    val respuestaTecnico: String = "",
    val fechaConformidad: String = "",
)
