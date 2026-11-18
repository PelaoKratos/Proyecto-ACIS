package cl.acis.usuarios.autenticacion.modelo;

import java.time.Instant;
import java.util.UUID;

public record Recuperacion(UUID id, UUID usuarioId, Instant venceEn) { }
