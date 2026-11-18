package cl.acis.usuarios.autenticacion.dto;

import cl.acis.usuarios.usuario.dto.UsuarioRespuesta;

public record TokensRespuesta(String tokenAcceso, String tokenRenovacion, long venceEnSegundos, UsuarioRespuesta usuario) {
    @Override public String toString() { return "TokensRespuesta[datos reservados]"; }
}
