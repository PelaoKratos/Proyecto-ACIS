package cl.acis.usuarios.autenticacion;

import cl.acis.usuarios.soporte.ApiUsuariosTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class AutenticacionHttpTest extends ApiUsuariosTest {
    @Test void registraClienteConHashYSesionSinPublicarSecretos() throws Exception {
        var usuario = registrar("Persona@Example.test");
        assertEquals("persona@example.test", usuario.path("correo").asText());
        assertEquals("CLIENTE", usuario.path("rol").asText());
        assertFalse(usuario.has("hashClave"));
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuarios", String.class);
        assertNotEquals(CLAVE, hash);
        assertTrue(hash.startsWith("$2"));
        var sesion = login("PERSONA@example.test");
        mvc.perform(get("/api/v1/auth/sesion").header("Authorization", acceso(sesion)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuario.path("id").asText()));
        var guardada = jdbc.queryForMap("SELECT acceso_hash, renovacion_hash FROM sesiones");
        assertNotEquals(sesion.path("tokenAcceso").asText(), guardada.get("acceso_hash"));
        assertNotEquals(sesion.path("tokenRenovacion").asText(), guardada.get("renovacion_hash"));
    }
    @Test void registroNoAceptaUnRolPrivilegiado() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", "Persona", "correo", "persona@example.test", "clave", CLAVE, "rol", "ADMINISTRADOR"))))
            .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
    }
    @Test void correoDuplicadoNoCreaOtraCuenta() throws Exception {
        registrar("persona@example.test");
        mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", "Otra", "correo", "PERSONA@example.test", "clave", CLAVE))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORREO_OCUPADO"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
    }
    @Test void datosInvalidosMuestranCamposYNoCreanRegistros() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", " ", "correo", "invalido", "clave", "corta"))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nombre").exists())
            .andExpect(jsonPath("$.campos.correo").exists()).andExpect(jsonPath("$.campos.clave").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
    }
    @Test void unaClaveDeEspaciosNoSeConsideraValida() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", "Persona", "correo", "persona@example.test", "clave", "        "))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.clave").exists());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
    }
    @Test void claveUnicodeLargaSeRechazaSinTruncarla() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", "Persona", "correo", "persona@example.test", "clave", "á".repeat(40)))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.clave").exists());
    }
    @Test void credencialesErroneasYCuentaDesactivadaDevuelvenElMismoError() throws Exception {
        var usuario = registrar("persona@example.test");
        for (String email : new String[]{"persona@example.test", "noexiste@example.test"}) {
            mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("correo", email, "clave", "Clave-incorrecta"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }
        jdbc.update("UPDATE usuarios SET activo = false WHERE id = ?", UUID.fromString(usuario.path("id").asText()));
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("correo", "persona@example.test", "clave", CLAVE))))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }
    @Test void renuevaUnaSesionVencidaYRevocaSusTokensAnteriores() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        when(reloj.instant()).thenReturn(AHORA.plus(Duration.ofMinutes(16)));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion))).andExpect(status().isUnauthorized());
        var nueva = cuerpo(mvc.perform(post("/api/v1/auth/renovar").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("tokenRenovacion", sesion.path("tokenRenovacion").asText()))))
            .andExpect(status().isOk()).andReturn());
        assertNotEquals(sesion.path("tokenAcceso").asText(), nueva.path("tokenAcceso").asText());
        assertEquals(401, renovar(sesion.path("tokenRenovacion").asText()));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(nueva))).andExpect(status().isOk());
    }
    @Test void unaRenovacionCaducadaNoSeReutiliza() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        when(reloj.instant()).thenReturn(AHORA.plus(Duration.ofDays(8)));
        assertEquals(401, renovar(sesion.path("tokenRenovacion").asText()));
    }
    @Test void dosRenovacionesConcurrentesSoloProducenUnCambio() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            var a = hilos.submit(() -> renovar(sesion.path("tokenRenovacion").asText()));
            var b = hilos.submit(() -> renovar(sesion.path("tokenRenovacion").asText()));
            var estados = java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, estados.stream().filter(e -> e == 200).count());
            assertEquals(1, estados.stream().filter(e -> e == 401).count());
        } finally { hilos.shutdownNow(); }
    }
    @Test void cerrarSesionInvalidaAccesoYRenovacion() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", acceso(sesion))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion))).andExpect(status().isUnauthorized());
        assertEquals(401, renovar(sesion.path("tokenRenovacion").asText()));
    }
    @Test void editarPerfilControlaVersionYNoPermiteCambiarRol() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        mvc.perform(put("/api/v1/mi-perfil").header("Authorization", acceso(sesion)).contentType(APPLICATION_JSON)
            .content("{\"nombre\":\"Nombre nuevo\",\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/v1/mi-perfil").header("Authorization", acceso(sesion)).contentType(APPLICATION_JSON)
            .content("{\"nombre\":\"Anterior\",\"version\":0}"))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/mi-perfil").header("Authorization", acceso(sesion)).contentType(APPLICATION_JSON)
            .content("{\"nombre\":\"Nombre\",\"version\":1,\"rol\":\"ADMINISTRADOR\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion)))
            .andExpect(jsonPath("$.nombre").value("Nombre nuevo")).andExpect(jsonPath("$.rol").value("CLIENTE"));
    }
}
