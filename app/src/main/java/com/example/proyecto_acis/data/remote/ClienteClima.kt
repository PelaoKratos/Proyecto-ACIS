package com.example.proyecto_acis.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object ClienteClima {
    private val cliente by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(16, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    fun crearServicio(
        urlBase: String = "https://api.open-meteo.com/",
        clienteHttp: OkHttpClient = cliente,
    ): ServicioClima = Retrofit.Builder()
        .baseUrl(urlBase)
        .client(clienteHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ServicioClima::class.java)
}
