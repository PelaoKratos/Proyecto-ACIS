package cl.acis.usuarios.usuario.dto;

import cl.acis.comun.modelo.Rol;
import cl.acis.usuarios.usuario.modelo.Usuario;
import java.util.UUID;

public record UsuarioRespuesta(UUID id, String nombre, String correo, Rol rol, boolean activo, long version) {
    public static UsuarioRespuesta desde(Usuario usuario) {
        return new UsuarioRespuesta(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol(), usuario.activo(), usuario.version());
    }
}
