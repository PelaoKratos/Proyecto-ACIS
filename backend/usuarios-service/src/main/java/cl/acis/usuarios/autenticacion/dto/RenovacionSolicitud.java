package cl.acis.usuarios.autenticacion.dto;

import jakarta.validation.constraints.*;

public record RenovacionSolicitud(
    @NotBlank @Size(max = 100) String tokenRenovacion) {
    @Override public String toString() { return "RenovacionSolicitud[datos reservados]"; }
 }
