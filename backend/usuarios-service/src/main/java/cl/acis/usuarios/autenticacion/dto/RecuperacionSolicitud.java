package cl.acis.usuarios.autenticacion.dto;

import jakarta.validation.constraints.*;

public record RecuperacionSolicitud(
    @NotBlank @Email @Size(max = 254) String correo) { }
