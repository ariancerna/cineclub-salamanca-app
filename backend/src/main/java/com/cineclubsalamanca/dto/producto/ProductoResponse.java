package com.cineclubsalamanca.dto.producto;

import com.cineclubsalamanca.entity.Producto;

import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    String nombre,
    BigDecimal precio,
    String descripcion,
    String imagenUrl
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getPrecio(),
                p.getDescripcion(), p.getImagenUrl());
    }
}
