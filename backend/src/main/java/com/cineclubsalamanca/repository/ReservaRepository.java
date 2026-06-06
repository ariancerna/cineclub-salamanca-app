package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
    List<Reserva> findByUsuarioIdOrderByFechaEmisionDesc(Long usuarioId);
    List<Reserva> findByFuncionIdOrderByFechaEmisionAsc(Long funcionId);
    boolean existsByUsuarioIdAndFuncionId(Long usuarioId, Long funcionId);
    Optional<Reserva> findByNumeroButacaAndFuncionId(String numeroButaca, Long funcionId);

    @Query("SELECT r.numeroButaca FROM Reserva r WHERE r.funcion.id = :funcionId")
    List<String> findButacasOcupadasByFuncionId(Long funcionId);
}
