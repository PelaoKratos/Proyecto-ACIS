package com.example.proyecto_acis.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ordenes", foreignKeys = [ForeignKey(entity = EquipoEntidad::class,
    parentColumns = ["id"], childColumns = ["equipoId"])],
    indices = [Index(value = ["equipoId"], name = "ordenes_por_equipo")])
data class OrdenEntidad(
    @PrimaryKey val id: String,
    val local: String,
    val equipo: String,
    val descripcion: String,
    val fechaProgramada: String,
    val tipo: String,
    val prioridad: String,
    val estado: String,
    val tecnico: String,
    val checkIn: String,
    @ColumnInfo(defaultValue = "''") val ubicacionCheckIn: String,
    val checkOut: String,
    val diagnostico: String,
    val actividad: String,
    val materiales: String,
    val observacionCliente: String,
    val respuestaTecnico: String,
    val fechaConformidad: String,
    @ColumnInfo(defaultValue = "''") val fotoEvidencia: String,
    @ColumnInfo(defaultValue = "0") val ultimaActualizacion: Long,
    val equipoId: String?,
    val slaClienteId: String?,
    val slaClienteNombre: String?,
    val slaHorasLlegada: Int?,
    val slaHorasResolucion: Int?,
    val slaInicio: Long?,
)
