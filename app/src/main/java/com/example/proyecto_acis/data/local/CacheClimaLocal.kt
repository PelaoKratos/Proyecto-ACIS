package com.example.proyecto_acis.data.local

import com.example.proyecto_acis.data.serialization.FormatoClima
import android.content.Context
import com.example.proyecto_acis.data.repository.CacheClima
import com.example.proyecto_acis.domain.service.Pronostico

class CacheClimaLocal(context: Context, nombre: String = "acis_clima") : CacheClima {
    private val preferencias = context.getSharedPreferences(nombre, Context.MODE_PRIVATE)
    override fun leer(clave: String): Pronostico? = runCatching {
        preferencias.getString(clave, null)?.let(FormatoClima::leer)
    }.getOrNull()
    override fun guardar(clave: String, pronostico: Pronostico) {
        preferencias.edit().putString(clave, FormatoClima.guardar(pronostico)).apply()
    }
}
