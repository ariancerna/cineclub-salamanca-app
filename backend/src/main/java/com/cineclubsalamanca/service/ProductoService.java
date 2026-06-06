package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.producto.ProductoRequest;
import com.cineclubsalamanca.dto.producto.ProductoResponse;
import com.cineclubsalamanca.entity.Producto;
import com.cineclubsalamanca.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream().map(ProductoResponse::from).toList();
    }

    public ProductoResponse crear(ProductoRequest req) {
        Producto producto = Producto.builder()
                .nombre(req.nombre())
                .precio(req.precio())
                .descripcion(req.descripcion())
                .imagenUrl(req.imagenUrl())
                .build();
        return ProductoResponse.from(productoRepository.save(producto));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto producto = buscarPorId(id);
        producto.setNombre(req.nombre());
        producto.setPrecio(req.precio());
        producto.setDescripcion(req.descripcion());
        producto.setImagenUrl(req.imagenUrl());
        return ProductoResponse.from(productoRepository.save(producto));
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        productoRepository.deleteById(id);
    }

    Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
    }
}
