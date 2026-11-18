package cl.acis.usuarios.autenticacion.servicio;

import cl.acis.comun.web.FalloApi;
import cl.acis.usuarios.autenticacion.repositorio.AutenticacionRepository;
import cl.acis.usuarios.notificaciones.modelo.AvisoRecuperacion;
import cl.acis.usuarios.seguridad.tokens.GeneradorTokens;
import cl.acis.usuarios.usuario.repositorio.UsuarioRepository;
import cl.acis.usuarios.usuario.validacion.ReglasCuenta;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;

@Service
public class RecuperacionService {
    private final UsuarioRepository usuarios;
    private final AutenticacionRepository recuperaciones;
    private final GeneradorTokens tokens;
    private final PasswordEncoder claves;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public RecuperacionService(UsuarioRepository usuarios, AutenticacionRepository recuperaciones,
                               GeneradorTokens tokens, PasswordEncoder claves, Clock reloj, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios; this.recuperaciones = recuperaciones; this.tokens = tokens;
        this.claves = claves; this.reloj = reloj; this.eventos = eventos;
    }

    @Transactional
    public void solicitar(String correo) {
        var buscado = usuarios.porCorreo(ReglasCuenta.correo(correo));
        if (buscado.isEmpty()) return;
        var cuenta = usuarios.bloquear(buscado.get().id());
        if (cuenta.isEmpty() || !cuenta.get().activo()) return;
        var usuario = cuenta.get();
        String token = tokens.nuevo();
        recuperaciones.eliminarRecuperaciones(usuario.id());
        recuperaciones.insertarRecuperacion(usuario.id(), tokens.hash(token), reloj.instant().plus(Duration.ofMinutes(15)));
        eventos.publishEvent(new AvisoRecuperacion(usuario.correo(), token));
    }

    @Transactional
    public void restablecer(String token, String clave) {
        ReglasCuenta.clave(clave);
        if (!tokens.formatoValido(token)) throw codigoInvalido();
        String hash = tokens.hash(token);
        var buscada = recuperaciones.porRecuperacion(hash).orElseThrow(RecuperacionService::codigoInvalido);
        // Todos los cambios de recuperación bloquean primero la cuenta.
        var usuario = usuarios.bloquear(buscada.usuarioId()).orElseThrow(RecuperacionService::codigoInvalido);
        var actual = recuperaciones.bloquearRecuperacion(hash).orElseThrow(RecuperacionService::codigoInvalido);
        if (!usuario.activo() || !actual.venceEn().isAfter(reloj.instant())) throw codigoInvalido();
        usuarios.clave(usuario.id(), claves.encode(clave), reloj.instant());
        recuperaciones.revocarUsuario(usuario.id());
        recuperaciones.eliminarRecuperaciones(usuario.id());
    }

    private static FalloApi codigoInvalido() { return new FalloApi(400, "CODIGO_INVALIDO", "El código venció o no es válido."); }
}
