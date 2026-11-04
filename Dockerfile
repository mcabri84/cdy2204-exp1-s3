FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/reservas-tickets-0.0.1-SNAPSHOT.jar app.jar

ENV TICKETS_DIR=/tickets

EXPOSE 8080

ENTRYPOINT ["java","-XX:MaxRAMPercentage=60.0","-jar","app.jar"]
