package com.cineclubsalamanca.maintenance;

import com.cineclubsalamanca.entity.Funcion;
import com.cineclubsalamanca.entity.Pelicula;
import com.cineclubsalamanca.entity.Reserva;
import com.cineclubsalamanca.repository.FuncionRepository;
import com.cineclubsalamanca.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tareas programadas de mantenimiento")
class TareasMantenimientoTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private FuncionRepository funcionRepository;

    @InjectMocks
    private TareasMantenimiento tareas;

    @BeforeEach
    void configurarRetencion() {
        // El valor lo inyecta @Value en producción; aquí se fija explícitamente
        ReflectionTestUtils.setField(tareas, "retencionMeses", 12);
    }

    private Funcion funcion(Long id, int aforoMaximo, int aforoDisponible) {
        return Funcion.builder()
                .id(id)
                .pelicula(Pelicula.builder().id(1L).titulo("Metrópolis").build())
                .fechaHora(LocalDateTime.now().plusDays(2))
                .aforoMaximo(aforoMaximo)
                .aforoDisponible(aforoDisponible)
                .sala("Sala Principal")
                .build();
    }

    // ---------- Auditoría de consistencia de aforo ----------

    @Test
    @DisplayName("Corrige el aforo cuando el contador se desvió de las reservas reales")
    void auditarAforo_debeCorregir_cuandoContadorDesviado() {
        // 32 butacas, 3 reservas reales -> deberían quedar 29, pero el contador dice 31
        Funcion desviada = funcion(1L, 32, 31);

        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(desviada));
        when(reservaRepository.countByFuncionId(1L)).thenReturn(3L);

        tareas.auditarConsistenciaDeAforo();

        assertThat(desviada.getAforoDisponible()).isEqualTo(29);
        verify(funcionRepository).save(desviada);
    }

    @Test
    @DisplayName("No escribe en la base cuando el aforo ya es consistente")
    void auditarAforo_noDebeGuardar_cuandoContadorCorrecto() {
        Funcion correcta = funcion(1L, 32, 29);

        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(correcta));
        when(reservaRepository.countByFuncionId(1L)).thenReturn(3L);

        tareas.auditarConsistenciaDeAforo();

        assertThat(correcta.getAforoDisponible()).isEqualTo(29);
        verify(funcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trata un aforo nulo como inconsistente y lo recalcula")
    void auditarAforo_debeCorregir_cuandoAforoEsNulo() {
        Funcion sinAforo = funcion(1L, 32, 0);
        sinAforo.setAforoDisponible(null);

        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(sinAforo));
        when(reservaRepository.countByFuncionId(1L)).thenReturn(2L);

        tareas.auditarConsistenciaDeAforo();

        assertThat(sinAforo.getAforoDisponible()).isEqualTo(30);
        verify(funcionRepository).save(sinAforo);
    }

    @Test
    @DisplayName("Revisa cada función y corrige solo las desviadas")
    void auditarAforo_debeCorregirSoloLasDesviadas() {
        Funcion ok = funcion(1L, 32, 30);        // 2 reservas -> 30, correcto
        Funcion mal = funcion(2L, 20, 20);       // 5 reservas -> 15, desviado

        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(ok, mal));
        when(reservaRepository.countByFuncionId(1L)).thenReturn(2L);
        when(reservaRepository.countByFuncionId(2L)).thenReturn(5L);

        tareas.auditarConsistenciaDeAforo();

        assertThat(ok.getAforoDisponible()).isEqualTo(30);
        assertThat(mal.getAforoDisponible()).isEqualTo(15);
        verify(funcionRepository, times(1)).save(any(Funcion.class));
        verify(funcionRepository).save(mal);
    }

    @Test
    @DisplayName("No falla cuando no hay funciones futuras que auditar")
    void auditarAforo_debeSoportarCarteleraVacia() {
        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of());

        tareas.auditarConsistenciaDeAforo();

        verify(funcionRepository, never()).save(any());
        verify(reservaRepository, never()).countByFuncionId(anyLong());
    }

    // ---------- Purga de reservas antiguas ----------

    @Test
    @DisplayName("Purga las reservas anteriores al periodo de retención")
    void purgar_debeEliminarReservasCaducadas() {
        List<Reserva> caducadas = List.of(
                Reserva.builder().codigoReserva("SLM-VIEJA01").build(),
                Reserva.builder().codigoReserva("SLM-VIEJA02").build());

        when(reservaRepository.findDeFuncionesAnterioresA(any())).thenReturn(caducadas);

        tareas.purgarReservasAntiguas();

        verify(reservaRepository).deleteAll(caducadas);
    }

    @Test
    @DisplayName("No llama a deleteAll cuando no hay nada que purgar")
    void purgar_noDebeBorrar_cuandoNoHayCaducadas() {
        when(reservaRepository.findDeFuncionesAnterioresA(any())).thenReturn(List.of());

        tareas.purgarReservasAntiguas();

        verify(reservaRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("El límite de purga respeta los meses de retención configurados")
    void purgar_debeUsarLaRetencionConfigurada() {
        ReflectionTestUtils.setField(tareas, "retencionMeses", 6);
        when(reservaRepository.findDeFuncionesAnterioresA(any())).thenReturn(List.of());

        tareas.purgarReservasAntiguas();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservaRepository).findDeFuncionesAnterioresA(captor.capture());

        // El límite debe caer ~6 meses atrás, no los 12 por defecto
        assertThat(captor.getValue())
                .isCloseTo(LocalDateTime.now().minusMonths(6), within(1, ChronoUnit.MINUTES));
    }

    // ---------- Reporte diario ----------

    @Test
    @DisplayName("El reporte diario consulta las reservas del día y el aforo remanente")
    void reporteDiario_debeConsultarReservasYAforo() {
        when(reservaRepository.countByFechaEmisionBetween(any(), any())).thenReturn(12L);
        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(funcion(1L, 32, 20), funcion(2L, 20, 15)));

        tareas.reporteDiarioDeOperacion();

        verify(reservaRepository).countByFechaEmisionBetween(any(), any());
        verify(funcionRepository).findByFechaHoraAfterOrderByFechaHoraAsc(any());
    }

    @Test
    @DisplayName("El reporte diario no falla si una función tiene el aforo nulo")
    void reporteDiario_debeSoportarAforoNulo() {
        Funcion sinAforo = funcion(1L, 32, 0);
        sinAforo.setAforoDisponible(null);

        when(reservaRepository.countByFechaEmisionBetween(any(), any())).thenReturn(0L);
        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any()))
                .thenReturn(List.of(sinAforo));

        // No debe lanzar NullPointerException al sumar el aforo
        tareas.reporteDiarioDeOperacion();

        verify(funcionRepository).findByFechaHoraAfterOrderByFechaHoraAsc(any());
    }
}
