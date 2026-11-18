package cl.acis.usuarios.configuracion;

import cl.acis.usuarios.autenticacion.dto.RegistroSolicitud;
import cl.acis.usuarios.usuario.servicio.UsuarioService;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final UsuarioService usuarios;
    private final Validator validator;
    private final String nombre, correo, clave;

    public AdministradorInicial(UsuarioService usuarios, Validator validator,
        @Value("${acis.administrador.nombre}") String nombre,
        @Value("${acis.administrador.correo}") String correo,
        @Value("${acis.administrador.clave}") String clave) {
        this.usuarios = usuarios; this.validator = validator; this.nombre = nombre; this.correo = correo; this.clave = clave;
    }

    @Override public void run(ApplicationArguments args) {
        if (correo.isBlank() && clave.isEmpty()) return;
        if (!validator.validate(new RegistroSolicitud(nombre, correo, clave)).isEmpty())
            throw new IllegalStateException("Revisa la configuración privada del administrador inicial.");
        usuarios.crearAdministradorInicial(nombre, correo, clave);
    }
}
