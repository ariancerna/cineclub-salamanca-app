package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
