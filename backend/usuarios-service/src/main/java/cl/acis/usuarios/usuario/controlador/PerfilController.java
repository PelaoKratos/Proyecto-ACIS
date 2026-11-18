package cl.acis.usuarios.usuario.controlador;

import cl.acis.usuarios.seguridad.modelo.Identidad;
import cl.acis.usuarios.usuario.dto.*;
import cl.acis.usuarios.usuario.servicio.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mi-perfil")
public class PerfilController {
    private final UsuarioService usuarios;
    public PerfilController(UsuarioService usuarios) { this.usuarios = usuarios; }
    @GetMapping
    public UsuarioRespuesta consultar(@AuthenticationPrincipal Identidad identidad) { return usuarios.perfil(identidad.usuarioId()); }
    @PutMapping
    public UsuarioRespuesta editar(@AuthenticationPrincipal Identidad identidad, @Valid @RequestBody PerfilSolicitud datos) {
        return usuarios.editar(identidad.usuarioId(), datos);
    }
}
