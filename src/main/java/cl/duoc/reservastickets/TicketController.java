package cl.duoc.reservastickets;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.text.Normalizer;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final ReservaRepository reservas;
    private final EventoRepository eventos;
    private final TicketRepository tickets;
    private final S3StorageService almacenamiento;

    public TicketController(ReservaRepository reservas,
                            EventoRepository eventos,
                            TicketRepository tickets,
                            S3StorageService almacenamiento) {
        this.reservas = reservas;
        this.eventos = eventos;
        this.tickets = tickets;
        this.almacenamiento = almacenamiento;
    }

    @PostMapping("/generar/{reservaId}")
    public ResponseEntity<Ticket> generar(@PathVariable Long reservaId) {

        Reserva reserva = reservas.findById(reservaId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        Evento evento = eventos.findById(reserva.eventoId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Evento no encontrado"));

        Ticket ticket = new Ticket();
        ticket.id = UUID.randomUUID().toString();
        ticket.reservaId = reserva.id;
        ticket.eventoId = evento.id;
        ticket.usuario = reserva.usuario;
        ticket.nombreArchivo = "ticket-" + ticket.id + ".pdf";
        ticket.fechaGeneracion = LocalDateTime.now();

        Path carpeta = Paths.get(System.getenv()
            .getOrDefault("TICKETS_DIR", "./data/tickets"))
            .toAbsolutePath().normalize();

        Path archivo = carpeta.resolve(ticket.nombreArchivo);

        try {
            Files.createDirectories(carpeta);

            try (PDDocument pdf = new PDDocument()) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                pdf.addPage(pagina);

                try (PDPageContentStream contenido =
                        new PDPageContentStream(pdf, pagina)) {

                    linea(contenido, "TICKET DE RESERVA", 770);
                    linea(contenido, "Ticket: " + ticket.id, 730);
                    linea(contenido, "Evento: " + evento.nombre, 700);
                    linea(contenido, "Fecha: " + evento.fecha, 670);
                    linea(contenido, "Usuario: " + reserva.usuario, 640);
                    linea(contenido, "Entradas: " + reserva.cantidad, 610);
                    linea(contenido, "Precio unitario: $" +
                          reserva.precioUnitario, 580);
                    linea(contenido, "TOTAL: $" + reserva.total, 550);
                }

                pdf.save(archivo.toFile());
            }

            ticket.claveS3 = almacenamiento.subir(ticket, archivo);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(tickets.save(ticket));

        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo generar el PDF", e);
        }
    }

    private static void linea(PDPageContentStream contenido,
                              String texto, int altura) throws IOException {
        String seguro = Normalizer.normalize(
            texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^\\x20-\\x7E]", "?");

        contenido.beginText();
        contenido.setFont(
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contenido.newLineAtOffset(50, altura);
        contenido.showText(seguro);
        contenido.endText();
    }
}
