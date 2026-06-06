package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.Funcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FuncionRepository extends JpaRepository<Funcion, Long> {
    List<Funcion> findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime desde);
    List<Funcion> findByPeliculaIdOrderByFechaHoraAsc(Long peliculaId);
}
