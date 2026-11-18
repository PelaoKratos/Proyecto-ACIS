package cl.acis.usuarios.seguridad.modelo;

import cl.acis.comun.modelo.Rol;
import java.util.UUID;

public record Identidad(UUID usuarioId, UUID sesionId, Rol rol) { }
