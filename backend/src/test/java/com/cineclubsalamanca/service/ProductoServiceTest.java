package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.producto.ProductoRequest;
import com.cineclubsalamanca.dto.producto.ProductoResponse;
import com.cineclubsalamanca.entity.Producto;
import com.cineclubsalamanca.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto productoBase() {
        return Producto.builder()
                .id(1L)
                .nombre("Canchita salada")
                .precio(new BigDecimal("8.50"))
                .descripcion("Porción personal")
                .imagenUrl("https://cineclub.test/canchita.jpg")
                .build();
    }

    @Test
    @DisplayName("listarTodos devuelve el catálogo del minibar")
    void listarTodos_debeRetornarTodosLosProductos() {
        Producto gaseosa = Producto.builder().id(2L).nombre("Gaseosa").precio(new BigDecimal("5.00")).build();
        when(productoRepository.findAll()).thenReturn(List.of(productoBase(), gaseosa));

        List<ProductoResponse> resultado = productoService.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ProductoResponse::nombre)
                .containsExactly("Canchita salada", "Gaseosa");
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacía cuando el minibar no tiene productos")
    void listarTodos_debeRetornarListaVacia_cuandoNoHayProductos() {
        when(productoRepository.findAll()).thenReturn(List.of());

        assertThat(productoService.listarTodos()).isEmpty();
    }

    @Test
    @DisplayName("crear persiste el producto conservando el precio exacto")
    void crear_debePersistirProducto_conDatosDelRequest() {
        ProductoRequest req = new ProductoRequest(
                "Chocolate", new BigDecimal("4.20"), "Barra de 40g", "https://cineclub.test/choco.jpg");

        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse resultado = productoService.crear(req);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());

        Producto guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Chocolate");
        // El precio se maneja con BigDecimal para no perder precisión monetaria
        assertThat(guardado.getPrecio()).isEqualByComparingTo("4.20");
        assertThat(resultado.precio()).isEqualByComparingTo("4.20");
    }

    @Test
    @DisplayName("crear acepta un producto de cortesía con precio cero")
    void crear_debeAceptarPrecioCero() {
        ProductoRequest req = new ProductoRequest("Agua de cortesía", BigDecimal.ZERO, null, null);
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse resultado = productoService.crear(req);

        assertThat(resultado.precio()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("actualizar modifica los campos de un producto existente")
    void actualizar_debeModificarCampos_cuandoProductoExiste() {
        Producto existente = productoBase();
        ProductoRequest req = new ProductoRequest(
                "Canchita mantequilla", new BigDecimal("9.90"), "Porción grande", "https://cineclub.test/canchita2.jpg");

        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoResponse resultado = productoService.actualizar(1L, req);

        assertThat(resultado.nombre()).isEqualTo("Canchita mantequilla");
        assertThat(resultado.precio()).isEqualByComparingTo("9.90");
        verify(productoRepository).save(existente);
    }

    @Test
    @DisplayName("actualizar sobre un id inexistente no persiste nada")
    void actualizar_debeLanzarExcepcion_cuandoNoExiste() {
        ProductoRequest req = new ProductoRequest("X", new BigDecimal("1.00"), null, null);
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(99L, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Producto no encontrado: 99");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar borra el producto cuando existe")
    void eliminar_debeBorrarProducto_cuandoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase()));

        productoService.eliminar(1L);

        verify(productoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar verifica la existencia antes de borrar y no borra si no existe")
    void eliminar_debeLanzarExcepcion_cuandoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.eliminar(99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("buscarPorId devuelve la entidad para que ReservaService calcule el subtotal")
    void buscarPorId_debeRetornarEntidad_cuandoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase()));

        Producto resultado = productoService.buscarPorId(1L);

        assertThat(resultado.getNombre()).isEqualTo("Canchita salada");
        assertThat(resultado.getPrecio()).isEqualByComparingTo("8.50");
    }
}
