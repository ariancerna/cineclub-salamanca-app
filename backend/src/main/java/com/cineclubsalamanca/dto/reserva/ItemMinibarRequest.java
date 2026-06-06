package com.cineclubsalamanca.dto.reserva;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemMinibarRequest(
    @NotNull Long productoId,
    @NotNull @Min(1) Integer cantidad
) {}
