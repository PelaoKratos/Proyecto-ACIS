package cl.acis.usuarios.autenticacion.repositorio;

import cl.acis.usuarios.autenticacion.modelo.Recuperacion;
import cl.acis.usuarios.autenticacion.modelo.Sesion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AutenticacionRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Sesion> SESION = (rs, fila) -> new Sesion(rs.getObject("id", UUID.class),
        rs.getObject("usuario_id", UUID.class), rs.getObject("acceso_vence_en", OffsetDateTime.class).toInstant(),
        rs.getObject("renovacion_vence_en", OffsetDateTime.class).toInstant());
    private static final RowMapper<Recuperacion> RECUPERACION = (rs, fila) -> new Recuperacion(rs.getObject("id", UUID.class),
        rs.getObject("usuario_id", UUID.class), rs.getObject("vence_en", OffsetDateTime.class).toInstant());

    public AutenticacionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void insertarSesion(Sesion sesion, String accesoHash, String renovacionHash) {
        jdbc.update("INSERT INTO sesiones (id,usuario_id,acceso_hash,renovacion_hash,acceso_vence_en,renovacion_vence_en) VALUES (?,?,?,?,?,?)",
            sesion.id(), sesion.usuarioId(), accesoHash, renovacionHash,
            sesion.accesoVenceEn().atOffset(ZoneOffset.UTC), sesion.renovacionVenceEn().atOffset(ZoneOffset.UTC));
    }
    public Optional<Sesion> porAcceso(String hash) {
        return jdbc.query("SELECT * FROM sesiones WHERE acceso_hash = ?", SESION, hash).stream().findFirst();
    }
    public Optional<Sesion> bloquearRenovacion(String hash) {
        return jdbc.query("SELECT * FROM sesiones WHERE renovacion_hash = ? FOR UPDATE", SESION, hash).stream().findFirst();
    }
    public boolean rotar(Sesion sesion, String accesoHash, String renovacionHash, String anteriorHash) {
        return jdbc.update("UPDATE sesiones SET acceso_hash = ?, renovacion_hash = ?, acceso_vence_en = ? WHERE id = ? AND renovacion_hash = ?",
            accesoHash, renovacionHash, sesion.accesoVenceEn().atOffset(ZoneOffset.UTC), sesion.id(), anteriorHash) == 1;
    }
    public void eliminarSesion(UUID id) { jdbc.update("DELETE FROM sesiones WHERE id = ?", id); }
    public void revocarUsuario(UUID id) { jdbc.update("DELETE FROM sesiones WHERE usuario_id = ?", id); }
    public void eliminarRecuperaciones(UUID id) { jdbc.update("DELETE FROM recuperaciones WHERE usuario_id = ?", id); }
    public void insertarRecuperacion(UUID usuarioId, String hash, Instant vence) {
        jdbc.update("INSERT INTO recuperaciones (id,usuario_id,token_hash,vence_en) VALUES (?,?,?,?)",
            UUID.randomUUID(), usuarioId, hash, vence.atOffset(ZoneOffset.UTC));
    }
    public Optional<Recuperacion> porRecuperacion(String hash) {
        return jdbc.query("SELECT * FROM recuperaciones WHERE token_hash = ?", RECUPERACION, hash).stream().findFirst();
    }
    public Optional<Recuperacion> bloquearRecuperacion(String hash) {
        return jdbc.query("SELECT * FROM recuperaciones WHERE token_hash = ? FOR UPDATE", RECUPERACION, hash).stream().findFirst();
    }
}
