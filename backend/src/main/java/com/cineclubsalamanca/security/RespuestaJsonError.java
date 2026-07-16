package com.cineclubsalamanca.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Escribe errores de la cadena de filtros de seguridad con el mismo formato JSON que
 * {@link com.cineclubsalamanca.config.GlobalExceptionHandler}.
 *
 * <p>Los rechazos de {@link EntradaNoAutenticada} y {@link SinPermisos} ocurren en los
 * filtros, antes de que la petición llegue a un controlador, por lo que el
 * {@code @RestControllerAdvice} no puede darles forma. Esta clase evita que el cliente
 * reciba dos formatos de error distintos según dónde se rechace la petición.</p>
 */
final class RespuestaJsonError {

    private RespuestaJsonError() {
    }

    static void escribir(ObjectMapper mapper, HttpServletResponse response,
                         HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", mensaje
        ));
    }
}
