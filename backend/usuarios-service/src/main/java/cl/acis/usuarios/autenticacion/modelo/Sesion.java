package cl.acis.usuarios.autenticacion.modelo;

import java.time.Instant;
import java.util.UUID;

public record Sesion(UUID id, UUID usuarioId, Instant accesoVenceEn, Instant renovacionVenceEn) { }
