package cl.acis.usuarios.usuario.dto;

import cl.acis.comun.modelo.Rol;
import jakarta.validation.constraints.*;

public record PermisosSolicitud(@NotNull Rol rol, @NotNull Boolean activo, @NotNull @PositiveOrZero Long version) { }
