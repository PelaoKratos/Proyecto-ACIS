package cl.acis.operaciones.web;

import cl.acis.comun.web.EstadoServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadoController {
    @GetMapping("/api/v1/estado")
    public EstadoServicio consultar() {
        return new EstadoServicio("acis-operaciones", "v1");
    }
}
