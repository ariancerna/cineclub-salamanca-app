package com.cineclubsalamanca.dto.funcion;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FuncionRequest(
    @NotNull Long peliculaId,
    @NotNull @Future LocalDateTime fechaHora,
    @NotNull @Min(1) Integer aforoMaximo
) {}
