package com.example.proyecto_acis

import android.app.Application
import com.example.proyecto_acis.data.OrdenesRepository
import com.example.proyecto_acis.ui.SesionDemo

class AcisApplication : Application() {
    val repository by lazy { OrdenesRepository(this) }
    val sesion = SesionDemo()
}
