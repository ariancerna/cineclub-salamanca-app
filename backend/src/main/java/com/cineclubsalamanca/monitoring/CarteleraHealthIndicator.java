package com.cineclubsalamanca.monitoring;

import com.cineclubsalamanca.repository.FuncionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Sonda de salud de la cartelera. Aparece en {@code GET /actuator/health} bajo la
 * clave {@code cartelera}.
 */
@Component("cartelera")
@RequiredArgsConstructor
public class CarteleraHealthIndicator implements HealthIndicator {

    /** Cartelera vacía. No es DOWN: Actuator mapea DOWN a 503 y el balanceador retiraría la instancia. */
    public static final Status SIN_CARTELERA = new Status("SIN_CARTELERA",
            "No hay funciones futuras programadas");

    private final FuncionRepository funcionRepository;

    @Override
    public Health health() {
        try {
            int funcionesFuturas = funcionRepository
                    .findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now())
                    .size();

            if (funcionesFuturas == 0) {
                return Health.status(SIN_CARTELERA)
                        .withDetail("funcionesFuturas", 0)
                        .withDetail("accion", "Programar nuevas funciones desde el panel de administración")
                        .build();
            }

            return Health.up()
                    .withDetail("funcionesFuturas", funcionesFuturas)
                    .build();

        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("motivo", "No se pudo consultar la cartelera")
                    .build();
        }
    }
}
