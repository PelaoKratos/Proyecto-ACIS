package cl.acis.comun.web;

import java.time.Instant;
import java.util.Map;

public record ErrorApi(Instant fecha, String codigo, String mensaje,
                       Map<String, String> campos, String ruta) {
    public static ErrorApi crear(String codigo, String mensaje, Map<String, String> campos, String ruta) {
        return new ErrorApi(Instant.now(), codigo, mensaje, campos, ruta);
    }
}
