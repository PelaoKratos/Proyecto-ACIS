package cl.acis.usuarios.autenticacion.dto;

import jakarta.validation.constraints.*;

public record LoginSolicitud(
    @NotBlank @Email @Size(max = 254) String correo,
    @NotBlank @Size(max = 128) String clave) {
    @Override public String toString() { return "LoginSolicitud[datos reservados]"; }
 }
