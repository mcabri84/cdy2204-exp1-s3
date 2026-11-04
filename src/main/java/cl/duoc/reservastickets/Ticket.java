package cl.duoc.reservastickets;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Ticket {
    @Id
    public String id;
    public Long reservaId;
    public Long eventoId;
    public String usuario;
    public String nombreArchivo;
    public String detalle;
    public String claveS3;
    public LocalDateTime fechaGeneracion;

    public Ticket() {}
}
