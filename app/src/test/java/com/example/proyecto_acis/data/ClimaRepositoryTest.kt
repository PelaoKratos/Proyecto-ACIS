package com.example.proyecto_acis.data

import com.example.proyecto_acis.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ClimaRepositoryTest {
    private class Memoria : CacheClima {
        val datos = mutableMapOf<String, Pronostico>()
        override fun leer(clave: String) = datos[clave]
        override fun guardar(clave: String, pronostico: Pronostico) { datos[clave] = pronostico }
    }
    private val solicitud = SolicitudClima(-36.82, -73.05, 3_600_000L)
    private val hora = HoraClima(3_600_000L, 70, 15.0)

    @Test fun reutilizaConsultaVigenteYForzarLaActualiza() = runBlocking {
        val cache = Memoria()
        var peticiones = 0
        val fuente = object : FuenteClima {
            override suspend fun cargar(solicitud: SolicitudClima): Pronostico {
                peticiones++
                return Pronostico(1000, listOf(hora))
            }
        }
        val repo = ClimaRepository(fuente, cache) { 1001 }
        assertEquals(hora, repo.consultar(solicitud).hora)
        assertEquals(hora, repo.consultar(solicitud).hora)
        assertEquals(1, peticiones)
        repo.consultar(solicitud, forzar = true)
        assertEquals(2, peticiones)
    }
    @Test fun falloDeRedUsaRespaldoSinConfundirOtraUbicacion() = runBlocking {
        val cache = Memoria()
        cache.guardar(solicitud.claveLocal, Pronostico(1000, listOf(hora)))
        val fuente = object : FuenteClima {
            override suspend fun cargar(solicitud: SolicitudClima): Pronostico = error("Sin conexión")
        }
        val repo = ClimaRepository(fuente, cache) { ReglasClima.VIGENCIA + 2000 }
        val respaldo = repo.consultar(solicitud)
        assertTrue(respaldo.enCache)
        assertEquals(hora, respaldo.hora)
        assertNull(repo.consultar(solicitud.copy(latitud = -33.4)).hora)
        assertEquals(1000L, cache.leer(solicitud.claveLocal)?.actualizado)
    }
    @Test fun sinRedYSinCacheDevuelveAvisoEnLugarDeFallarLaOrden() = runBlocking {
        val repo = ClimaRepository(object : FuenteClima {
            override suspend fun cargar(solicitud: SolicitudClima): Pronostico = error("Sin conexión")
        }, Memoria()) { 2000 }
        val resultado = repo.consultar(solicitud)
        assertNull(resultado.hora)
        assertTrue(resultado.aviso.isNotBlank())
    }
    @Test fun cancelacionNoSeTrataComoErrorDeConexion() = runBlocking {
        val repo = ClimaRepository(object : FuenteClima {
            override suspend fun cargar(solicitud: SolicitudClima): Pronostico = throw CancellationException()
        }, Memoria())
        try {
            repo.consultar(solicitud)
            fail("Debía propagar la cancelación")
        } catch (_: CancellationException) { }
    }
}
