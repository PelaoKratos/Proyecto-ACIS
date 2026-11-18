package cl.acis.usuarios.usuario.validacion;

import cl.acis.comun.web.FalloApi;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public final class ReglasCuenta {
    private ReglasCuenta() { }

    public static String correo(String valor) { return valor.trim().toLowerCase(Locale.ROOT); }

    public static void clave(String valor) {
        if (valor == null || valor.isBlank() || valor.length() < 8 || valor.length() > 64 || valor.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new FalloApi(400, "DATOS_INVALIDOS", "Revisa la clave.",
                Map.of("clave", "Usa entre 8 y 64 caracteres; reduce la longitud si usas muchos caracteres especiales."));
        }
    }
}
