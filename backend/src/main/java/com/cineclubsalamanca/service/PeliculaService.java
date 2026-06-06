package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.pelicula.PeliculaRequest;
import com.cineclubsalamanca.dto.pelicula.PeliculaResponse;
import com.cineclubsalamanca.entity.Pelicula;
import com.cineclubsalamanca.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeliculaService {

    private final PeliculaRepository peliculaRepository;

    public List<PeliculaResponse> listarTodas() {
        return peliculaRepository.findAll().stream().map(PeliculaResponse::from).toList();
    }

    public PeliculaResponse obtenerPorId(Long id) {
        return PeliculaResponse.from(buscarPorId(id));
    }

    public PeliculaResponse crear(PeliculaRequest req) {
        Pelicula pelicula = Pelicula.builder()
                .titulo(req.titulo())
                .sinopsis(req.sinopsis())
                .director(req.director())
                .duracionMinutos(req.duracionMinutos())
                .aficheUrl(req.aficheUrl())
                .build();
        return PeliculaResponse.from(peliculaRepository.save(pelicula));
    }

    public PeliculaResponse actualizar(Long id, PeliculaRequest req) {
        Pelicula pelicula = buscarPorId(id);
        pelicula.setTitulo(req.titulo());
        pelicula.setSinopsis(req.sinopsis());
        pelicula.setDirector(req.director());
        pelicula.setDuracionMinutos(req.duracionMinutos());
        pelicula.setAficheUrl(req.aficheUrl());
        return PeliculaResponse.from(peliculaRepository.save(pelicula));
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        peliculaRepository.deleteById(id);
    }

    Pelicula buscarPorId(Long id) {
        return peliculaRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Película no encontrada: " + id));
    }
}
