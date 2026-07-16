package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
    List<Reserva> findByUsuarioIdOrderByFechaEmisionDesc(Long usuarioId);
    List<Reserva> findByFuncionIdOrderByFechaEmisionAsc(Long funcionId);
    boolean existsByUsuarioIdAndFuncionId(Long usuarioId, Long funcionId);
    Optional<Reserva> findByNumeroButacaAndFuncionId(String numeroButaca, Long funcionId);

    @Query("SELECT r.numeroButaca FROM Reserva r WHERE r.funcion.id = :funcionId")
    List<String> findButacasOcupadasByFuncionId(Long funcionId);

    long countByFuncionId(Long funcionId);

    @Query("SELECT r FROM Reserva r WHERE r.funcion.fechaHora < :limite")
    List<Reserva> findDeFuncionesAnterioresA(LocalDateTime limite);

    long countByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta);
}
