package com.example.proyecto_acis.data.remote

import com.example.proyecto_acis.domain.service.HoraClima
import com.example.proyecto_acis.domain.service.Pronostico
import com.google.gson.annotations.SerializedName

data class RespuestaClima(
    @SerializedName("hourly") val porHora: HorasClima? = null,
)

data class HorasClima(
    @SerializedName("time") val tiempos: List<Long?>? = null,
    @SerializedName("precipitation_probability") val lluvias: List<Int?>? = null,
    @SerializedName("wind_speed_10m") val vientos: List<Double?>? = null,
)

internal fun RespuestaClima.aPronostico(ahora: Long): Pronostico {
    val datos = requireNotNull(porHora) { "La respuesta no contiene datos por hora." }
    val tiempos = requireNotNull(datos.tiempos)
    val lluvias = requireNotNull(datos.lluvias)
    val vientos = requireNotNull(datos.vientos)
    require(tiempos.size == lluvias.size && tiempos.size == vientos.size)

    val horas = buildList {
        tiempos.indices.forEach { i ->
            val segundos = tiempos[i] ?: return@forEach
            val lluvia = lluvias[i] ?: return@forEach
            val viento = vientos[i] ?: return@forEach
            if (lluvia !in 0..100 || !viento.isFinite() || viento < 0) return@forEach
            if (segundos !in 0..Long.MAX_VALUE / 1000L) return@forEach
            add(HoraClima(segundos * 1000L, lluvia, viento))
        }
    }
    require(horas.isNotEmpty()) { "No hay horas válidas en el pronóstico." }
    return Pronostico(ahora, horas)
}
