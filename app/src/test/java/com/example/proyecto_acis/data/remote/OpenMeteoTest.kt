package com.example.proyecto_acis.data.remote

import com.example.proyecto_acis.data.repository.CacheClima
import com.example.proyecto_acis.data.repository.ClimaRepository
import com.example.proyecto_acis.domain.service.HoraClima
import com.example.proyecto_acis.domain.service.Pronostico
import com.example.proyecto_acis.domain.service.ReglasClima
import com.example.proyecto_acis.domain.service.SolicitudClima
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenMeteoTest {
    private lateinit var servidor: MockWebServer
    private val solicitud = SolicitudClima(-36.82, -73.05, 3_600_000L)
    private val hora = HoraClima(3_600_000L, 65, 40.0)
    private val respuesta = """{"hourly":{"time":[3600],"precipitation_probability":[65],"wind_speed_10m":[40.0]}}"""

    private class Memoria(var pronostico: Pronostico? = null) : CacheClima {
        var guardados = 0
        override fun leer(clave: String) = pronostico
        override fun guardar(clave: String, pronostico: Pronostico) {
            this.pronostico = pronostico
            guardados++
        }
    }

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
    }

    @After fun cerrar() { servidor.shutdown() }

    private fun fuente(cliente: OkHttpClient = OkHttpClient()) = OpenMeteo(
        ClienteClima.crearServicio(servidor.url("/").toString(), cliente), reloj = { 1000L })

    @Test fun consultaMantieneCoordenadasUnidadesYHoraUtc() = runBlocking {
        servidor.enqueue(MockResponse().setBody(respuesta))
        val resultado = fuente().cargar(solicitud)
        assertEquals(Pronostico(1000L, listOf(hora)), resultado)
        val peticion = servidor.takeRequest(2, TimeUnit.SECONDS)!!
        assertEquals("GET", peticion.method)
        val url = peticion.requestUrl!!
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("-36.82", url.queryParameter("latitude"))
        assertEquals("-73.05", url.queryParameter("longitude"))
        assertEquals("precipitation_probability,wind_speed_10m", url.queryParameter("hourly"))
        assertEquals("kmh", url.queryParameter("wind_speed_unit"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("GMT", url.queryParameter("timezone"))
        assertEquals("16", url.queryParameter("forecast_days"))
    }

    @Test fun omiteHorasIncompletasSinInventarBuenClima() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"hourly":{"time":[3600,7200,10800],"precipitation_probability":[null,65,10],"wind_speed_10m":[15,40,null]}}"""))
        val resultado = fuente().cargar(solicitud)
        assertEquals(listOf(HoraClima(7_200_000L, 65, 40.0)), resultado.horas)
    }

    @Test fun rechazaRespuestasVaciasIncompletasOMalFormadas() = runBlocking {
        val textos = listOf(
            "{}", "null", "no es json", """{"hourly":{}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[],"wind_speed_10m":[5]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[null],"wind_speed_10m":[5]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[101],"wind_speed_10m":[5]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":[50],"wind_speed_10m":[-1]}}""",
            """{"hourly":{"time":[],"precipitation_probability":[],"wind_speed_10m":[]}}""",
            """{"hourly":{"time":[3600],"precipitation_probability":["incorrecto"],"wind_speed_10m":[5]}}""",
        )
        val fuente = fuente()
        textos.forEach { texto ->
            servidor.enqueue(MockResponse().setBody(texto))
            assertTrue("Se aceptó una respuesta inválida: $texto", runCatching { fuente.cargar(solicitud) }.isFailure)
        }
    }

    @Test fun errorHttpConservaElRespaldoYNoLoSobrescribe() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(503))
        val anterior = Pronostico(1000L, listOf(hora))
        val cache = Memoria(anterior)
        val repo = ClimaRepository(fuente(), cache) { ReglasClima.VIGENCIA + 2000L }
        val resultado = repo.consultar(solicitud)
        assertTrue(resultado.enCache)
        assertEquals(hora, resultado.hora)
        assertEquals(anterior, cache.pronostico)
        assertEquals(0, cache.guardados)
    }

    @Test fun errorHttpSinRespaldoDevuelveUnAviso() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(429))
        val cache = Memoria()
        val resultado = ClimaRepository(fuente(), cache) { 1000L }.consultar(solicitud)
        assertNull(resultado.hora)
        assertTrue(resultado.enCache)
        assertTrue(resultado.aviso.isNotBlank())
        assertEquals(0, cache.guardados)
    }

    @Test fun respuestaInvalidaNoReemplazaUnPronosticoGuardado() = runBlocking {
        servidor.enqueue(MockResponse().setBody("{}"))
        val anterior = Pronostico(1000L, listOf(hora))
        val cache = Memoria(anterior)
        val resultado = ClimaRepository(fuente(), cache) { 2000L }.consultar(solicitud, forzar = true)
        assertEquals(hora, resultado.hora)
        assertTrue(resultado.enCache)
        assertEquals(anterior, cache.pronostico)
        assertEquals(0, cache.guardados)
    }

    @Test fun esperaAgotadaPermiteUsarElRespaldo() = runBlocking {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val cliente = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS)
            .callTimeout(2, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
        val cache = Memoria(Pronostico(1000L, listOf(hora)))
        val resultado = ClimaRepository(fuente(cliente), cache) { 2000L }.consultar(solicitud, forzar = true)
        assertTrue(resultado.enCache)
        assertEquals(hora, resultado.hora)
        assertEquals(0, cache.guardados)
    }

    @Test fun cancelarLaConsultaCancelaLaPeticionYNoGuardaUnResultado() = runBlocking {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val terminada = CompletableDeferred<Unit>()
        val cliente = OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun callFailed(call: Call, ioe: IOException) { terminada.complete(Unit) }
        }).build()
        val cache = Memoria()
        val repo = ClimaRepository(fuente(cliente), cache) { 1000L }
        val consulta = async { repo.consultar(solicitud) }
        try {
            withContext(Dispatchers.IO) { assertNotNull(servidor.takeRequest(2, TimeUnit.SECONDS)) }
            consulta.cancelAndJoin()
            withTimeout(3000) { terminada.await() }
            assertTrue(consulta.isCancelled)
            assertEquals(0, cache.guardados)
            assertNull(cache.pronostico)
        } finally { consulta.cancelAndJoin() }
    }

    @Test fun coordenadasInvalidasNoGeneranUnaPeticion() = runBlocking {
        val fuente = fuente()
        listOf(solicitud.copy(latitud = 91.0), solicitud.copy(longitud = Double.NaN)).forEach {
            assertTrue(runCatching { fuente.cargar(it) }.isFailure)
        }
        assertEquals(0, servidor.requestCount)
    }
}
