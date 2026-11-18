package cl.acis.usuarios.autenticacion.dto;

import jakarta.validation.constraints.*;

public record RegistroSolicitud(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Email @Size(max = 254) String correo,
    @NotBlank @Size(min = 8, max = 64, message = "Usa entre 8 y 64 caracteres.") String clave) {
    @Override public String toString() { return "RegistroSolicitud[datos reservados]"; }
 }
