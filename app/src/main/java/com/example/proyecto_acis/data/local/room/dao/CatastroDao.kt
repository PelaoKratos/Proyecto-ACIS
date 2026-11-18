package com.example.proyecto_acis.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.proyecto_acis.data.local.room.entity.ClienteEntidad
import com.example.proyecto_acis.data.local.room.entity.EquipoEntidad
import com.example.proyecto_acis.data.local.room.entity.LocalEntidad

@Dao
interface CatastroDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC") fun clientes(): List<ClienteEntidad>
    @Query("SELECT * FROM locales ORDER BY nombre ASC") fun locales(): List<LocalEntidad>
    @Query("SELECT * FROM equipos ORDER BY nombre ASC") fun equipos(): List<EquipoEntidad>
    @Insert fun insertar(cliente: ClienteEntidad)
    @Insert fun insertar(local: LocalEntidad)
    @Insert fun insertar(equipo: EquipoEntidad)
    @Update fun actualizar(cliente: ClienteEntidad): Int
    @Update fun actualizar(local: LocalEntidad): Int
    @Update fun actualizar(equipo: EquipoEntidad): Int

    @Query("SELECT id FROM locales WHERE nombre = :nombre COLLATE NOCASE LIMIT 1")
    fun localPorNombre(nombre: String): String?

    @Query("SELECT id FROM equipos WHERE localId = :localId AND nombre = :nombre COLLATE NOCASE LIMIT 1")
    fun equipoPorNombre(localId: String, nombre: String): String?

    @Query("UPDATE equipos SET ultimaMantencion = :fecha WHERE id = :id AND (ultimaMantencion IS NULL OR ultimaMantencion < :fecha)")
    fun registrarMantencion(id: String, fecha: Long)
}
