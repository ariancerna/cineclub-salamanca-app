package com.cineclubsalamanca.controller;

import com.cineclubsalamanca.dto.pelicula.PeliculaRequest;
import com.cineclubsalamanca.dto.pelicula.PeliculaResponse;
import com.cineclubsalamanca.service.PeliculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/peliculas")
@RequiredArgsConstructor
public class PeliculaController {

    private final PeliculaService peliculaService;

    @GetMapping
    public ResponseEntity<List<PeliculaResponse>> listar() {
        return ResponseEntity.ok(peliculaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeliculaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(peliculaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PeliculaResponse> crear(@Valid @RequestBody PeliculaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(peliculaService.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PeliculaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody PeliculaRequest req) {
        return ResponseEntity.ok(peliculaService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        peliculaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
