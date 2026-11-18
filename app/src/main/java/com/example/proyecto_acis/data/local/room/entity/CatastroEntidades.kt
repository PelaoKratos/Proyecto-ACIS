package com.example.proyecto_acis.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clientes", indices = [Index(value = ["nombre"], unique = true, name = "clientes_nombre")])
data class ClienteEntidad(
    @PrimaryKey val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    @ColumnInfo(defaultValue = "0") val esDemo: Boolean = false,
    val horasLlegada: Int? = null,
    val horasResolucion: Int? = null,
)

@Entity(tableName = "locales", foreignKeys = [ForeignKey(entity = ClienteEntidad::class,
    parentColumns = ["id"], childColumns = ["clienteId"])], indices = [
    Index(value = ["clienteId"], name = "locales_por_cliente"),
    Index(value = ["nombre"], unique = true, name = "locales_nombre")])
data class LocalEntidad(
    @PrimaryKey val id: String,
    val clienteId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    @ColumnInfo(defaultValue = "''") val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
)

@Entity(tableName = "equipos", foreignKeys = [ForeignKey(entity = LocalEntidad::class,
    parentColumns = ["id"], childColumns = ["localId"])], indices = [
    Index(value = ["localId"], name = "equipos_por_local"),
    Index(value = ["localId", "nombre"], unique = true, name = "equipos_local_nombre")])
data class EquipoEntidad(
    @PrimaryKey val id: String,
    val localId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    val tipo: String,
    @ColumnInfo(defaultValue = "''") val ubicacion: String = "",
    @ColumnInfo(defaultValue = "0") val esExterior: Boolean = false,
    val ultimaMantencion: Long? = null,
)
