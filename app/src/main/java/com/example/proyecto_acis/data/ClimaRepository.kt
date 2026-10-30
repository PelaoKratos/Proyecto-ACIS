package com.example.proyecto_acis.data

import com.example.proyecto_acis.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface FuenteClima {
    suspend fun cargar(solicitud: SolicitudClima): Pronostico
}
interface CacheClima {
    fun leer(clave: String): Pronostico?
    fun guardar(clave: String, pronostico: Pronostico)
}
class ClimaRepository(private val fuente: FuenteClima, private val cache: CacheClima,
    private val reloj: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    suspend fun consultar(solicitud: SolicitudClima, forzar: Boolean = false): ResultadoClima = mutex.withLock {
        val anterior = cache.leer(solicitud.claveLocal)
        if (!forzar && anterior != null && ReglasClima.vigente(anterior, reloj()))
            return@withLock ReglasClima.resultado(anterior, solicitud, reloj(), cache = false)
        try {
            val nuevo = fuente.cargar(solicitud)
            cache.guardar(solicitud.claveLocal, nuevo)
            ReglasClima.resultado(nuevo, solicitud, reloj(), cache = false)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (_: Exception) {
            ReglasClima.resultado(anterior, solicitud, reloj(), cache = true)
        }
    }
}
