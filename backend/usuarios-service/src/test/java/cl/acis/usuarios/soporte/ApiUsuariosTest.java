package cl.acis.usuarios.soporte;

import cl.acis.comun.modelo.Rol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.mockito.ArgumentCaptor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiUsuariosTest {
    protected static final String CLAVE = "Clave-de-prueba-2026";
    protected static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @MockitoBean protected JavaMailSender correo;
    @MockitoBean protected Clock reloj;

    @BeforeEach void prepararApi() {
        jdbc.update("DELETE FROM usuarios");
        reset(correo, reloj);
        when(reloj.instant()).thenReturn(AHORA);
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
    }
    @AfterEach void limpiarApi() { jdbc.update("DELETE FROM usuarios"); }

    protected JsonNode cuerpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    protected JsonNode registrar(String email) throws Exception {
        return cuerpo(mvc.perform(post("/api/v1/auth/registro").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("nombre", "Persona de prueba", "correo", email, "clave", CLAVE))))
            .andExpect(status().isCreated()).andReturn());
    }
    protected JsonNode login(String email) throws Exception {
        return cuerpo(mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("correo", email, "clave", CLAVE))))
            .andExpect(status().isOk()).andReturn());
    }
    protected JsonNode cuentaConRol(Rol rol, String email) throws Exception {
        var usuario = registrar(email);
        // Solo prepara el rol en la base aislada de pruebas.
        jdbc.update("UPDATE usuarios SET rol = ? WHERE id = ?", rol.name(), java.util.UUID.fromString(usuario.path("id").asText()));
        return login(email);
    }
    protected String acceso(JsonNode sesion) { return "Bearer " + sesion.path("tokenAcceso").asText(); }
    protected int renovar(String token) {
        try {
            return mvc.perform(post("/api/v1/auth/renovar").contentType(APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("tokenRenovacion", token)))).andReturn().getResponse().getStatus();
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    protected String codigoCorreo() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo, atLeastOnce()).send(captor.capture());
        var mensajes = captor.getAllValues();
        var matcher = Pattern.compile("Código de recuperación: ([A-Za-z0-9_-]{43})").matcher(mensajes.get(mensajes.size() - 1).getText());
        assertTrue(matcher.find());
        return matcher.group(1);
    }
    protected void solicitarRecuperacion(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/recuperacion").contentType(APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("correo", email)))).andExpect(status().isOk());
    }
}
