package com.example.proyecto_acis

import android.app.Application
import com.example.proyecto_acis.data.local.CacheClimaLocal
import com.example.proyecto_acis.data.remote.OpenMeteo
import com.example.proyecto_acis.data.repository.ClimaRepository
import com.example.proyecto_acis.data.repository.OrdenesRepository
import com.example.proyecto_acis.data.preferences.PreferenciasRepository
import com.example.proyecto_acis.session.SesionDemo

import kotlinx.coroutines.*

class AcisApplication : Application() {
    internal val tareas = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository by lazy { OrdenesRepository(this).also { repo -> tareas.launch { repo.inicializar() } } }
    val clima by lazy {
        ClimaRepository(
            OpenMeteo(), CacheClimaLocal(this))
    }
    val sesion = SesionDemo()
    val preferencias by lazy { PreferenciasRepository.crear(this) }
}
