package com.cineclubsalamanca.controller;

import com.cineclubsalamanca.dto.reserva.CrearReservaRequest;
import com.cineclubsalamanca.dto.reserva.ReservaResponse;
import com.cineclubsalamanca.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> crear(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody CrearReservaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(userDetails.getUsername(), req));
    }

    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponse>> misReservas(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reservaService.listarMisReservas(userDetails.getUsername()));
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(reservaService.obtenerPorCodigo(codigo));
    }

    @GetMapping("/funcion/{funcionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarPorFuncion(@PathVariable Long funcionId) {
        return ResponseEntity.ok(reservaService.listarPorFuncion(funcionId));
    }

    @PatchMapping("/{codigo}/confirmar-ingreso")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponse> confirmarIngreso(@PathVariable String codigo) {
        return ResponseEntity.ok(reservaService.confirmarIngreso(codigo));
    }
}
