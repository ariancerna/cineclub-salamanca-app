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
 * Responde 401 cuando la petición no está autenticada o el token JWT no es válido.
 *
 * <p>Sin un {@link AuthenticationEntryPoint} explícito, Spring Security aplica
 * {@code Http403ForbiddenEntryPoint} y devuelve 403 tanto al usuario anónimo como al
 * autenticado sin permisos. El cliente no puede entonces distinguir "necesitas iniciar
 * sesión" (401) de "tu sesión es válida pero no te alcanza el rol" (403), que es
 * precisamente la diferencia que el frontend necesita para decidir si redirige al login.</p>
 *
 * <p>El mensaje es deliberadamente genérico: no revela si el correo existe.</p>
 *
 * @see SinPermisos
 */
@Component
@RequiredArgsConstructor
public class EntradaNoAutenticada implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        RespuestaJsonError.escribir(objectMapper, response,
                HttpStatus.UNAUTHORIZED, "Credenciales ausentes o inválidas");
    }
}
