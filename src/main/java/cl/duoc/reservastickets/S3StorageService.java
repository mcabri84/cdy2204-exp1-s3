
package cl.duoc.reservastickets;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import java.nio.file.Path;

@Service
public class S3StorageService {

    private final S3Client cliente = S3Client.builder()
            .region(Region.US_EAST_1).build();

    public String subir(Ticket ticket, Path archivo) {
        String bucket = System.getenv("S3_BUCKET");

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Falta configurar S3_BUCKET");
        }

        String usuario = ticket.usuario
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        String clave = "evento" + ticket.eventoId + "/"
                + usuario + "/" + ticket.nombreArchivo;

        cliente.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType("application/pdf")
                .build(),
            RequestBody.fromFile(archivo)
        );

        return clave;
    }

    public byte[] descargar(String clave) {
        return cliente.getObjectAsBytes(
            software.amazon.awssdk.services.s3.model.GetObjectRequest
                .builder().bucket(bucket()).key(clave).build()
        ).asByteArray();
    }

    public void eliminar(String clave) {
        cliente.deleteObject(
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest
                .builder().bucket(bucket()).key(clave).build()
        );
    }

    private String bucket() {
        String nombre = System.getenv("S3_BUCKET");
        if (nombre == null || nombre.isBlank())
            throw new IllegalStateException("Falta S3_BUCKET");
        return nombre;
    }

}
