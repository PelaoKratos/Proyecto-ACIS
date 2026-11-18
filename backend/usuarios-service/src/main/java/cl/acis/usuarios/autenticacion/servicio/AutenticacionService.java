package cl.acis.usuarios.autenticacion.servicio;

import cl.acis.comun.modelo.Rol;
import cl.acis.comun.web.FalloApi;
import cl.acis.usuarios.autenticacion.dto.*;
import cl.acis.usuarios.autenticacion.modelo.Sesion;
import cl.acis.usuarios.autenticacion.repositorio.AutenticacionRepository;
import cl.acis.usuarios.seguridad.modelo.Identidad;
import cl.acis.usuarios.seguridad.tokens.GeneradorTokens;
import cl.acis.usuarios.usuario.dto.UsuarioRespuesta;
import cl.acis.usuarios.usuario.modelo.Usuario;
import cl.acis.usuarios.usuario.repositorio.UsuarioRepository;
import cl.acis.usuarios.usuario.validacion.ReglasCuenta;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AutenticacionService {
    private final UsuarioRepository usuarios;
    private final AutenticacionRepository sesiones;
    private final GeneradorTokens tokens;
    private final PasswordEncoder claves;
    private final Clock reloj;
    private final String hashComprobacion;

    public AutenticacionService(UsuarioRepository usuarios, AutenticacionRepository sesiones,
                                GeneradorTokens tokens, PasswordEncoder claves, Clock reloj) {
        this.usuarios = usuarios; this.sesiones = sesiones; this.tokens = tokens; this.claves = claves; this.reloj = reloj;
        hashComprobacion = claves.encode("acis-comprobacion-interna");
    }

    @Transactional
    public UsuarioRespuesta registrar(RegistroSolicitud datos) {
        ReglasCuenta.clave(datos.clave());
        Usuario nuevo = new Usuario(UUID.randomUUID(), datos.nombre().trim(), ReglasCuenta.correo(datos.correo()),
            claves.encode(datos.clave()), Rol.CLIENTE, true, 0);
        try { usuarios.insertar(nuevo); }
        catch (DataIntegrityViolationException error) {
            throw new FalloApi(409, "CORREO_OCUPADO", "Ese correo ya está registrado.");
        }
        return UsuarioRespuesta.desde(nuevo);
    }

    @Transactional
    public TokensRespuesta login(LoginSolicitud datos) {
        if (datos.clave().getBytes(StandardCharsets.UTF_8).length > 72) throw credencialesInvalidas();
        Usuario usuario = usuarios.porCorreo(ReglasCuenta.correo(datos.correo())).orElse(null);
        boolean coincide = claves.matches(datos.clave(), usuario == null ? hashComprobacion : usuario.hashClave());
        if (!coincide || usuario == null || !usuario.activo()) throw credencialesInvalidas();
        Instant ahora = reloj.instant();
        Sesion sesion = new Sesion(UUID.randomUUID(), usuario.id(), ahora.plus(Duration.ofMinutes(15)), ahora.plus(Duration.ofDays(7)));
        String acceso = tokens.nuevo(), renovacion = tokens.nuevo();
        sesiones.insertarSesion(sesion, tokens.hash(acceso), tokens.hash(renovacion));
        return respuesta(usuario, sesion, acceso, renovacion, ahora);
    }

    @Transactional(readOnly = true)
    public Identidad autenticar(String acceso) {
        if (!tokens.formatoValido(acceso)) throw sesionInvalida();
        Sesion sesion = sesiones.porAcceso(tokens.hash(acceso)).orElseThrow(AutenticacionService::sesionInvalida);
        if (!sesion.accesoVenceEn().isAfter(reloj.instant())) throw sesionInvalida();
        Usuario usuario = usuarios.porId(sesion.usuarioId()).filter(Usuario::activo).orElseThrow(AutenticacionService::sesionInvalida);
        return new Identidad(usuario.id(), sesion.id(), usuario.rol());
    }

    @Transactional
    public TokensRespuesta renovar(String renovacion) {
        if (!tokens.formatoValido(renovacion)) throw sesionInvalida();
        Sesion anterior = sesiones.bloquearRenovacion(tokens.hash(renovacion)).orElseThrow(AutenticacionService::sesionInvalida);
        Instant ahora = reloj.instant();
        if (!anterior.renovacionVenceEn().isAfter(ahora)) throw sesionInvalida();
        Usuario usuario = usuarios.porId(anterior.usuarioId()).filter(Usuario::activo).orElseThrow(AutenticacionService::sesionInvalida);
        Instant vence = ahora.plus(Duration.ofMinutes(15));
        if (vence.isAfter(anterior.renovacionVenceEn())) vence = anterior.renovacionVenceEn();
        Sesion nueva = new Sesion(anterior.id(), anterior.usuarioId(), vence, anterior.renovacionVenceEn());
        String acceso = tokens.nuevo(), refresco = tokens.nuevo();
        if (!sesiones.rotar(nueva, tokens.hash(acceso), tokens.hash(refresco), tokens.hash(renovacion))) throw sesionInvalida();
        return respuesta(usuario, nueva, acceso, refresco, ahora);
    }

    @Transactional
    public void cerrar(UUID sesionId) { sesiones.eliminarSesion(sesionId); }

    private TokensRespuesta respuesta(Usuario usuario, Sesion sesion, String acceso, String renovacion, Instant ahora) {
        return new TokensRespuesta(acceso, renovacion, Duration.between(ahora, sesion.accesoVenceEn()).toSeconds(), UsuarioRespuesta.desde(usuario));
    }
    private static FalloApi sesionInvalida() { return new FalloApi(401, "SESION_INVALIDA", "La sesión venció o no es válida."); }
    private static FalloApi credencialesInvalidas() { return new FalloApi(401, "CREDENCIALES_INVALIDAS", "Correo o clave incorrectos."); }
}
