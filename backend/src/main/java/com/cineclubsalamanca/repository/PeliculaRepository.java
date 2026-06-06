package com.cineclubsalamanca.repository;

import com.cineclubsalamanca.entity.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {
}
