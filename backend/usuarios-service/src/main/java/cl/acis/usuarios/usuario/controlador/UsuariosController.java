package cl.acis.usuarios.usuario.controlador;

import cl.acis.usuarios.seguridad.modelo.Identidad;
import cl.acis.usuarios.usuario.dto.*;
import cl.acis.usuarios.usuario.servicio.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuariosController {
    private final UsuarioService usuarios;
    public UsuariosController(UsuarioService usuarios) { this.usuarios = usuarios; }
    @GetMapping
    public PaginaUsuarios listar(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return usuarios.listar(pagina, tamano);
    }
    @PutMapping("/{id}/permisos")
    public UsuarioRespuesta permisos(@AuthenticationPrincipal Identidad identidad, @PathVariable UUID id,
                                     @Valid @RequestBody PermisosSolicitud datos) {
        return usuarios.permisos(identidad.usuarioId(), id, datos);
    }
}
