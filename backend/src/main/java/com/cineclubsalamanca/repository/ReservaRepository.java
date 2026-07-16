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

    /** Cuenta las reservas asociadas a una función; usado para auditar el aforo. */
    long countByFuncionId(Long funcionId);

    /** Reservas de funciones ya proyectadas antes de la fecha indicada (purga por retención). */
    @Query("SELECT r FROM Reserva r WHERE r.funcion.fechaHora < :limite")
    List<Reserva> findDeFuncionesAnterioresA(LocalDateTime limite);

    /** Reservas emitidas dentro de un rango, para el reporte diario de operación. */
    long countByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta);
}
