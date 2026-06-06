package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.DetalleMinibar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleMinibarRepository extends JpaRepository<DetalleMinibar, Long> {
    List<DetalleMinibar> findByReservaCodigoReserva(String codigoReserva);
}
