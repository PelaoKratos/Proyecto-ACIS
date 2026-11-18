package cl.acis.usuarios.notificaciones.modelo;

public record AvisoRecuperacion(String correo, String token) {
    @Override public String toString() { return "AvisoRecuperacion[datos reservados]"; }
}
