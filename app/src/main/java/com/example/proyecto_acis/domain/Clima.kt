package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import java.util.Locale

const val UMBRAL_LLUVIA = 60
const val UMBRAL_VIENTO = 40.0

data class SolicitudClima(val latitud: Double, val longitud: Double, val instante: Long) {
    val claveLocal get() = String.format(Locale.US, "%.5f,%.5f", latitud, longitud)
    val hora get() = instante / 3_600_000L * 3_600_000L
}
data class HoraClima(val instante: Long, val lluvia: Int, val viento: Double) {
    val hayAlerta get() = lluvia >= UMBRAL_LLUVIA || viento >= UMBRAL_VIENTO
}
data class Pronostico(val actualizado: Long, val horas: List<HoraClima>)
data class ResultadoClima(val hora: HoraClima?, val actualizado: Long?, val enCache: Boolean, val aviso: String = "")
data class ContextoClima(val local: String, val solicitud: SolicitudClima?, val aviso: String = "")

object ReglasClima {
    const val VIGENCIA = 30 * 60_000L
    const val MAX_CACHE = 48 * 3_600_000L
    fun vigente(pronostico: Pronostico, ahora: Long) = ahora - pronostico.actualizado in 0..VIGENCIA
    fun resultado(pronostico: Pronostico?, solicitud: SolicitudClima, ahora: Long, cache: Boolean): ResultadoClima {
        if (pronostico == null || ahora - pronostico.actualizado !in 0..MAX_CACHE)
            return ResultadoClima(null, null, cache, "No hay un pronóstico disponible. Puedes continuar con la orden.")
        val hora = pronostico.horas.firstOrNull { it.instante == solicitud.hora }
        return ResultadoClima(hora, pronostico.actualizado, cache,
            if (hora == null) "No hay pronóstico para esa fecha y hora. Puedes continuar con la orden." else "")
    }
    fun contexto(local: Local, fecha: String): ContextoClima {
        val lat = local.latitud
        val lon = local.longitud
        if (lat == null || lon == null || !ValidadorCatastro.coordenadasValidas(lat, lon))
            return ContextoClima(local.nombre, null, "Agrega la ubicación del local en Catálogo > Locales para consultar el clima.")
        val instante = TiempoVisita.fecha(fecha)
            ?: return ContextoClima(local.nombre, null, "Selecciona fecha y hora para consultar el clima.")
        return ContextoClima(local.nombre, SolicitudClima(lat, lon, instante))
    }
    fun orden(orden: OrdenTrabajo, catastro: Catastro): ContextoClima? {
        val equipo = catastro.equipos.firstOrNull { it.id == orden.equipoId } ?: return null
        if (!equipo.esExterior || orden.estado == EstadoOt.CERRADA) return null
        val local = catastro.locales.firstOrNull { it.id == equipo.localId } ?: return null
        return contexto(local, orden.fechaProgramada)
    }
    fun formulario(localNombre: String, equipoNombre: String, fecha: String, catastro: Catastro): ContextoClima? {
        val local = catastro.locales.firstOrNull { it.nombre.equals(localNombre, true) } ?: return null
        val equipo = catastro.equipos.firstOrNull { it.localId == local.id && it.nombre.equals(equipoNombre, true) } ?: return null
        return if (equipo.esExterior) contexto(local, fecha) else null
    }
}
