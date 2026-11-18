package cl.acis.comun.web;

import java.util.Map;

public class FalloApi extends RuntimeException {
    private final int estado;
    private final String codigo;
    private final Map<String, String> campos;

    public FalloApi(int estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, Map.of());
    }

    public FalloApi(int estado, String codigo, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.campos = Map.copyOf(campos);
    }

    public int estado() { return estado; }
    public String codigo() { return codigo; }
    public Map<String, String> campos() { return campos; }
}
