package com.cineclubsalamanca.maintenance;

import com.cineclubsalamanca.entity.Funcion;
import com.cineclubsalamanca.entity.Reserva;
import com.cineclubsalamanca.repository.FuncionRepository;
import com.cineclubsalamanca.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tareas programadas de mantenimiento. Las expresiones cron se leen de la configuración
 * para poder ajustarlas por entorno.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareasMantenimiento {

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;

    @Value("${app.mantenimiento.retencion-meses:12}")
    private int retencionMeses;

    /**
     * Registra el volumen de reservas del día y el aforo remanente. Permite reconstruir la
     * actividad histórica desde los logs aunque las reservas ya se hayan purgado.
     */
    @Scheduled(cron = "${app.mantenimiento.cron.reporte-diario:0 0 23 * * *}")
    public void reporteDiarioDeOperacion() {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);

        long reservasHoy = reservaRepository.countByFechaEmisionBetween(inicioDia, finDia);
        List<Funcion> futuras = funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now());
        int aforoLibre = futuras.stream()
                .mapToInt(f -> f.getAforoDisponible() == null ? 0 : f.getAforoDisponible())
                .sum();

        log.info("[MANTENIMIENTO] Reporte diario {} — reservas emitidas: {}, funciones futuras: {}, aforo libre: {}",
                LocalDate.now(), reservasHoy, futuras.size(), aforoLibre);
    }

    /**
     * Recalcula el aforo de las funciones futuras y corrige las desviaciones.
     *
     * <p>El aforo se descuenta al crear cada reserva, sin bloqueo. Si una transacción falla
     * a medias o dos reservas concurrentes se solapan, el contador se desvía del número real
     * de reservas.</p>
     */
    @Scheduled(cron = "${app.mantenimiento.cron.auditoria-aforo:0 30 3 * * MON}")
    @Transactional
    public void auditarConsistenciaDeAforo() {
        List<Funcion> futuras = funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime.now());
        int corregidas = 0;

        for (Funcion funcion : futuras) {
            long reservas = reservaRepository.countByFuncionId(funcion.getId());
            int esperado = funcion.getAforoMaximo() - (int) reservas;

            if (funcion.getAforoDisponible() == null || funcion.getAforoDisponible() != esperado) {
                log.warn("[MANTENIMIENTO] Aforo inconsistente en función {} — registrado: {}, real: {}. Corrigiendo.",
                        funcion.getId(), funcion.getAforoDisponible(), esperado);
                funcion.setAforoDisponible(esperado);
                funcionRepository.save(funcion);
                corregidas++;
            }
        }

        log.info("[MANTENIMIENTO] Auditoría de aforo completada — funciones revisadas: {}, corregidas: {}",
                futuras.size(), corregidas);
    }

    /**
     * Elimina las reservas de funciones anteriores al periodo de retención, para que la
     * tabla no crezca de forma indefinida. Los detalles del minibar caen en cascada.
     */
    @Scheduled(cron = "${app.mantenimiento.cron.purga-reservas:0 0 4 1 * *}")
    @Transactional
    public void purgarReservasAntiguas() {
        LocalDateTime limite = LocalDateTime.now().minusMonths(retencionMeses);
        List<Reserva> caducadas = reservaRepository.findDeFuncionesAnterioresA(limite);

        if (caducadas.isEmpty()) {
            log.info("[MANTENIMIENTO] Purga de reservas — no hay registros anteriores a {}", limite.toLocalDate());
            return;
        }

        reservaRepository.deleteAll(caducadas);
        log.info("[MANTENIMIENTO] Purga de reservas — {} reservas anteriores a {} eliminadas",
                caducadas.size(), limite.toLocalDate());
    }
}
