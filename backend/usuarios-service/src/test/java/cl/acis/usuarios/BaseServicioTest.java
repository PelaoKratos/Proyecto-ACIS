package cl.acis.usuarios;

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
        assertEquals(new EstadoServicio("acis-usuarios", "v1"), respuesta.getBody());
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
        var respuesta = http.getForEntity("/api/v1/usuarios", ErrorApi.class);
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("SESION_REQUERIDA", respuesta.getBody().codigo());
        assertEquals("/api/v1/usuarios", respuesta.getBody().ruta());
        assertNull(respuesta.getHeaders().getFirst("Set-Cookie"));
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/actuator/env", String.class).getStatusCode());
    }

    @Test
    void unaRutaPublicaDeLecturaNoPermiteEscrituras() {
        assertEquals(HttpStatus.UNAUTHORIZED,
            http.postForEntity("/api/v1/estado", "{}", String.class).getStatusCode());
    }


    @Test
    void laBaseRechazaUnRolDesconocido() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO usuarios (id, nombre, correo, password_hash, rol) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(), "Persona", "persona@example.test", "hash-solo-de-prueba", "SUPERVISOR"));
    }

    @Test
    void laMigracionNoBorraUsuariosGuardados() {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO usuarios (id, nombre, correo, password_hash, rol) VALUES (?, ?, ?, ?, ?)",
                id, "Persona", "persona@example.test", "hash-solo-de-prueba", "CLIENTE");
            assertEquals(0, flyway.migrate().migrationsExecuted);
            assertEquals("CLIENTE", jdbc.queryForObject("SELECT rol FROM usuarios WHERE id = ?", String.class, id));
        } finally { jdbc.update("DELETE FROM usuarios WHERE id = ?", id); }
    }

    @Test
    void cadaServicioTieneSuPropiaMigracion() {
        assertEquals(2, flyway.info().applied().length);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'usuarios'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'clientes'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
    }
}
