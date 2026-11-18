package com.example.proyecto_acis.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PreferenciasUsuario
import kotlinx.coroutines.flow.first

private val Context.preferenciasAcis by preferencesDataStore(name = "acis_preferencias")

class PreferenciasRepository(private val almacen: DataStore<Preferences>) {
    suspend fun leer(): PreferenciasUsuario = almacen.data.first().aModelo()

    suspend fun recordar(activar: Boolean, perfil: Perfil, tecnico: String): PreferenciasUsuario =
        almacen.edit {
            it[RECORDAR] = activar
            if (activar) it.guardarAcceso(perfil, tecnico)
            else { it.remove(PERFIL); it.remove(TECNICO) }
        }.aModelo()

    suspend fun actualizarAcceso(perfil: Perfil, tecnico: String): PreferenciasUsuario =
        almacen.edit { if (it[RECORDAR] == true) it.guardarAcceso(perfil, tecnico) }.aModelo()

    private fun MutablePreferences.guardarAcceso(perfil: Perfil, tecnico: String) {
        require(perfil != Perfil.TECNICO || tecnico.isNotBlank())
        this[PERFIL] = perfil.name
        this[TECNICO] = tecnico
    }

    private fun Preferences.aModelo(): PreferenciasUsuario {
        val perfil = Perfil.entries.firstOrNull { it.name == this[PERFIL] }
        return if (this[RECORDAR] == true && perfil != null)
            PreferenciasUsuario(true, perfil, this[TECNICO].orEmpty()) else PreferenciasUsuario()
    }

    companion object {
        private val RECORDAR = booleanPreferencesKey("recordar_perfil")
        private val PERFIL = stringPreferencesKey("ultimo_perfil")
        private val TECNICO = stringPreferencesKey("ultimo_tecnico")
        fun crear(context: Context) = PreferenciasRepository(context.applicationContext.preferenciasAcis)
    }
}
