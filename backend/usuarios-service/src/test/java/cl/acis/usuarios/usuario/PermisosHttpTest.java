package cl.acis.usuarios.usuario;

import cl.acis.comun.modelo.Rol;
import cl.acis.usuarios.soporte.ApiUsuariosTest;
import cl.acis.usuarios.usuario.servicio.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class PermisosHttpTest extends ApiUsuariosTest {
    @Autowired UsuarioService usuarios;

    @ParameterizedTest @EnumSource(Rol.class)
    void soloElAdministradorPuedeListarCuentas(Rol rol) throws Exception {
        var sesion = cuentaConRol(rol, "persona@example.test");
        mvc.perform(get("/api/v1/usuarios").header("Authorization", acceso(sesion)))
            .andExpect(status().is(rol == Rol.ADMINISTRADOR ? 200 : 403));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value(rol.name()))
            .andExpect(jsonPath("$.hashClave").doesNotExist());
    }
    @Test void cambiarRolRevocaLaSesionYRequiereUnaVersionActual() throws Exception {
        var admin = cuentaConRol(Rol.ADMINISTRADOR, "admin@example.test");
        var persona = registrar("persona@example.test");var sesion = login("persona@example.test");
        String ruta = "/api/v1/usuarios/" + persona.path("id").asText() + "/permisos";
        mvc.perform(put(ruta).header("Authorization", acceso(admin)).contentType(APPLICATION_JSON)
            .content("{\"rol\":\"TECNICO\",\"activo\":true,\"version\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion))).andExpect(status().isUnauthorized());
        assertEquals(401, renovar(sesion.path("tokenRenovacion").asText()));
        assertEquals("TECNICO", login("persona@example.test").path("usuario").path("rol").asText());
        mvc.perform(put(ruta).header("Authorization", acceso(admin)).contentType(APPLICATION_JSON)
            .content("{\"rol\":\"CLIENTE\",\"activo\":true,\"version\":0}"))
            .andExpect(status().isConflict());
    }
    @Test void noPermiteDesactivarAlUltimoAdministrador() throws Exception {
        var admin = cuentaConRol(Rol.ADMINISTRADOR, "admin@example.test");
        String ruta = "/api/v1/usuarios/" + admin.path("usuario").path("id").asText() + "/permisos";
        mvc.perform(put(ruta).header("Authorization", acceso(admin)).contentType(APPLICATION_JSON)
            .content("{\"rol\":\"CLIENTE\",\"activo\":true,\"version\":0}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ULTIMO_ADMINISTRADOR"));
    }
    @Test void desactivarUnaCuentaImpideSuAccesoYSuRecuperacion() throws Exception {
        var admin = cuentaConRol(Rol.ADMINISTRADOR, "admin@example.test");
        var persona = registrar("persona@example.test");var sesion = login("persona@example.test");
        solicitarRecuperacion("persona@example.test");
        mvc.perform(put("/api/v1/usuarios/" + persona.path("id").asText() + "/permisos")
            .header("Authorization", acceso(admin)).contentType(APPLICATION_JSON)
            .content("{\"rol\":\"CLIENTE\",\"activo\":false,\"version\":0}"))
            .andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperaciones", Integer.class));
        mvc.perform(get("/api/v1/mi-perfil").header("Authorization", acceso(sesion))).andExpect(status().isUnauthorized());
        solicitarRecuperacion("persona@example.test");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperaciones", Integer.class));
    }
    @Test void crearElAdministradorInicialNoPromueveUnClienteExistente() throws Exception {
        registrar("persona@example.test");
        assertThrows(IllegalStateException.class, () -> usuarios.crearAdministradorInicial("Admin", "persona@example.test", CLAVE));
        assertEquals("CLIENTE", jdbc.queryForObject("SELECT rol FROM usuarios", String.class));
    }
    @Test void crearElAdministradorInicialEsRepetibleSinCambiarSuClave() throws Exception {
        usuarios.crearAdministradorInicial("Admin", "admin@example.test", CLAVE);
        usuarios.crearAdministradorInicial("Otro", "otro@example.test", "Otra-clave-2026");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
        assertEquals("ADMINISTRADOR", login("admin@example.test").path("usuario").path("rol").asText());
    }
    @Test void borrarUnaCuentaEliminaSusSesionesYCodigosRelacionados() throws Exception {
        registrar("persona@example.test");login("persona@example.test");solicitarRecuperacion("persona@example.test");
        jdbc.update("DELETE FROM usuarios");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM sesiones", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperaciones", Integer.class));
    }
}
