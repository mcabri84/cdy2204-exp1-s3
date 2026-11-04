package cl.duoc.reservastickets;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ReservasController {

    private final EventoRepository eventos;
    private final ReservaRepository reservas;

    public ReservasController(EventoRepository eventos,
                              ReservaRepository reservas) {
        this.eventos = eventos;
        this.reservas = reservas;
    }

    @GetMapping("/eventos")
    public List<Evento> listarEventos() {
        return eventos.findAll();
    }

    @PostMapping("/eventos")
    public ResponseEntity<Evento> crearEvento(@RequestBody Evento evento) {
        if (evento.nombre == null || evento.nombre.isBlank()
                || evento.fecha == null || evento.precio == null
                || evento.precio.signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Datos del evento invalidos");
        }
        evento.id = null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventos.save(evento));
    }

    public record Compra(Long eventoId, String usuario, Integer cantidad) {}

    @PostMapping("/reservas")
    public ResponseEntity<Reserva> comprar(@RequestBody Compra compra) {
        if (compra.eventoId() == null || compra.usuario() == null
                || compra.usuario().isBlank() || compra.cantidad() == null
                || compra.cantidad() <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Compra invalida");
        }

        Evento evento = eventos.findById(compra.eventoId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Evento no encontrado"));

        Reserva reserva = new Reserva();
        reserva.eventoId = evento.id;
        reserva.usuario = compra.usuario();
        reserva.cantidad = compra.cantidad();
        reserva.precioUnitario = evento.precio;
        reserva.total = evento.precio.multiply(
                BigDecimal.valueOf(compra.cantidad()));
        reserva.fechaCompra = LocalDateTime.now();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservas.save(reserva));
    }

    @GetMapping("/eventos/{id}/estadisticas")
    public Map<String, Object> estadisticas(@PathVariable Long id) {
        if (!eventos.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Evento no encontrado");
        }

        List<Reserva> compras = reservas.findByEventoId(id);
        int entradas = compras.stream()
                .mapToInt(r -> r.cantidad).sum();
        BigDecimal ventas = compras.stream()
                .map(r -> r.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
            "eventoId", id,
            "reservas", compras.size(),
            "entradasVendidas", entradas,
            "totalVentas", ventas
        );
    }
}
