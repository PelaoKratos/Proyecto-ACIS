package cl.acis.usuarios.autenticacion.dto;

import jakarta.validation.constraints.*;

public record RestablecerSolicitud(
    @NotBlank @Size(max = 100) String token,
    @NotBlank @Size(min = 8, max = 64, message = "Usa entre 8 y 64 caracteres.") String clave) {
    @Override public String toString() { return "RestablecerSolicitud[datos reservados]"; }
 }
