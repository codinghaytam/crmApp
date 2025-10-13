# Dockerfile for Spring Boot
# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables for PostgreSQL
ENV SPRING_DATASOURCE_URL="jdbc:postgresql://ep-restless-surf-a9j3buon-pooler.gwc.azure.neon.tech/neondb?user=neondb_owner&password=npg_CVg2wv1lNtcT&sslmode=require&channelBinding=require"


EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

