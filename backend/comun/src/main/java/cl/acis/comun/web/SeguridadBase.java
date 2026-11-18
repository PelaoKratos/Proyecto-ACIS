package cl.acis.comun.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class SeguridadBase {
    @Bean
    SecurityFilterChain filtros(HttpSecurity http, ObjectMapper json) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers(HttpMethod.GET, "/api/v1/estado", "/actuator/health", "/actuator/health/**").permitAll()
                // Las acciones de negocio se habilitan al implementar la autenticación.
                .anyRequest().denyAll())
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint((peticion, respuesta, error) -> responder(json, peticion, respuesta,
                    401, "SESION_REQUERIDA", "Inicia sesión para continuar."))
                .accessDeniedHandler((peticion, respuesta, error) -> responder(json, peticion, respuesta,
                    403, "ACCESO_DENEGADO", "No tienes permiso para esta acción.")))
            .build();
    }

    private void responder(ObjectMapper json, HttpServletRequest peticion, HttpServletResponse respuesta,
                           int estado, String codigo, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), ErrorApi.crear(codigo, mensaje, Map.of(), peticion.getRequestURI()));
    }
}
