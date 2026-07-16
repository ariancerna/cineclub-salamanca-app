package com.cineclubsalamanca.monitoring;

import com.cineclubsalamanca.repository.FuncionRepository;
import com.cineclubsalamanca.repository.ReservaRepository;
import com.cineclubsalamanca.repository.UsuarioRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Métricas de negocio para {@code /actuator/metrics} y {@code /actuator/prometheus}.
 *
 * <p>Se implementa como MeterBinder para no tener que instrumentar los servicios.
 * Los valores se leen cuando el sistema de monitoreo consulta las métricas.</p>
 */
@Component
@RequiredArgsConstructor
public class MetricasNegocio implements MeterBinder {

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void bindTo(MeterRegistry registry) {

        Gauge.builder("cineclub.reservas.totales", reservaRepository, ReservaRepository::count)
                .description("Número total de reservas registradas")
                .baseUnit("reservas")
                .register(registry);

        Gauge.builder("cineclub.reservas.asistencias", this, MetricasNegocio::contarAsistencias)
                .description("Reservas cuyo ingreso fue confirmado en puerta")
                .baseUnit("reservas")
                .register(registry);

        Gauge.builder("cineclub.funciones.futuras", this, MetricasNegocio::contarFuncionesFuturas)
                .description("Funciones programadas con fecha posterior a ahora")
                .baseUnit("funciones")
                .register(registry);

        Gauge.builder("cineclub.aforo.disponible", this, MetricasNegocio::contarAforoDisponible)
                .description("Butacas libres sumando todas las funciones futuras")
                .baseUnit("butacas")
                .register(registry);

        Gauge.builder("cineclub.usuarios.registrados", usuarioRepository, UsuarioRepository::count)
                .description("Número de usuarios registrados en la plataforma")
                .baseUnit("usuarios")
                .register(registry);
    }

    private double contarAsistencias() {
        return reservaRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getAsistioIngreso()))
                .count();
    }

    private double contarFuncionesFuturas() {
        return funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now()).size();
    }

    private double contarAforoDisponible() {
        return funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now())
                .stream()
                .mapToInt(f -> f.getAforoDisponible() == null ? 0 : f.getAforoDisponible())
                .sum();
    }
}
