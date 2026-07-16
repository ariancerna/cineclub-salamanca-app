package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.pelicula.PeliculaRequest;
import com.cineclubsalamanca.dto.pelicula.PeliculaResponse;
import com.cineclubsalamanca.entity.Pelicula;
import com.cineclubsalamanca.repository.PeliculaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeliculaServiceTest {

    @Mock private PeliculaRepository peliculaRepository;

    @InjectMocks
    private PeliculaService peliculaService;

    private Pelicula peliculaBase() {
        return Pelicula.builder()
                .id(1L)
                .titulo("Metrópolis")
                .sinopsis("Una ciudad futurista dividida entre pensadores y obreros.")
                .director("Fritz Lang")
                .duracionMinutos(153)
                .aficheUrl("https://cineclub.test/metropolis.jpg")
                .build();
    }

    @Test
    @DisplayName("listarTodas devuelve la cartelera completa mapeada a DTO")
    void listarTodas_debeRetornarTodasLasPeliculas() {
        Pelicula otra = Pelicula.builder().id(2L).titulo("El Acorazado Potemkin").director("Eisenstein").build();
        when(peliculaRepository.findAll()).thenReturn(List.of(peliculaBase(), otra));

        List<PeliculaResponse> resultado = peliculaService.listarTodas();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(PeliculaResponse::titulo)
                .containsExactly("Metrópolis", "El Acorazado Potemkin");
    }

    @Test
    @DisplayName("listarTodas devuelve lista vacía cuando no hay películas cargadas")
    void listarTodas_debeRetornarListaVacia_cuandoNoHayPeliculas() {
        when(peliculaRepository.findAll()).thenReturn(List.of());

        assertThat(peliculaService.listarTodas()).isEmpty();
    }

    @Test
    @DisplayName("obtenerPorId devuelve la película cuando el id existe")
    void obtenerPorId_debeRetornarPelicula_cuandoExiste() {
        when(peliculaRepository.findById(1L)).thenReturn(Optional.of(peliculaBase()));

        PeliculaResponse resultado = peliculaService.obtenerPorId(1L);

        assertThat(resultado.titulo()).isEqualTo("Metrópolis");
        assertThat(resultado.director()).isEqualTo("Fritz Lang");
        assertThat(resultado.duracionMinutos()).isEqualTo(153);
    }

    @Test
    @DisplayName("obtenerPorId lanza EntityNotFoundException cuando el id no existe")
    void obtenerPorId_debeLanzarExcepcion_cuandoNoExiste() {
        when(peliculaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> peliculaService.obtenerPorId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Película no encontrada: 99");
    }

    @Test
    @DisplayName("crear persiste la película con todos los datos del request")
    void crear_debePersistirPelicula_conDatosDelRequest() {
        PeliculaRequest req = new PeliculaRequest(
                "Amanecer", "Un hombre del campo y la tentación de la ciudad.",
                "F. W. Murnau", 94, "https://cineclub.test/amanecer.jpg");

        when(peliculaRepository.save(any(Pelicula.class))).thenAnswer(inv -> inv.getArgument(0));

        PeliculaResponse resultado = peliculaService.crear(req);

        ArgumentCaptor<Pelicula> captor = ArgumentCaptor.forClass(Pelicula.class);
        verify(peliculaRepository).save(captor.capture());

        Pelicula guardada = captor.getValue();
        assertThat(guardada.getTitulo()).isEqualTo("Amanecer");
        assertThat(guardada.getDirector()).isEqualTo("F. W. Murnau");
        assertThat(guardada.getDuracionMinutos()).isEqualTo(94);
        assertThat(resultado.titulo()).isEqualTo("Amanecer");
    }

    @Test
    @DisplayName("actualizar modifica los campos de una película existente")
    void actualizar_debeModificarCampos_cuandoPeliculaExiste() {
        Pelicula existente = peliculaBase();
        PeliculaRequest req = new PeliculaRequest(
                "Metrópolis (restaurada)", "Versión restaurada de 2010.",
                "Fritz Lang", 148, "https://cineclub.test/metropolis-hd.jpg");

        when(peliculaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(peliculaRepository.save(any(Pelicula.class))).thenAnswer(inv -> inv.getArgument(0));

        PeliculaResponse resultado = peliculaService.actualizar(1L, req);

        assertThat(resultado.titulo()).isEqualTo("Metrópolis (restaurada)");
        assertThat(resultado.duracionMinutos()).isEqualTo(148);
        assertThat(existente.getTitulo()).isEqualTo("Metrópolis (restaurada)");
        verify(peliculaRepository).save(existente);
    }

    @Test
    @DisplayName("actualizar sobre un id inexistente no persiste nada")
    void actualizar_debeLanzarExcepcion_cuandoNoExiste() {
        PeliculaRequest req = new PeliculaRequest("X", null, null, 90, null);
        when(peliculaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> peliculaService.actualizar(99L, req))
                .isInstanceOf(EntityNotFoundException.class);

        verify(peliculaRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar borra la película cuando existe")
    void eliminar_debeBorrarPelicula_cuandoExiste() {
        when(peliculaRepository.findById(1L)).thenReturn(Optional.of(peliculaBase()));

        peliculaService.eliminar(1L);

        verify(peliculaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar verifica la existencia antes de borrar y no borra si no existe")
    void eliminar_debeLanzarExcepcion_cuandoNoExiste() {
        when(peliculaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> peliculaService.eliminar(99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(peliculaRepository, never()).deleteById(any());
    }
}
