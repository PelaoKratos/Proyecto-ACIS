package cl.acis.usuarios.usuario.servicio;

import cl.acis.comun.modelo.Rol;
import cl.acis.comun.web.FalloApi;
import cl.acis.usuarios.autenticacion.repositorio.AutenticacionRepository;
import cl.acis.usuarios.usuario.dto.*;
import cl.acis.usuarios.usuario.modelo.Usuario;
import cl.acis.usuarios.usuario.repositorio.UsuarioRepository;
import cl.acis.usuarios.usuario.validacion.ReglasCuenta;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final AutenticacionRepository sesiones;
    private final Clock reloj;
    private final PasswordEncoder claves;

    public UsuarioService(UsuarioRepository usuarios, AutenticacionRepository sesiones, Clock reloj, PasswordEncoder claves) {
        this.usuarios = usuarios; this.sesiones = sesiones; this.reloj = reloj; this.claves = claves;
    }
    @Transactional(readOnly = true)
    public UsuarioRespuesta perfil(UUID id) { return UsuarioRespuesta.desde(buscar(id)); }

    @Transactional
    public UsuarioRespuesta editar(UUID id, PerfilSolicitud datos) {
        buscar(id);
        if (!usuarios.nombre(id, datos.nombre().trim(), datos.version(), reloj.instant())) throw versionAntigua();
        return perfil(id);
    }

    @Transactional(readOnly = true)
    public PaginaUsuarios listar(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) throw new FalloApi(400, "PAGINA_INVALIDA", "Usa una página desde cero y un tamaño entre 1 y 100.");
        return new PaginaUsuarios(usuarios.listar(pagina, tamano).stream().map(UsuarioRespuesta::desde).toList(), pagina, tamano, usuarios.total());
    }

    @Transactional
    public UsuarioRespuesta permisos(UUID actorId, UUID id, PermisosSolicitud datos) {
        usuarios.bloquearAdministracion();
        Usuario actor = buscar(actorId);
        if (!actor.activo() || actor.rol() != Rol.ADMINISTRADOR) throw new FalloApi(403, "ACCESO_DENEGADO", "No tienes permiso para esta acción.");
        Usuario actual = buscar(id);
        if (actual.version() != datos.version()) throw versionAntigua();
        if (actual.rol() == datos.rol() && actual.activo() == datos.activo()) return UsuarioRespuesta.desde(actual);
        boolean quitaAdministrador = actual.rol() == Rol.ADMINISTRADOR && actual.activo()
            && (datos.rol() != Rol.ADMINISTRADOR || !datos.activo());
        if (quitaAdministrador && usuarios.administradoresActivos() <= 1)
            throw new FalloApi(409, "ULTIMO_ADMINISTRADOR", "Debe quedar al menos un administrador activo.");
        if (!usuarios.permisos(id, datos.rol(), datos.activo(), datos.version(), reloj.instant())) throw versionAntigua();
        sesiones.revocarUsuario(id);
        sesiones.eliminarRecuperaciones(id);
        return perfil(id);
    }

    @Transactional
    public void crearAdministradorInicial(String nombre, String correo, String clave) {
        ReglasCuenta.clave(clave);
        usuarios.bloquearAdministracion();
        if (usuarios.administradoresActivos() > 0) return;
        String normalizado = ReglasCuenta.correo(correo);
        if (usuarios.porCorreo(normalizado).isPresent())
            throw new IllegalStateException("La cuenta inicial ya existe; no se eleva su rol automáticamente.");
        usuarios.insertar(new Usuario(UUID.randomUUID(), nombre.trim(), normalizado, claves.encode(clave), Rol.ADMINISTRADOR, true, 0));
    }

    private Usuario buscar(UUID id) { return usuarios.porId(id).orElseThrow(() -> new FalloApi(404, "USUARIO_NO_ENCONTRADO", "El usuario no existe.")); }
    private static FalloApi versionAntigua() { return new FalloApi(409, "VERSION_ANTIGUA", "Los datos cambiaron. Actualiza antes de guardar."); }
}
