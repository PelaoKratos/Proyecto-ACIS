package cl.acis.usuarios.autenticacion;

import cl.acis.usuarios.soporte.ApiUsuariosTest;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class RecuperacionHttpTest extends ApiUsuariosTest {
    private int restablecer(String token, String clave) {
        try {
            return mvc.perform(post("/api/v1/auth/restablecer").contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("token", token, "clave", clave))))
                .andReturn().getResponse().getStatus();
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    @Test void respuestaPublicaNoRevelaSiExisteElCorreo() throws Exception {
        registrar("persona@example.test");
        var existente = mvc.perform(post("/api/v1/auth/recuperacion").contentType(APPLICATION_JSON)
            .content("{\"correo\":\"persona@example.test\"}")).andExpect(status().isOk()).andReturn();
        var ausente = mvc.perform(post("/api/v1/auth/recuperacion").contentType(APPLICATION_JSON)
            .content("{\"correo\":\"ausente@example.test\"}")).andExpect(status().isOk()).andReturn();
        assertEquals(cuerpo(existente), cuerpo(ausente));
        assertFalse(cuerpo(existente).has("token"));
        verify(correo, times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void elCodigoLlegaPorCorreoYSeGuardaComoHash() throws Exception {
        registrar("persona@example.test");solicitarRecuperacion("persona@example.test");
        String token = codigoCorreo();
        String hash = jdbc.queryForObject("SELECT token_hash FROM recuperaciones", String.class);
        assertEquals(64, hash.length());assertNotEquals(token, hash);
    }
    @Test void restablecerConsumeElCodigoYRevocaTodasLasSesiones() throws Exception {
        registrar("persona@example.test");var sesion = login("persona@example.test");
        solicitarRecuperacion("persona@example.test");String token = codigoCorreo();
        assertEquals(204, restablecer(token, "Otra-clave-2026"));
        assertEquals(400, restablecer(token, "Tercera-clave-2026"));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion))).andExpect(status().isUnauthorized());
        assertEquals(401, renovar(sesion.path("tokenRenovacion").asText()));
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("correo", "persona@example.test", "clave", CLAVE))))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("correo", "persona@example.test", "clave", "Otra-clave-2026"))))
            .andExpect(status().isOk());
    }
    @Test void unaNuevaSolicitudInvalidaElCodigoAnterior() throws Exception {
        registrar("persona@example.test");solicitarRecuperacion("persona@example.test");String anterior = codigoCorreo();
        solicitarRecuperacion("persona@example.test");String nuevo = codigoCorreo();
        assertNotEquals(anterior, nuevo);
        assertEquals(400, restablecer(anterior, "Otra-clave-2026"));
        assertEquals(204, restablecer(nuevo, "Otra-clave-2026"));
    }
    @Test void noAceptaUnCodigoCaducado() throws Exception {
        registrar("persona@example.test");solicitarRecuperacion("persona@example.test");String token = codigoCorreo();
        when(reloj.instant()).thenReturn(AHORA.plus(Duration.ofMinutes(16)));
        assertEquals(400, restablecer(token, "Otra-clave-2026"));
    }
    @Test void unaClaveInvalidaNoConsumeElCodigo() throws Exception {
        registrar("persona@example.test");solicitarRecuperacion("persona@example.test");String token = codigoCorreo();
        assertEquals(400, restablecer(token, "corta"));
        assertEquals(204, restablecer(token, "Otra-clave-2026"));
    }
    @Test void dosRestablecimientosConcurrentesNoReutilizanElCodigo() throws Exception {
        registrar("persona@example.test");solicitarRecuperacion("persona@example.test");String token = codigoCorreo();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            var a = hilos.submit(() -> restablecer(token, "Otra-clave-2026"));
            var b = hilos.submit(() -> restablecer(token, "Tercera-clave-2026"));
            var estados = java.util.List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, estados.stream().filter(e -> e == 204).count());
            assertEquals(1, estados.stream().filter(e -> e == 400).count());
        } finally { hilos.shutdownNow(); }
    }
    @Test void unFalloSmtpNoPublicaElCorreoNiUnErrorDiferente() throws Exception {
        registrar("persona@example.test");
        doThrow(new MailSendException("SMTP no disponible")).when(correo).send(any(SimpleMailMessage.class));
        solicitarRecuperacion("persona@example.test");
        solicitarRecuperacion("ausente@example.test");
    }
}
