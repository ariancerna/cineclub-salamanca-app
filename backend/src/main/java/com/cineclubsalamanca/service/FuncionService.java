package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.funcion.FuncionRequest;
import com.cineclubsalamanca.dto.funcion.FuncionResponse;
import com.cineclubsalamanca.entity.Funcion;
import com.cineclubsalamanca.entity.Pelicula;
import com.cineclubsalamanca.repository.FuncionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionService {

    private final FuncionRepository funcionRepository;
    private final PeliculaService peliculaService;

    public List<FuncionResponse> listarProximas() {
        return funcionRepository
                .findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now())
                .stream().map(FuncionResponse::from).toList();
    }

    public List<FuncionResponse> listarTodas() {
        return funcionRepository.findAll().stream().map(FuncionResponse::from).toList();
    }

    public FuncionResponse obtenerPorId(Long id) {
        return FuncionResponse.from(buscarPorId(id));
    }

    public FuncionResponse crear(FuncionRequest req) {
        Pelicula pelicula = peliculaService.buscarPorId(req.peliculaId());
        Funcion funcion = Funcion.builder()
                .pelicula(pelicula)
                .fechaHora(req.fechaHora())
                .aforoMaximo(req.aforoMaximo())
                .aforoDisponible(req.aforoMaximo())
                .build();
        return FuncionResponse.from(funcionRepository.save(funcion));
    }

    public FuncionResponse actualizar(Long id, FuncionRequest req) {
        Funcion funcion = buscarPorId(id);
        Pelicula pelicula = peliculaService.buscarPorId(req.peliculaId());
        funcion.setPelicula(pelicula);
        funcion.setFechaHora(req.fechaHora());
        funcion.setAforoMaximo(req.aforoMaximo());
        return FuncionResponse.from(funcionRepository.save(funcion));
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        funcionRepository.deleteById(id);
    }

    public Funcion buscarPorId(Long id) {
        return funcionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Función no encontrada: " + id));
    }
}
