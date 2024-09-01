# Usa l'immagine Maven come builder
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app

# Copia i file del progetto nella directory di lavoro
COPY . .

# Compila l'applicazione
RUN mvn clean package -DskipTests

# Usa l'immagine OpenJDK per il runtime
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copia il JAR dell'applicazione dalla fase di build
COPY --from=build /app/target/videoconference-p2p-0.0.1-SNAPSHOT.jar app.jar

# Espone la porta su cui gira l'applicazione
EXPOSE 8080

# Comando per eseguire l'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
