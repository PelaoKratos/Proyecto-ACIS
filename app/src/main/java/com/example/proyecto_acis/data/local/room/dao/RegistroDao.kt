package com.example.proyecto_acis.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.proyecto_acis.data.local.room.entity.EventoEntidad
import com.example.proyecto_acis.data.local.room.entity.SyncEntidad

@Dao
interface RegistroDao {
    @Query("SELECT * FROM eventos_ot ORDER BY fechaMillis ASC, id ASC")
    fun eventos(): List<EventoEntidad>
    @Insert fun insertar(evento: EventoEntidad)

    @Query("SELECT * FROM sync_ot ORDER BY otId ASC") fun sincronizacion(): List<SyncEntidad>
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertar(registro: SyncEntidad)

    @Query("UPDATE sync_ot SET revision = revision + 1, estado = 'PENDIENTE', intentoMillis = NULL WHERE otId = :id")
    fun marcarPendiente(id: String)

    @Query("UPDATE sync_ot SET estado = :estado, intentoMillis = :instante WHERE otId = :id AND revision = :revision AND estado != 'ENVIADO'")
    fun resultado(id: String, revision: Long, estado: String, instante: Long): Int
}
