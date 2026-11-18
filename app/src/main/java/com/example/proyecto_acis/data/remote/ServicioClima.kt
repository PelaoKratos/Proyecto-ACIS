package com.example.proyecto_acis.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ServicioClima {
    @GET("v1/forecast")
    suspend fun pronostico(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("hourly") variables: String = "precipitation_probability,wind_speed_10m",
        @Query("wind_speed_unit") unidadViento: String = "kmh",
        @Query("timeformat") formatoTiempo: String = "unixtime",
        @Query("timezone") zonaHoraria: String = "GMT",
        @Query("forecast_days") dias: Int = 16,
    ): RespuestaClima
}
