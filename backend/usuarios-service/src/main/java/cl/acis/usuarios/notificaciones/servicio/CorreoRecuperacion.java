package cl.acis.usuarios.notificaciones.servicio;

import cl.acis.usuarios.notificaciones.modelo.AvisoRecuperacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CorreoRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacion.class);
    private final JavaMailSender correo;
    private final String remitente;

    public CorreoRecuperacion(JavaMailSender correo, @Value("${acis.correo.remitente}") String remitente) {
        this.correo = correo; this.remitente = remitente;
    }

    @TransactionalEventListener
    public void enviar(AvisoRecuperacion aviso) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(aviso.correo());
        mensaje.setSubject("Recuperación de acceso ACIS");
        mensaje.setText("Solicitaste cambiar tu clave de ACIS.\nCódigo de recuperación: " + aviso.token()
            + "\nVence en 15 minutos y solo puede usarse una vez. Si no lo solicitaste, ignora este correo.");
        try { correo.send(mensaje); }
        catch (org.springframework.mail.MailException error) {
            // No se publican el correo, el código ni la configuración SMTP en los logs.
            log.warn("No se pudo entregar un correo de recuperación. Revisa el servicio SMTP.");
        }
    }
}
