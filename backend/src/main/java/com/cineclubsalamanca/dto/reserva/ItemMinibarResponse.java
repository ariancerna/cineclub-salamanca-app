package com.cineclubsalamanca.dto.reserva;

import com.cineclubsalamanca.entity.DetalleMinibar;

import java.math.BigDecimal;

public record ItemMinibarResponse(
    Long productoId,
    String productoNombre,
    Integer cantidad,
    BigDecimal subtotal
) {
    public static ItemMinibarResponse from(DetalleMinibar d) {
        return new ItemMinibarResponse(d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getSubtotal());
    }
}
