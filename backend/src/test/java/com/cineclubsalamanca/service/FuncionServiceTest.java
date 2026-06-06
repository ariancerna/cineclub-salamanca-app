package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.funcion.FuncionRequest;
import com.cineclubsalamanca.dto.funcion.FuncionResponse;
import com.cineclubsalamanca.entity.Funcion;
import com.cineclubsalamanca.entity.Pelicula;
import com.cineclubsalamanca.repository.FuncionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionServiceTest {

    @Mock private FuncionRepository funcionRepository;
    @Mock private PeliculaService peliculaService;

    @InjectMocks
    private FuncionService funcionService;

    private Pelicula peliculaBase() {
        return Pelicula.builder()
                .id(1L)
                .titulo("Ciudadano Kane")
                .director("Orson Welles")
                .duracionMinutos(119)
                .build();
    }

    private Funcion funcionBase(Pelicula p) {
        return Funcion.builder()
                .id(1L)
                .pelicula(p)
                .fechaHora(LocalDateTime.now().plusDays(3))
                .aforoMaximo(20)
                .aforoDisponible(20)
                .sala("Sala Norte")
                .build();
    }

    @Test
    @DisplayName("Crear función guarda los datos correctamente")
    void crear_debeRetornarFuncion_cuandoDatosValidos() {
        Pelicula p = peliculaBase();
        FuncionRequest req = new FuncionRequest(1L, LocalDateTime.now().plusDays(3), 20);

        when(peliculaService.buscarPorId(1L)).thenReturn(p);
        when(funcionRepository.save(any(Funcion.class))).thenReturn(funcionBase(p));

        FuncionResponse response = funcionService.crear(req);

        assertThat(response).isNotNull();
        assertThat(response.aforoMaximo()).isEqualTo(20);
        assertThat(response.aforoDisponible()).isEqualTo(20);
        assertThat(response.pelicula().titulo()).isEqualTo("Ciudadano Kane");
        verify(funcionRepository).save(any(Funcion.class));
    }

    @Test
    @DisplayName("Al crear, aforoDisponible arranca igual al aforoMaximo")
    void crear_debeInicializarAforoDisponible_igualAAforoMaximo() {
        Pelicula p = peliculaBase();
        FuncionRequest req = new FuncionRequest(1L, LocalDateTime.now().plusDays(3), 15);

        Funcion funcionGuardada = Funcion.builder()
                .id(2L).pelicula(p)
                .fechaHora(LocalDateTime.now().plusDays(3))
                .aforoMaximo(15).aforoDisponible(15).build();

        when(peliculaService.buscarPorId(1L)).thenReturn(p);
        when(funcionRepository.save(any(Funcion.class))).thenReturn(funcionGuardada);

        FuncionResponse response = funcionService.crear(req);

        assertThat(response.aforoDisponible()).isEqualTo(response.aforoMaximo());
    }

    @Test
    @DisplayName("Buscar función con ID inexistente lanza EntityNotFoundException")
    void buscarPorId_debeLanzarExcepcion_cuandoFuncionNoExiste() {
        when(funcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> funcionService.buscarPorId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Buscar función con ID válido retorna la función")
    void buscarPorId_debeRetornarFuncion_cuandoExiste() {
        Pelicula p = peliculaBase();
        when(funcionRepository.findById(1L)).thenReturn(Optional.of(funcionBase(p)));

        Funcion resultado = funcionService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lista próximas funciones ordenadas por fecha ascendente")
    void listarProximas_debeRetornarFuncionesEnElFuturo() {
        Pelicula p = peliculaBase();
        List<Funcion> funciones = List.of(funcionBase(p), funcionBase(p));
        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any(LocalDateTime.class)))
                .thenReturn(funciones);

        List<FuncionResponse> resultado = funcionService.listarProximas();

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("Lista vacía cuando no hay funciones programadas aún")
    void listarProximas_debeRetornarListaVacia_cuandoNoHayFuncionesFuturas() {
        when(funcionRepository.findByFechaHoraAfterOrderByFechaHoraAsc(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<FuncionResponse> resultado = funcionService.listarProximas();

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Eliminar función existente llama a deleteById")
    void eliminar_debeEliminar_cuandoFuncionExiste() {
        Pelicula p = peliculaBase();
        when(funcionRepository.findById(1L)).thenReturn(Optional.of(funcionBase(p)));

        funcionService.eliminar(1L);

        verify(funcionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Eliminar función inexistente lanza excepción sin tocar la BD")
    void eliminar_debeLanzarExcepcion_cuandoFuncionNoExiste() {
        when(funcionRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> funcionService.eliminar(55L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(funcionRepository, never()).deleteById(any());
    }
}
