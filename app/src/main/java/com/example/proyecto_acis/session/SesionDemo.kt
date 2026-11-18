package com.example.proyecto_acis.session

import androidx.compose.runtime.*
import com.example.proyecto_acis.data.repository.OrdenesRepository
import com.example.proyecto_acis.domain.model.Perfil

class SesionDemo {
    var perfil by mutableStateOf(Perfil.PLANIFICADOR)
        internal set
    var tecnico by mutableStateOf(OrdenesRepository.TECNICO_DEMO)
        internal set
}
