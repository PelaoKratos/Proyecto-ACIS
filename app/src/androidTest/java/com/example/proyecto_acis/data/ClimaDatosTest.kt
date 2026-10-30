package com.example.proyecto_acis.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.proyecto_acis.domain.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ClimaDatosTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "clima_prueba_${UUID.randomUUID()}"
    @After fun limpiar() { context.deleteSharedPreferences(nombre) }

    @Test fun respuestaDescartaHorasIncompletasSinInventarBuenClima() {
        val texto = """{"hourly":{"time":[3600,7200,10800],"precipitation_probability":[null,65,10],"wind_speed_10m":[15,40,null]}}"""
        val pronostico = FormatoClima.respuesta(texto, 1000)
        assertEquals(listOf(HoraClima(7_200_000L, 65, 40.0)), pronostico.horas)
        assertEquals(1000L, pronostico.actualizado)
    }
    @Test fun cachePersisteYLosDatosCorruptosNoRompenLaConsulta() {
        val pronostico = Pronostico(1000, listOf(HoraClima(3_600_000L, 65, 15.0)))
        CacheClimaLocal(context, nombre).guardar("local", pronostico)
        assertEquals(pronostico, CacheClimaLocal(context, nombre).leer("local"))
        context.getSharedPreferences(nombre, Context.MODE_PRIVATE).edit().putString("local", "incompleto").commit()
        assertNull(CacheClimaLocal(context, nombre).leer("local"))
    }
    @Test fun respuestaSinDatosValidosSeRechaza() {
        val textos = listOf(
            """{"hourly":{"time":[3600],"precipitation_probability":[],"wind_speed_10m":[5]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[null],"wind_speed_10m":[5]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[101],"wind_speed_10m":[5]}}""")
        textos.forEach { texto ->
            assertTrue(runCatching { FormatoClima.respuesta(texto, 1000) }.isFailure)
        }
    }
}
