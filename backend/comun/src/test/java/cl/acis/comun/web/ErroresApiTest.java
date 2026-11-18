package cl.acis.comun.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErroresApiTest {
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
            .setControllerAdvice(new ErroresApi()).build();
    }

    @Test
    void informaElCampoQueDebeCorregirse() throws Exception {
        mvc.perform(post("/prueba").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.campos.nombre").value("Ingresa el nombre."))
            .andExpect(jsonPath("$.ruta").value("/prueba"));
    }

    @Test
    void rechazaJsonMalformadoSinRepetirElContenidoRecibido() throws Exception {
        mvc.perform(post("/prueba").contentType(MediaType.APPLICATION_JSON).content("dato-privado"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"))
            .andExpect(content().string(not(containsString("dato-privado"))));
    }

    @Test
    void unErrorInternoNoDevuelveDetallesDelServidor() throws Exception {
        mvc.perform(get("/fallo"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
            .andExpect(content().string(not(containsString("detalle-interno"))));
    }

    @RestController
    static class ControladorPrueba {
        record Entrada(@NotBlank(message = "Ingresa el nombre.") String nombre) { }

        @PostMapping("/prueba")
        Entrada guardar(@Valid @RequestBody Entrada entrada) { return entrada; }

        @GetMapping("/fallo")
        void fallar() { throw new IllegalStateException("detalle-interno"); }

        @GetMapping("/prohibido")
        void prohibir() { throw new org.springframework.security.access.AccessDeniedException("detalle-privado"); }
    }

    @Test
    void unMetodoIncorrectoNoSeInformaComoFalloInterno() throws Exception {
        mvc.perform(get("/prueba"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.codigo").value("METODO_NO_ADMITIDO"));
    }

    @Test
    void unTipoDeContenidoIncorrectoNoSeInformaComoFalloInterno() throws Exception {
        mvc.perform(post("/prueba").contentType(MediaType.TEXT_PLAIN).content("dato-privado"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.codigo").value("TIPO_NO_ADMITIDO"));
    }

    @Test
    void unPermisoDenegadoNoSeInformaComoFalloInterno() throws Exception {
        mvc.perform(get("/prohibido"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
            .andExpect(content().string(not(containsString("detalle-privado"))));
    }
}
