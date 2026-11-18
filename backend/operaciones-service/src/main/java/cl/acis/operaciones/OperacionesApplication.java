package cl.acis.operaciones;

import cl.acis.comun.web.ErroresApi;
import cl.acis.comun.web.SeguridadBase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({SeguridadBase.class, ErroresApi.class})
public class OperacionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperacionesApplication.class, args);
    }
}
