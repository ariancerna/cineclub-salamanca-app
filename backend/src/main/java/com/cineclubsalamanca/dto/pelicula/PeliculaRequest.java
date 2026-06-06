package com.cineclubsalamanca.dto.pelicula;

import jakarta.validation.constraints.NotBlank;

public record PeliculaRequest(
    @NotBlank String titulo,
    String sinopsis,
    String director,
    Integer duracionMinutos,
    String aficheUrl
) {}
