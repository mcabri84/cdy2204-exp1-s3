package cl.duoc.reservastickets;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketOperationsController {

    private final TicketRepository tickets;
    private final ReservaRepository reservas;
    private final EventoRepository eventos;
    private final S3StorageService storage;

    public TicketOperationsController(
            TicketRepository tickets,
            ReservaRepository reservas,
            EventoRepository eventos,
            S3StorageService storage) {
        this.tickets = tickets;
        this.reservas = reservas;
        this.eventos = eventos;
        this.storage = storage;
    }

    private Ticket buscar(String id) {
        return tickets.findById(id).orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    private List<Ticket> filtrar(Long eventoId, String usuario) {
        return tickets.findAll().stream()
            .filter(t -> eventoId == null ||
                    eventoId.equals(t.eventoId))
            .filter(t -> usuario == null ||
                    usuario.equals(t.usuario))
            .toList();
    }

    @GetMapping("/historial")
    public List<Ticket> historial(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) String usuario) {
        return filtrar(eventoId, usuario);
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable String id) {
        Ticket t = buscar(id);
        if (t.claveS3 == null)
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Ticket no almacenado en S3");

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + t.nombreArchivo + "\"")
            .body(storage.descargar(t.claveS3));
    }

    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarGrupo(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) String usuario) throws IOException {

        if (eventoId == null && (usuario == null || usuario.isBlank()))
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Indique eventoId o usuario");

        List<Ticket> lista = filtrar(eventoId, usuario).stream()
            .filter(t -> t.claveS3 != null).toList();

        if (lista.isEmpty())
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No hay tickets");

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Ticket t : lista) {
                zip.putNextEntry(new ZipEntry(t.nombreArchivo));
                zip.write(storage.descargar(t.claveS3));
                zip.closeEntry();
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"tickets.zip\"")
            .body(salida.toByteArray());
    }

    public record CambioDetalle(String detalle) {}

    @PutMapping("/{id}")
    public Ticket modificar(@PathVariable String id,
                             @RequestBody CambioDetalle cambio) {

        Ticket t = buscar(id);

        if (cambio == null || cambio.detalle() == null ||
                cambio.detalle().isBlank())
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Detalle obligatorio");

        Reserva r = reservas.findById(t.reservaId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        Evento e = eventos.findById(t.eventoId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Evento no encontrado"));

        t.detalle = cambio.detalle().substring(
            0, Math.min(120, cambio.detalle().length()));

        Path archivo = Paths.get(System.getenv()
            .getOrDefault("TICKETS_DIR", "./data/tickets"))
            .resolve(t.nombreArchivo);

        try {
            Files.createDirectories(archivo.getParent());

            try (PDDocument pdf = new PDDocument()) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                pdf.addPage(pagina);

                try (PDPageContentStream c =
                        new PDPageContentStream(pdf, pagina)) {
                    String[] lineas = {
                        "TICKET DE RESERVA",
                        "Ticket: " + t.id,
                        "Evento: " + e.nombre,
                        "Fecha: " + e.fecha,
                        "Usuario: " + t.usuario,
                        "Entradas: " + r.cantidad,
                        "Precio unitario: $" + r.precioUnitario,
                        "TOTAL: $" + r.total,
                        "Detalle: " + t.detalle
                    };

                    int y = 770;
                    for (String linea : lineas) {
                        String seguro = Normalizer.normalize(
                            linea, Normalizer.Form.NFD)
                            .replaceAll("\\p{M}", "")
                            .replaceAll("[^\\x20-\\x7E]", "?");

                        c.beginText();
                        c.setFont(new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA), 12);
                        c.newLineAtOffset(50, y);
                        c.showText(seguro);
                        c.endText();
                        y -= 30;
                    }
                }
                pdf.save(archivo.toFile());
            }

            t.claveS3 = storage.subir(t, archivo);
            return tickets.save(t);

        } catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo actualizar PDF", ex);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        Ticket t = buscar(id);

        if (t.claveS3 != null)
            storage.eliminar(t.claveS3);

        tickets.delete(t);

        try {
            Files.deleteIfExists(Paths.get(System.getenv()
                .getOrDefault("TICKETS_DIR", "./data/tickets"))
                .resolve(t.nombreArchivo));
        } catch (IOException ignored) {
            // El archivo temporal puede limpiarse posteriormente.
        }
    }
}
