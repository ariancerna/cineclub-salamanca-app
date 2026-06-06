package com.cineclubsalamanca.dto.auth;

public record JwtResponse(
    String token,
    String tipo,
    Long id,
    String nombre,
    String email,
    String rol
) {
    public JwtResponse(String token, Long id, String nombre, String email, String rol) {
        this(token, "Bearer", id, nombre, email, rol);
    }
}
