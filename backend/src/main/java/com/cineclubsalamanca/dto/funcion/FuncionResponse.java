package com.cineclubsalamanca.dto.funcion;

import com.cineclubsalamanca.dto.pelicula.PeliculaResponse;
import com.cineclubsalamanca.entity.Funcion;

import java.time.LocalDateTime;

public record FuncionResponse(
    Long id,
    PeliculaResponse pelicula,
    LocalDateTime fechaHora,
    Integer aforoMaximo,
    Integer aforoDisponible,
    String sala
) {
    public static FuncionResponse from(Funcion f) {
        return new FuncionResponse(
                f.getId(),
                PeliculaResponse.from(f.getPelicula()),
                f.getFechaHora(),
                f.getAforoMaximo(),
                f.getAforoDisponible(),
                f.getSala()
        );
    }
}
