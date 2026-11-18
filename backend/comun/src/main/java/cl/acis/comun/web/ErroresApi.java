package cl.acis.comun.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ErroresApi {
    private static final Logger log = LoggerFactory.getLogger(ErroresApi.class);

    @ExceptionHandler(FalloApi.class)
    public ResponseEntity<ErrorApi> negocio(FalloApi error, HttpServletRequest peticion) {
        return ResponseEntity.status(error.estado()).body(ErrorApi.crear(
            error.codigo(), error.getMessage(), error.campos(), peticion.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> parametro(HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorApi.crear(
            "PARAMETRO_INVALIDO", "Revisa los parámetros de la petición.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validar(MethodArgumentNotValidException error, HttpServletRequest peticion) {
        Map<String, String> campos = new TreeMap<>();
        error.getBindingResult().getFieldErrors().forEach(campo ->
            campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorApi.crear(
            "DATOS_INVALIDOS", "Revisa los campos indicados.", campos, peticion.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> formato(HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(ErrorApi.crear(
            "FORMATO_INVALIDO", "El formato de la petición no es válido.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorApi> noEncontrado(HttpServletRequest peticion) {
        return ResponseEntity.status(404).body(ErrorApi.crear(
            "NO_ENCONTRADO", "El recurso no existe.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorApi> contenido(HttpServletRequest peticion) {
        return ResponseEntity.status(415).body(ErrorApi.crear(
            "TIPO_NO_ADMITIDO", "Envía los datos como JSON.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodo(HttpRequestMethodNotSupportedException error, HttpServletRequest peticion) {
        String permitidos = String.join(", ", error.getSupportedMethods() == null ? new String[0] : error.getSupportedMethods());
        return ResponseEntity.status(405).header(HttpHeaders.ALLOW, permitidos).body(ErrorApi.crear(
            "METODO_NO_ADMITIDO", "La acción solicitada no está disponible.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> sinPermiso(HttpServletRequest peticion) {
        return ResponseEntity.status(403).body(ErrorApi.crear(
            "ACCESO_DENEGADO", "No tienes permiso para esta acción.", Map.of(), peticion.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception error, HttpServletRequest peticion) {
        log.error("Error interno en {}", peticion.getRequestURI(), error);
        return ResponseEntity.internalServerError().body(ErrorApi.crear(
            "ERROR_INTERNO", "No se pudo completar la operación.", Map.of(), peticion.getRequestURI()));
    }
}
