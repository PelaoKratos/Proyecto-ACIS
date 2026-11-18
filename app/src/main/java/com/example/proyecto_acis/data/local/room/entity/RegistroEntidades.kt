package com.example.proyecto_acis.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_ot", foreignKeys = [ForeignKey(entity = OrdenEntidad::class,
    parentColumns = ["id"], childColumns = ["otId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["otId", "fechaMillis", "id"], name = "eventos_por_orden")])
data class EventoEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val otId: String,
    val tipo: String,
    val rol: String,
    val actor: String,
    val fechaMillis: Long,
    val detalle: String,
)

@Entity(tableName = "sync_ot", foreignKeys = [ForeignKey(entity = OrdenEntidad::class,
    parentColumns = ["id"], childColumns = ["otId"], onDelete = ForeignKey.CASCADE)])
data class SyncEntidad(
    @PrimaryKey val otId: String,
    val revision: Long,
    val estado: String,
    val intentoMillis: Long? = null,
)
