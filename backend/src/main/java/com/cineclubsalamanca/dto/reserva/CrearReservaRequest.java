package com.cineclubsalamanca.dto.reserva;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearReservaRequest(
    @NotNull Long funcionId,
    @NotBlank String numeroButaca,
    @Valid List<ItemMinibarRequest> itemsMinibar
) {}
