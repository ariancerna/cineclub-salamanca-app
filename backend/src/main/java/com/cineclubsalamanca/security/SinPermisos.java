package com.cineclubsalamanca.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve 403 cuando el usuario está autenticado pero le falta el rol requerido.
 *
 * @see EntradaNoAutenticada
 */
@Component
@RequiredArgsConstructor
public class SinPermisos implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RespuestaJsonError.escribir(objectMapper, response,
                HttpStatus.FORBIDDEN, "Acceso denegado");
    }
}
