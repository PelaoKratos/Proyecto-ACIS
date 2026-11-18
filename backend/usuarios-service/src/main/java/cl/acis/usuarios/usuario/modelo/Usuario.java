package cl.acis.usuarios.usuario.modelo;

import cl.acis.comun.modelo.Rol;
import java.util.UUID;

public record Usuario(UUID id, String nombre, String correo, String hashClave, Rol rol, boolean activo, long version) {
    @Override public String toString() { return "Usuario[id=" + id + ", rol=" + rol + "]"; }
}
