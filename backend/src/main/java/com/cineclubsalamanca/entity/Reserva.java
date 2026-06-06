package com.cineclubsalamanca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reserva")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reserva {

    @Id
    @Column(name = "codigo_reserva", length = 12)
    private String codigoReserva;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "funcion_id")
    private Funcion funcion;

    @Column(name = "numero_butaca", length = 4)
    private String numeroButaca;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "asistio_ingreso", nullable = false)
    @Builder.Default
    private Boolean asistioIngreso = false;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleMinibar> detallesMinibar = new ArrayList<>();
}
