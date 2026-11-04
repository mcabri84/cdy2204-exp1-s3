package cl.duoc.reservastickets;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String nombre;
    public LocalDate fecha;
    public BigDecimal precio;

    public Evento() {}
}
