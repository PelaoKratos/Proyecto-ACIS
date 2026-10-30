package com.example.proyecto_acis

import android.app.Application
import com.example.proyecto_acis.data.ClimaRepository
import com.example.proyecto_acis.data.OpenMeteo
import com.example.proyecto_acis.data.CacheClimaLocal
import com.example.proyecto_acis.data.OrdenesRepository
import com.example.proyecto_acis.ui.SesionDemo

class AcisApplication : Application() {
    val repository by lazy { OrdenesRepository(this) }
    val clima by lazy {
        ClimaRepository(
            OpenMeteo(), CacheClimaLocal(this))
    }
    val sesion = SesionDemo()
}
