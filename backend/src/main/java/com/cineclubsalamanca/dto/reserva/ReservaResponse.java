package com.cineclubsalamanca.dto.reserva;

import com.cineclubsalamanca.dto.funcion.FuncionResponse;
import com.cineclubsalamanca.entity.Reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
    String codigoReserva,
    String usuarioNombre,
    String usuarioEmail,
    FuncionResponse funcion,
    String numeroButaca,
    LocalDateTime fechaEmision,
    Boolean asistioIngreso,
    List<ItemMinibarResponse> itemsMinibar,
    BigDecimal totalMinibar
) {
    public static ReservaResponse from(Reserva r) {
        List<ItemMinibarResponse> items = r.getDetallesMinibar().stream()
                .map(ItemMinibarResponse::from).toList();

        BigDecimal total = items.stream()
                .map(ItemMinibarResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReservaResponse(
                r.getCodigoReserva(),
                r.getUsuario().getNombre(),
                r.getUsuario().getEmail(),
                FuncionResponse.from(r.getFuncion()),
                r.getNumeroButaca(),
                r.getFechaEmision(),
                r.getAsistioIngreso(),
                items,
                total
        );
    }
}
