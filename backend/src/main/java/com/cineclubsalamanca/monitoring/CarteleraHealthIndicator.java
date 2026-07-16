package com.cineclubsalamanca.monitoring;

import com.cineclubsalamanca.repository.FuncionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Indicador de salud funcional de la cartelera.
 *
 * <p>Complementa a las sondas técnicas de Actuator (base de datos, disco). Una aplicación
 * puede responder correctamente y aun así ser inútil para el espectador si no hay funciones
 * futuras publicadas: la cartelera aparecería vacía.</p>
 *
 * <p>Una cartelera vacía se reporta con el estado propio {@link #SIN_CARTELERA} y
 * <em>no</em> como {@code DOWN}. La diferencia es deliberada: Actuator traduce {@code DOWN}
 * a un HTTP 503, lo que haría que el balanceador retirase la instancia y que Docker
 * reiniciase el contenedor. Pero que el administrador no haya programado funciones es una
 * condición de negocio, no una avería del sistema: la aplicación sigue sana y debe seguir
 * atendiendo tráfico. Los estados no reconocidos se mapean a HTTP 200 y tienen menor
 * precedencia que {@code UP} al agregar, de modo que el aviso queda visible en el detalle
 * del endpoint sin degradar la salud global.</p>
 *
 * <p>Se consulta desde {@code GET /actuator/health}, bajo la clave {@code cartelera}.</p>
 */
@Component("cartelera")
@RequiredArgsConstructor
public class CarteleraHealthIndicator implements HealthIndicator {

    /** No hay funciones futuras: la cartelera está vacía, pero la aplicación opera con normalidad. */
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
            // Un fallo al consultar sí es una avería real: aquí DOWN es lo correcto.
            return Health.down(e)
                    .withDetail("motivo", "No se pudo consultar la cartelera")
                    .build();
        }
    }
}
