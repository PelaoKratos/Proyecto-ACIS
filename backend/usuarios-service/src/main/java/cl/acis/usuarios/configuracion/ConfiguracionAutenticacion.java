package cl.acis.usuarios.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;

@Configuration
public class ConfiguracionAutenticacion {
    @Bean public Clock reloj() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder claves() { return new BCryptPasswordEncoder(); }
}
