package com.example.proyecto_acis.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.proyecto_acis.data.local.room.entity.OrdenEntidad

@Dao
interface OrdenDao {
    @Query("SELECT * FROM ordenes ORDER BY id ASC")
    fun leer(): List<OrdenEntidad>

    @Query("SELECT * FROM ordenes WHERE id = :id")
    fun buscar(id: String): OrdenEntidad?

    @Insert fun insertar(orden: OrdenEntidad)
    @Update fun actualizar(orden: OrdenEntidad): Int

    @Query("DELETE FROM ordenes WHERE id = :id AND estado = :estado")
    fun eliminar(id: String, estado: String): Int

    @Query("SELECT DISTINCT equipoId FROM ordenes WHERE equipoId IS NOT NULL")
    fun equiposConOrdenes(): List<String>
}
