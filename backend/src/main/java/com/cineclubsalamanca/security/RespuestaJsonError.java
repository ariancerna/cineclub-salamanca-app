package com.cineclubsalamanca.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Escribe los errores de los filtros de seguridad con el mismo formato JSON que
 * GlobalExceptionHandler. Estos rechazos ocurren antes de llegar a un controlador, así que
 * el @RestControllerAdvice no puede darles forma.
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
