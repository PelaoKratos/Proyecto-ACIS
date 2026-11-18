package com.example.proyecto_acis.data.local.room

import com.example.proyecto_acis.data.local.room.entity.*
import com.example.proyecto_acis.data.sync.EstadoSync
import com.example.proyecto_acis.data.sync.RegistroSync
import com.example.proyecto_acis.domain.model.*

internal fun OrdenTrabajo.aEntidad() = OrdenEntidad(
    id, local, equipo, descripcion, fechaProgramada, tipo.name, prioridad.name, estado.name,
    tecnico, checkIn, ubicacionCheckIn, checkOut, diagnostico, actividad, materiales,
    observacionCliente, respuestaTecnico, fechaConformidad, fotoEvidencia, ultimaActualizacion,
    equipoId, acuerdoSla?.clienteId, acuerdoSla?.clienteNombre, acuerdoSla?.horasLlegada,
    acuerdoSla?.horasResolucion, acuerdoSla?.inicioMillis,
)

internal fun OrdenEntidad.aModelo() = OrdenTrabajo(
    id = id, local = local, equipo = equipo, descripcion = descripcion, fechaProgramada = fechaProgramada,
    tipo = TipoOt.entries.firstOrNull { it.name == tipo } ?: TipoOt.SIN_ESPECIFICAR,
    prioridad = PrioridadOt.entries.firstOrNull { it.name == prioridad } ?: PrioridadOt.SIN_ESPECIFICAR,
    estado = EstadoOt.entries.firstOrNull { it.name == estado } ?: EstadoOt.CREADA,
    tecnico = tecnico, checkIn = checkIn, ubicacionCheckIn = ubicacionCheckIn, checkOut = checkOut,
    diagnostico = diagnostico, actividad = actividad, materiales = materiales,
    observacionCliente = observacionCliente, respuestaTecnico = respuestaTecnico,
    fechaConformidad = fechaConformidad, fotoEvidencia = fotoEvidencia,
    ultimaActualizacion = ultimaActualizacion, equipoId = equipoId,
    acuerdoSla = slaClienteId?.let { AcuerdoSla(it, slaClienteNombre.orEmpty(),
        slaHorasLlegada ?: 0, slaHorasResolucion ?: 0, slaInicio ?: 0) },
)

internal fun EventoOt.aEntidad() = EventoEntidad(id, otId, tipo.name, rolActor, actor, fechaMillis, detalle)
internal fun EventoEntidad.aModelo() = EventoOt(id, otId,
    TipoEvento.entries.firstOrNull { it.name == tipo } ?: TipoEvento.REGISTRO_PREVIO,
    actor, fechaMillis, detalle, rol)
internal fun SyncEntidad.aModelo() = RegistroSync(otId, revision,
    EstadoSync.entries.firstOrNull { it.name == estado } ?: EstadoSync.LOCAL, intentoMillis)
internal fun ClienteEntidad.aModelo() = Cliente(id, nombre, esDemo, horasLlegada, horasResolucion)
internal fun LocalEntidad.aModelo() = Local(id, clienteId, nombre, direccion, latitud, longitud)
internal fun EquipoEntidad.aModelo() = Equipo(id, localId, nombre, tipo, ubicacion, esExterior, ultimaMantencion)
