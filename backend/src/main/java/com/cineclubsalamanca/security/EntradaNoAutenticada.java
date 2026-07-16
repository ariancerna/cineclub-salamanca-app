package com.cineclubsalamanca.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Devuelve 401 cuando la petición no está autenticada o el token no es válido.
 *
 * <p>Sin esto, Spring Security aplica Http403ForbiddenEntryPoint y responde 403 también al
 * usuario anónimo, con lo que el cliente no puede saber si debe volver a iniciar sesión.</p>
 */
@Component
@RequiredArgsConstructor
public class EntradaNoAutenticada implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Mensaje genérico: no debe revelar si el correo existe
        RespuestaJsonError.escribir(objectMapper, response,
                HttpStatus.UNAUTHORIZED, "Credenciales ausentes o inválidas");
    }
}
