package cl.acis.usuarios.usuario.dto;

import jakarta.validation.constraints.*;

public record PerfilSolicitud(
    @NotBlank(message = "Ingresa tu nombre.") @Size(max = 120) String nombre,
    @NotNull @PositiveOrZero Long version) { }
