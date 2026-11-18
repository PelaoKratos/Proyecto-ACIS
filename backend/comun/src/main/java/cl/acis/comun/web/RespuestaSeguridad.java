package cl.acis.comun.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class RespuestaSeguridad {
    private RespuestaSeguridad() { }
    public static void escribir(ObjectMapper json, HttpServletRequest peticion, HttpServletResponse respuesta,
                                 int estado, String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), ErrorApi.crear(codigo, mensaje, Map.of(), peticion.getRequestURI()));
    }
}
