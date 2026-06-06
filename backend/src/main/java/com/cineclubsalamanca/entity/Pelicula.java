package com.cineclubsalamanca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pelicula")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String sinopsis;

    private String director;

    private Integer duracionMinutos;

    @Column(name = "afiche_url", columnDefinition = "TEXT")
    private String aficheUrl;
}
