package com.example.proyecto_acis.data.remote

import com.example.proyecto_acis.data.repository.FuenteClima
import com.example.proyecto_acis.domain.service.Pronostico
import com.example.proyecto_acis.domain.service.SolicitudClima
import com.example.proyecto_acis.domain.validation.ValidadorCatastro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenMeteo(
    private val servicio: ServicioClima = ClienteClima.crearServicio(),
    private val reloj: () -> Long = System::currentTimeMillis,
) : FuenteClima {
    override suspend fun cargar(solicitud: SolicitudClima): Pronostico = withContext(Dispatchers.IO) {
        require(ValidadorCatastro.coordenadasValidas(solicitud.latitud, solicitud.longitud))
        servicio.pronostico(solicitud.latitud, solicitud.longitud).aPronostico(reloj())
    }
}
