package com.example.proyecto_acis.ui

import androidx.compose.runtime.*
import com.example.proyecto_acis.data.Perfil
import com.example.proyecto_acis.data.OrdenesRepository

class SesionDemo {
    var perfil by mutableStateOf(Perfil.PLANIFICADOR)
        internal set
    var tecnico by mutableStateOf(OrdenesRepository.TECNICO_DEMO)
        internal set
}
