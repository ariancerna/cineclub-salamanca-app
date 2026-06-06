package com.cineclubsalamanca.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank String nombre,
    @NotNull @DecimalMin("0.00") BigDecimal precio,
    String descripcion,
    String imagenUrl
) {}
