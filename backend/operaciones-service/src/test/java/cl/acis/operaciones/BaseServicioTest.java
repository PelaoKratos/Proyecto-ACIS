package cl.acis.operaciones;

import cl.acis.comun.web.EstadoServicio;
import cl.acis.comun.web.ErrorApi;
import com.fasterxml.jackson.databind.JsonNode;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.UUID;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BaseServicioTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void publicaSoloLaIdentidadDelServicio() {
        var respuesta = http.getForEntity("/api/v1/estado", EstadoServicio.class);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(new EstadoServicio("acis-operaciones", "v1"), respuesta.getBody());
    }

    @Test
    void readinessCompruebaLaBaseSinPublicarSuConfiguracion() {
        var respuesta = http.getForEntity("/actuator/health/readiness", JsonNode.class);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("UP", respuesta.getBody().path("status").asText());
        assertFalse(respuesta.getBody().has("components"));
    }

    @Test
    void lasRutasDeNegocioNoQuedanAbiertas() {
        var respuesta = http.getForEntity("/api/v1/clientes", ErrorApi.class);
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("SESION_REQUERIDA", respuesta.getBody().codigo());
        assertEquals("/api/v1/clientes", respuesta.getBody().ruta());
        assertNull(respuesta.getHeaders().getFirst("Set-Cookie"));
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/actuator/env", String.class).getStatusCode());
    }

    @Test
    void unaRutaPublicaDeLecturaNoPermiteEscrituras() {
        assertEquals(HttpStatus.UNAUTHORIZED,
            http.postForEntity("/api/v1/estado", "{}", String.class).getStatusCode());
    }


    @Test
    void laBaseRechazaUnAcuerdoSlaIncompleto() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO clientes (id, nombre, nombre_normalizado, horas_llegada) VALUES (?, ?, ?, ?)",
            UUID.randomUUID(), "Local de prueba", "local de prueba", 4));
    }

    @Test
    void laMigracionNoBorraClientesGuardados() {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO clientes (id, nombre, nombre_normalizado) VALUES (?, ?, ?)",
                id, "Cliente de prueba", "cliente de prueba");
            assertEquals(0, flyway.migrate().migrationsExecuted);
            assertEquals("Cliente de prueba", jdbc.queryForObject("SELECT nombre FROM clientes WHERE id = ?", String.class, id));
        } finally { jdbc.update("DELETE FROM clientes WHERE id = ?", id); }
    }

    @Test
    void cadaServicioTieneSuPropiaMigracion() {
        assertEquals(1, flyway.info().applied().length);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'clientes'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'usuarios'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM clientes", Integer.class));
    }
}
