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
 * Tareas programadas de mantenimiento de la aplicación (cron jobs).
 *
 * <p>Las expresiones cron se leen desde la configuración para poder ajustarlas por entorno
 * sin recompilar. Todas las tareas son idempotentes y registran su resultado en el log, de
 * modo que su ejecución queda auditada junto con el resto de la operación.</p>
 *
 * @see com.cineclubsalamanca.config.SchedulingConfig
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TareasMantenimiento {

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;

    /** Meses de antigüedad a partir de los cuales una reserva pasada deja de conservarse. */
    @Value("${app.mantenimiento.retencion-meses:12}")
    private int retencionMeses;

    /**
     * Reporte diario de operación. Deja en el log el volumen de reservas del día y el
     * aforo remanente, lo que permite reconstruir la actividad histórica desde los logs
     * rotados sin necesidad de consultar la base de datos.
     *
     * <p>Se ejecuta a las 23:00 todos los días, tras la última función.</p>
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
     * Audita la consistencia del aforo de las funciones futuras.
     *
     * <p>El aforo disponible se descuenta al crear cada reserva. Si una transacción falla a
     * medias, o dos reservas concurrentes se solapan, el contador puede desviarse respecto
     * al número real de reservas. Esta tarea recalcula el valor correcto
     * ({@code aforoMaximo - reservas}) y corrige las desviaciones, dejando constancia en el log.</p>
     *
     * <p>Se ejecuta los lunes a las 03:30, fuera del horario de funciones.</p>
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
     * Purga las reservas de funciones antiguas según la política de retención.
     *
     * <p>Evita el crecimiento indefinido de la tabla {@code reserva}. Los detalles del
     * minibar se eliminan en cascada. El histórico agregado permanece en los reportes
     * diarios del log, por lo que el borrado no pierde información de gestión.</p>
     *
     * <p>Se ejecuta el día 1 de cada mes a las 04:00.</p>
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
