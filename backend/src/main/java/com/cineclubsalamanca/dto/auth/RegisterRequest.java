package com.cineclubsalamanca.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String nombre,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres") String password
) {}
