# CDY2204 - Actividad Sumativa 1
## Sistema Cloud Native de Reservas y Tickets

### 1. Descripción

Aplicación REST desarrollada en Java y Spring Boot para gestionar
eventos, reservas y tickets electrónicos en formato PDF.

La solución utiliza servicios de AWS para el almacenamiento de
tickets y Docker para su ejecución en la nube.

### 2. Tecnologías utilizadas

- Java 17
- Spring Boot 4.0.9
- Maven
- Apache PDFBox
- H2 Database
- Docker
- AWS EC2
- AWS Elastic File System (EFS)
- AWS S3
- GitHub Actions
- Docker Hub
- Postman

### 3. Arquitectura

Cliente Postman
      |
      v
API REST Spring Boot
      |
      v
Docker en AWS EC2
      |
      +-- H2: eventos, reservas y tickets
      |
      +-- AWS EFS: almacenamiento de archivos PDF
      |
      +-- AWS S3: almacenamiento de tickets por evento y usuario

Los tickets PDF se generan en el directorio montado desde EFS
y se almacenan en S3 mediante la integración de la aplicación.

Estructura utilizada en S3:

eventoN/usuario/ticket-uuid.pdf

### 4. Endpoints REST

| Método | Endpoint | Función |
|---|---|---|
| GET | /api/eventos | Listar eventos |
| POST | /api/eventos | Crear evento |
| POST | /api/reservas | Registrar reserva |
| GET | /api/eventos/{id}/estadisticas | Consultar estadísticas |
| POST | /api/tickets/generar/{reservaId} | Generar PDF y subir a S3 |
| GET | /api/tickets/historial | Consultar historial |
| PUT | /api/tickets/{id} | Modificar ticket |
| GET | /api/tickets/{id}/descargar | Descargar PDF |
| GET | /api/tickets/descargar?usuario=nombre | Descargar ZIP |
| DELETE | /api/tickets/{id} | Eliminar ticket |

### 5. Infraestructura AWS

Región: us-east-1

- EC2: instancia t3.small con Docker.
- EFS: duoc-cdy2204-efs.
- S3: duoc-cdy2204-tickets-763911473229-20261104.
- IAM: LabInstanceProfile para acceso a servicios AWS.

El bucket S3 tiene el acceso público bloqueado y versionado habilitado.

### 6. Integración continua y despliegue

El workflow de GitHub Actions realiza:

1. Obtención del código fuente.
2. Compilación de la aplicación Java.
3. Construcción de la imagen Docker.
4. Publicación de la imagen en Docker Hub.
5. Despliegue en EC2 mediante un runner autoalojado.

Docker Hub:
https://hub.docker.com/r/mcabri/cdy2204-exp1-s3

GitHub:
https://github.com/mcabri84/cdy2204-exp1-s3

### 7. Pruebas realizadas

Se utilizó una colección de Postman con 10 solicitudes REST.

Resultados de la ejecución:

- Pruebas aprobadas: 10
- Pruebas fallidas: 0
- Errores: 0

La colección está incluida en las evidencias como
CDY2204_Postman.json.

También se verificaron:

- Archivo PDF almacenado en AWS S3.
- Sistema EFS montado en EC2 mediante NFS.
- Archivo PDF presente en el directorio EFS.
- Contenedor Docker ejecutándose en EC2.
- Publicación de imágenes en Docker Hub.
- Workflow GitHub Actions ejecutado exitosamente.

### 8. Evidencias

01_Postman_10_Pruebas.png
02_GitHub_Actions.png
03_AWS_S3.png
04_AWS_EFS.png
05_EFS_PDF_EC2.png
06_Docker_EC2.png
07_Docker_Hub.png
CDY2204_Postman.json

### 9. Consideraciones

El laboratorio AWS Academy utiliza credenciales temporales.
Al reiniciar el laboratorio puede ser necesario actualizar
las credenciales y verificar el estado de la instancia EC2.

El acceso externo a la API durante las pruebas se realizó
mediante AWS Systems Manager Session Manager con un túnel
hacia el puerto 8080 del contenedor.

### 10. Entrega

Proyecto desarrollado para la asignatura CDY2204 de Duoc UC.

La entrega se complementa con documentación, evidencias
y un video demostrativo de las funcionalidades.
