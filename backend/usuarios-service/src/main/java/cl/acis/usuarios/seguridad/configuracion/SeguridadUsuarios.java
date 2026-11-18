package cl.acis.usuarios.seguridad.configuracion;

import cl.acis.comun.web.RespuestaSeguridad;
import cl.acis.usuarios.autenticacion.servicio.AutenticacionService;
import cl.acis.usuarios.seguridad.filtro.FiltroSesion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SeguridadUsuarios {
    @Bean
    SecurityFilterChain filtrosUsuarios(HttpSecurity http, AutenticacionService sesiones, ObjectMapper json) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers(HttpMethod.GET, "/api/v1/estado", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/registro", "/api/v1/auth/login", "/api/v1/auth/renovar",
                    "/api/v1/auth/recuperacion", "/api/v1/auth/restablecer").permitAll()
                .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/sesion", "/api/v1/mi-perfil").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/mi-perfil").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint((req, res, error) -> RespuestaSeguridad.escribir(json, req, res,
                    401, "SESION_REQUERIDA", "Inicia sesión para continuar."))
                .accessDeniedHandler((req, res, error) -> RespuestaSeguridad.escribir(json, req, res,
                    403, "ACCESO_DENEGADO", "No tienes permiso para esta acción.")))
            .addFilterBefore(new FiltroSesion(sesiones, json), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
