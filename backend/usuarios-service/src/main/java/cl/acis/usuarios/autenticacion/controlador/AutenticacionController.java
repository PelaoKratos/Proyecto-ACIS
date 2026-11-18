package cl.acis.usuarios.autenticacion.controlador;

import cl.acis.usuarios.autenticacion.dto.*;
import cl.acis.usuarios.autenticacion.servicio.AutenticacionService;
import cl.acis.usuarios.autenticacion.servicio.RecuperacionService;
import cl.acis.usuarios.seguridad.modelo.Identidad;
import cl.acis.usuarios.usuario.dto.UsuarioRespuesta;
import cl.acis.usuarios.usuario.servicio.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacionController {
    private final AutenticacionService acceso;
    private final RecuperacionService recuperacion;
    private final UsuarioService usuarios;
    public AutenticacionController(AutenticacionService acceso, RecuperacionService recuperacion, UsuarioService usuarios) {
        this.acceso = acceso; this.recuperacion = recuperacion; this.usuarios = usuarios;
    }
    @PostMapping("/registro")
    public ResponseEntity<UsuarioRespuesta> registrar(@Valid @RequestBody RegistroSolicitud datos) {
        return ResponseEntity.status(201).body(acceso.registrar(datos));
    }
    @PostMapping("/login")
    public TokensRespuesta login(@Valid @RequestBody LoginSolicitud datos) { return acceso.login(datos); }
    @PostMapping("/renovar")
    public TokensRespuesta renovar(@Valid @RequestBody RenovacionSolicitud datos) { return acceso.renovar(datos.tokenRenovacion()); }
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrar(@AuthenticationPrincipal Identidad identidad) {
        acceso.cerrar(identidad.sesionId());
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/sesion")
    public UsuarioRespuesta sesion(@AuthenticationPrincipal Identidad identidad) { return usuarios.perfil(identidad.usuarioId()); }
    @PostMapping("/recuperacion")
    public MensajeRespuesta recuperar(@Valid @RequestBody RecuperacionSolicitud datos) {
        recuperacion.solicitar(datos.correo());
        return new MensajeRespuesta("Si la cuenta está registrada y activa, recibirás un código de recuperación.");
    }
    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody RestablecerSolicitud datos) {
        recuperacion.restablecer(datos.token(), datos.clave());
        return ResponseEntity.noContent().build();
    }
}
