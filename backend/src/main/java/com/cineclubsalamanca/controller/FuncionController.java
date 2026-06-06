package com.cineclubsalamanca.controller;

import com.cineclubsalamanca.dto.funcion.FuncionRequest;
import com.cineclubsalamanca.dto.funcion.FuncionResponse;
import com.cineclubsalamanca.service.FuncionService;
import com.cineclubsalamanca.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funciones")
@RequiredArgsConstructor
public class FuncionController {

    private final FuncionService funcionService;
    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<List<FuncionResponse>> listarProximas() {
        return ResponseEntity.ok(funcionService.listarProximas());
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuncionResponse>> listarTodas() {
        return ResponseEntity.ok(funcionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(funcionService.obtenerPorId(id));
    }

    @GetMapping("/{id}/butacas-ocupadas")
    public ResponseEntity<List<String>> butacasOcupadas(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.butacasOcupadas(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionResponse> crear(@Valid @RequestBody FuncionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionService.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuncionResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody FuncionRequest req) {
        return ResponseEntity.ok(funcionService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        funcionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
