package cl.acis.usuarios.usuario.repositorio;

import cl.acis.comun.modelo.Rol;
import cl.acis.usuarios.usuario.modelo.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<Usuario> MAPA = (rs, fila) -> new Usuario(
        rs.getObject("id", UUID.class), rs.getString("nombre"), rs.getString("correo"),
        rs.getString("password_hash"), Rol.valueOf(rs.getString("rol")), rs.getBoolean("activo"), rs.getLong("version"));

    public UsuarioRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<Usuario> porId(UUID id) { return jdbc.query("SELECT * FROM usuarios WHERE id = ?", MAPA, id).stream().findFirst(); }
    public Optional<Usuario> porCorreo(String correo) { return jdbc.query("SELECT * FROM usuarios WHERE correo = ?", MAPA, correo).stream().findFirst(); }
    public Optional<Usuario> bloquear(UUID id) { return jdbc.query("SELECT * FROM usuarios WHERE id = ? FOR UPDATE", MAPA, id).stream().findFirst(); }
    public void bloquearAdministracion() { jdbc.queryForObject("SELECT id FROM control_administracion WHERE id = 1 FOR UPDATE", Integer.class); }
    public long administradoresActivos() { return jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE rol = 'ADMINISTRADOR' AND activo = true", Long.class); }
    public long total() { return jdbc.queryForObject("SELECT count(*) FROM usuarios", Long.class); }
    public List<Usuario> listar(int pagina, int tamano) {
        return jdbc.query("SELECT * FROM usuarios ORDER BY correo, id LIMIT ? OFFSET ?", MAPA, tamano, (long) pagina * tamano);
    }
    public void insertar(Usuario usuario) {
        jdbc.update("INSERT INTO usuarios (id,nombre,correo,password_hash,rol,activo) VALUES (?,?,?,?,?,?)",
            usuario.id(), usuario.nombre(), usuario.correo(), usuario.hashClave(), usuario.rol().name(), usuario.activo());
    }
    public boolean nombre(UUID id, String nombre, long version, Instant ahora) {
        return jdbc.update("UPDATE usuarios SET nombre = ?, version = version + 1, actualizado_en = ? WHERE id = ? AND version = ?",
            nombre, ahora.atOffset(ZoneOffset.UTC), id, version) == 1;
    }
    public boolean permisos(UUID id, Rol rol, boolean activo, long version, Instant ahora) {
        return jdbc.update("UPDATE usuarios SET rol = ?, activo = ?, version = version + 1, actualizado_en = ? WHERE id = ? AND version = ?",
            rol.name(), activo, ahora.atOffset(ZoneOffset.UTC), id, version) == 1;
    }
    public void clave(UUID id, String hash, Instant ahora) {
        jdbc.update("UPDATE usuarios SET password_hash = ?, version = version + 1, actualizado_en = ? WHERE id = ?", hash, ahora.atOffset(ZoneOffset.UTC), id);
    }
}
