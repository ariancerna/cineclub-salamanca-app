package com.cineclubsalamanca.dto.pelicula;

import com.cineclubsalamanca.entity.Pelicula;

public record PeliculaResponse(
    Long id,
    String titulo,
    String sinopsis,
    String director,
    Integer duracionMinutos,
    String aficheUrl
) {
    public static PeliculaResponse from(Pelicula p) {
        return new PeliculaResponse(p.getId(), p.getTitulo(), p.getSinopsis(),
                p.getDirector(), p.getDuracionMinutos(), p.getAficheUrl());
    }
}
