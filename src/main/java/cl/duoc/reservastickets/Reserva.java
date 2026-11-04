package cl.duoc.reservastickets;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long eventoId;
    public String usuario;
    public int cantidad;
    public BigDecimal precioUnitario;
    public BigDecimal total;
    public LocalDateTime fechaCompra;

    public Reserva() {}
}
