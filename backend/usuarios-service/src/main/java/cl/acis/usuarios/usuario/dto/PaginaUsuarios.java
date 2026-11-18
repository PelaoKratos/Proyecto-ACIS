package cl.acis.usuarios.usuario.dto;

import java.util.List;

public record PaginaUsuarios(List<UsuarioRespuesta> contenido, int pagina, int tamano, long total) { }
