package cl.acis.usuarios.seguridad.filtro;

import cl.acis.comun.web.FalloApi;
import cl.acis.comun.web.RespuestaSeguridad;
import cl.acis.usuarios.autenticacion.servicio.AutenticacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class FiltroSesion extends OncePerRequestFilter {
    private final AutenticacionService sesiones;
    private final ObjectMapper json;
    public FiltroSesion(AutenticacionService sesiones, ObjectMapper json) { this.sesiones = sesiones; this.json = json; }

    @Override protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
        throws ServletException, IOException {
        String autorizacion = peticion.getHeader("Authorization");
        if (autorizacion != null) {
            try {
                if (!autorizacion.startsWith("Bearer ")) throw new FalloApi(401, "SESION_INVALIDA", "La sesión no es válida.");
                var identidad = sesiones.autenticar(autorizacion.substring(7));
                var contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(identidad, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + identidad.rol().name()))));
                SecurityContextHolder.setContext(contexto);
            } catch (FalloApi error) {
                RespuestaSeguridad.escribir(json, peticion, respuesta, error.estado(), error.codigo(), error.getMessage());
                return;
            } catch (DataAccessException error) {
                RespuestaSeguridad.escribir(json, peticion, respuesta, 503, "SESION_NO_DISPONIBLE", "No se pudo comprobar la sesión. Reintenta.");
                return;
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
