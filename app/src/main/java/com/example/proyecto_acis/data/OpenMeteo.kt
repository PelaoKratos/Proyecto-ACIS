package com.example.proyecto_acis.data

import android.content.Context
import com.example.proyecto_acis.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object FormatoClima {
    fun leer(texto: String): Pronostico {
        val raiz = JSONObject(texto)
        val horas = raiz.getJSONArray("horas")
        return Pronostico(raiz.getLong("actualizado"), buildList {
            for (i in 0 until horas.length()) {
                val fila = horas.getJSONObject(i)
                add(HoraClima(fila.getLong("instante"), fila.getInt("lluvia"), fila.getDouble("viento")))
            }
        })
    }
    fun guardar(pronostico: Pronostico): String = JSONObject().apply {
        put("actualizado", pronostico.actualizado)
        put("horas", JSONArray().apply { pronostico.horas.forEach { hora ->
            put(JSONObject().apply { put("instante", hora.instante); put("lluvia", hora.lluvia); put("viento", hora.viento) })
        } })
    }.toString()

    fun respuesta(texto: String, ahora: Long): Pronostico {
        val hourly = JSONObject(texto).getJSONObject("hourly")
        val tiempos = hourly.getJSONArray("time")
        val lluvias = hourly.getJSONArray("precipitation_probability")
        val vientos = hourly.getJSONArray("wind_speed_10m")
        require(tiempos.length() == lluvias.length() && tiempos.length() == vientos.length())
        val horas = buildList {
            for (i in 0 until tiempos.length()) {
                if (tiempos.isNull(i) || lluvias.isNull(i) || vientos.isNull(i)) continue
                val lluvia = lluvias.getInt(i)
                val viento = vientos.getDouble(i)
                if (lluvia !in 0..100 || !viento.isFinite() || viento < 0) continue
                add(HoraClima(tiempos.getLong(i) * 1000L, lluvia, viento))
            }
        }
        require(horas.isNotEmpty())
        return Pronostico(ahora, horas)
    }
}

class CacheClimaLocal(context: Context, nombre: String = "acis_clima") : CacheClima {
    private val preferencias = context.getSharedPreferences(nombre, Context.MODE_PRIVATE)
    override fun leer(clave: String): Pronostico? = runCatching {
        preferencias.getString(clave, null)?.let(FormatoClima::leer)
    }.getOrNull()
    override fun guardar(clave: String, pronostico: Pronostico) {
        preferencias.edit().putString(clave, FormatoClima.guardar(pronostico)).apply()
    }
}

class OpenMeteo : FuenteClima {
    override suspend fun cargar(solicitud: SolicitudClima): Pronostico = withContext(Dispatchers.IO) {
        require(ValidadorCatastro.coordenadasValidas(solicitud.latitud, solicitud.longitud))
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${solicitud.latitud}&longitude=${solicitud.longitud}" +
            "&hourly=precipitation_probability,wind_speed_10m&wind_speed_unit=kmh&timeformat=unixtime&timezone=GMT&forecast_days=16"
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            check(conexion.responseCode == 200)
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            FormatoClima.respuesta(texto, System.currentTimeMillis())
        } finally { conexion.disconnect() }
    }
}
