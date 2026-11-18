package com.example.proyecto_acis.data.remote

import com.example.proyecto_acis.data.local.CacheClimaLocal
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.proyecto_acis.domain.service.HoraClima
import com.example.proyecto_acis.domain.service.Pronostico
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClimaDatosTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "clima_prueba_${UUID.randomUUID()}"
    @After fun limpiar() { context.deleteSharedPreferences(nombre) }

    @Test fun cachePersisteYLosDatosCorruptosNoRompenLaConsulta() {
        val pronostico = Pronostico(1000, listOf(HoraClima(3_600_000L, 65, 15.0)))
        CacheClimaLocal(context, nombre).guardar("local", pronostico)
        assertEquals(pronostico, CacheClimaLocal(context, nombre).leer("local"))
        context.getSharedPreferences(nombre, Context.MODE_PRIVATE).edit().putString("local", "incompleto").commit()
        assertNull(CacheClimaLocal(context, nombre).leer("local"))
    }
}
