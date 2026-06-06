package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.reserva.CrearReservaRequest;
import com.cineclubsalamanca.dto.reserva.ReservaResponse;
import com.cineclubsalamanca.entity.*;
import com.cineclubsalamanca.repository.*;
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
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FuncionService funcionService;
    @Mock private FuncionRepository funcionRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuarioBase() {
        return Usuario.builder()
                .id(1L)
                .nombre("Joan Pelayo")
                .email("joan@test.com")
                .passwordHash("hashed")
                .rol(Rol.ROLE_USER)
                .build();
    }

    private Funcion funcionBase() {
        Pelicula pelicula = Pelicula.builder()
                .id(1L)
                .titulo("Metrópolis")
                .director("Fritz Lang")
                .duracionMinutos(153)
                .build();

        return Funcion.builder()
                .id(1L)
                .pelicula(pelicula)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .aforoMaximo(20)
                .aforoDisponible(10)
                .sala("Sala Principal")
                .build();
    }

    @Test
    @DisplayName("Reserva exitosa retorna código y datos completos")
    void crear_debeRetornarReserva_cuandoDatosValidos() {
        Usuario usuario = usuarioBase();
        Funcion funcion = funcionBase();
        CrearReservaRequest req = new CrearReservaRequest(1L, "A-1", Collections.emptyList());

        Reserva reservaGuardada = Reserva.builder()
                .codigoReserva("SLM-TEST01")
                .usuario(usuario)
                .funcion(funcion)
                .numeroButaca("A-1")
                .fechaEmision(LocalDateTime.now())
                .asistioIngreso(false)
                .detallesMinibar(Collections.emptyList())
                .build();

        when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
        when(funcionService.buscarPorId(1L)).thenReturn(funcion);
        when(reservaRepository.existsByUsuarioIdAndFuncionId(1L, 1L)).thenReturn(false);
        when(reservaRepository.findByNumeroButacaAndFuncionId("A-1", 1L)).thenReturn(Optional.empty());
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reservaGuardada);
        when(funcionRepository.save(any(Funcion.class))).thenReturn(funcion);

        ReservaResponse response = reservaService.crear("joan@test.com", req);

        assertThat(response.codigoReserva()).isEqualTo("SLM-TEST01");
        assertThat(response.numeroButaca()).isEqualTo("A-1");
        assertThat(response.usuarioNombre()).isEqualTo("Joan Pelayo");
        assertThat(response.asistioIngreso()).isFalse();
        verify(reservaRepository).save(any(Reserva.class));
        verify(funcionRepository).save(any(Funcion.class));
    }

    @Test
    @DisplayName("No se puede reservar si el aforo está agotado")
    void crear_debeLanzarExcepcion_cuandoAforoAgotado() {
        Usuario usuario = usuarioBase();
        Funcion funcion = funcionBase();
        funcion.setAforoDisponible(0);
        CrearReservaRequest req = new CrearReservaRequest(1L, "A-1", Collections.emptyList());

        when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
        when(funcionService.buscarPorId(1L)).thenReturn(funcion);

        assertThatThrownBy(() -> reservaService.crear("joan@test.com", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay butacas disponibles");

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario no puede reservar dos veces la misma función")
    void crear_debeLanzarExcepcion_cuandoUsuarioYaTieneReserva() {
        Usuario usuario = usuarioBase();
        CrearReservaRequest req = new CrearReservaRequest(1L, "A-2", Collections.emptyList());

        when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
        when(funcionService.buscarPorId(1L)).thenReturn(funcionBase());
        when(reservaRepository.existsByUsuarioIdAndFuncionId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reservaService.crear("joan@test.com", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya tienes una reserva");

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede reservar una butaca ya ocupada por otro usuario")
    void crear_debeLanzarExcepcion_cuandoButacaOcupada() {
        Usuario usuario = usuarioBase();
        CrearReservaRequest req = new CrearReservaRequest(1L, "B-3", Collections.emptyList());
        Reserva reservaExistente = Reserva.builder().codigoReserva("SLM-EXIST1").build();

        when(usuarioRepository.findByEmail("joan@test.com")).thenReturn(Optional.of(usuario));
        when(funcionService.buscarPorId(1L)).thenReturn(funcionBase());
        when(reservaRepository.existsByUsuarioIdAndFuncionId(1L, 1L)).thenReturn(false);
        when(reservaRepository.findByNumeroButacaAndFuncionId("B-3", 1L))
                .thenReturn(Optional.of(reservaExistente));

        assertThatThrownBy(() -> reservaService.crear("joan@test.com", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("B-3");

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email inexistente lanza EntityNotFoundException")
    void crear_debeLanzarExcepcion_cuandoUsuarioNoExiste() {
        CrearReservaRequest req = new CrearReservaRequest(1L, "A-1", Collections.emptyList());
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crear("noexiste@test.com", req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    @DisplayName("El admin confirma ingreso y asistioIngreso pasa a true")
    void confirmarIngreso_debeMarcarAsistencia_cuandoCodigoValido() {
        Usuario usuario = usuarioBase();
        Funcion funcion = funcionBase();

        Reserva reserva = Reserva.builder()
                .codigoReserva("SLM-ABC123")
                .usuario(usuario)
                .funcion(funcion)
                .numeroButaca("C-5")
                .fechaEmision(LocalDateTime.now())
                .asistioIngreso(false)
                .detallesMinibar(Collections.emptyList())
                .build();

        Reserva reservaConfirmada = Reserva.builder()
                .codigoReserva("SLM-ABC123")
                .usuario(usuario)
                .funcion(funcion)
                .numeroButaca("C-5")
                .fechaEmision(reserva.getFechaEmision())
                .asistioIngreso(true)
                .detallesMinibar(Collections.emptyList())
                .build();

        when(reservaRepository.findById("SLM-ABC123")).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reservaConfirmada);

        ReservaResponse response = reservaService.confirmarIngreso("SLM-ABC123");

        assertThat(response.asistioIngreso()).isTrue();
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Código de reserva inválido lanza EntityNotFoundException")
    void confirmarIngreso_debeLanzarExcepcion_cuandoCodigoInvalido() {
        when(reservaRepository.findById("INVALIDO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.confirmarIngreso("INVALIDO"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Reserva no encontrada");
    }

    @Test
    @DisplayName("Retorna los códigos de butaca ocupados para renderizar el mapa de asientos")
    void butacasOcupadas_debeRetornarListaCorrecta() {
        List<String> butacas = List.of("A-1", "B-3", "C-5");
        when(reservaRepository.findButacasOcupadasByFuncionId(1L)).thenReturn(butacas);

        List<String> resultado = reservaService.butacasOcupadas(1L);

        assertThat(resultado).hasSize(3).containsExactlyInAnyOrder("A-1", "B-3", "C-5");
    }

    @Test
    @DisplayName("Lista vacía cuando la función no tiene reservas aún")
    void butacasOcupadas_debeRetornarListaVacia_cuandoNoHayReservas() {
        when(reservaRepository.findButacasOcupadasByFuncionId(99L)).thenReturn(Collections.emptyList());

        List<String> resultado = reservaService.butacasOcupadas(99L);

        assertThat(resultado).isEmpty();
    }
}
