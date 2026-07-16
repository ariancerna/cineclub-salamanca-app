package com.cineclubsalamanca.service;

import com.cineclubsalamanca.dto.reserva.CrearReservaRequest;
import com.cineclubsalamanca.dto.reserva.ReservaResponse;
import com.cineclubsalamanca.entity.*;
import com.cineclubsalamanca.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FuncionService funcionService;
    private final ProductoService productoService;
    private final FuncionRepository funcionRepository;

    @Transactional
    public ReservaResponse crear(String email, CrearReservaRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Funcion funcion = funcionService.buscarPorId(req.funcionId());

        if (funcion.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La función ya se proyectó y no admite reservas");
        }

        if (funcion.getAforoDisponible() <= 0) {
            throw new IllegalStateException("No hay butacas disponibles para esta función");
        }

        if (reservaRepository.existsByUsuarioIdAndFuncionId(usuario.getId(), funcion.getId())) {
            throw new IllegalStateException("Ya tienes una reserva para esta función");
        }

        if (reservaRepository.findByNumeroButacaAndFuncionId(req.numeroButaca(), funcion.getId()).isPresent()) {
            throw new IllegalStateException("La butaca " + req.numeroButaca() + " ya está ocupada");
        }

        String codigo = generarCodigo();

        Reserva reserva = Reserva.builder()
                .codigoReserva(codigo)
                .usuario(usuario)
                .funcion(funcion)
                .numeroButaca(req.numeroButaca())
                .fechaEmision(LocalDateTime.now())
                .build();

        if (req.itemsMinibar() != null) {
            for (var item : req.itemsMinibar()) {
                Producto producto = productoService.buscarPorId(item.productoId());
                BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.cantidad()));
                DetalleMinibar detalle = DetalleMinibar.builder()
                        .reserva(reserva)
                        .producto(producto)
                        .cantidad(item.cantidad())
                        .subtotal(subtotal)
                        .build();
                reserva.getDetallesMinibar().add(detalle);
            }
        }

        funcion.setAforoDisponible(funcion.getAforoDisponible() - 1);
        funcionRepository.save(funcion);

        return ReservaResponse.from(reservaRepository.save(reserva));
    }

    public List<ReservaResponse> listarMisReservas(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return reservaRepository.findByUsuarioIdOrderByFechaEmisionDesc(usuario.getId())
                .stream().map(ReservaResponse::from).toList();
    }

    public ReservaResponse obtenerPorCodigo(String codigo) {
        return ReservaResponse.from(buscarPorCodigo(codigo));
    }

    public List<ReservaResponse> listarPorFuncion(Long funcionId) {
        return reservaRepository.findByFuncionIdOrderByFechaEmisionAsc(funcionId)
                .stream().map(ReservaResponse::from).toList();
    }

    public List<String> butacasOcupadas(Long funcionId) {
        return reservaRepository.findButacasOcupadasByFuncionId(funcionId);
    }

    @Transactional
    public ReservaResponse confirmarIngreso(String codigo) {
        Reserva reserva = buscarPorCodigo(codigo);
        reserva.setAsistioIngreso(true);
        return ReservaResponse.from(reservaRepository.save(reserva));
    }

    private Reserva buscarPorCodigo(String codigo) {
        return reservaRepository.findById(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada: " + codigo));
    }

    private String generarCodigo() {
        String base = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "SLM-" + base;
    }
}
