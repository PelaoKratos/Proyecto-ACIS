package com.example.proyecto_acis.data.serialization

import com.example.proyecto_acis.domain.service.HoraClima
import com.example.proyecto_acis.domain.service.Pronostico
import org.json.JSONArray
import org.json.JSONObject

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

}
