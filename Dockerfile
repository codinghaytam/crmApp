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

# Allow the SPRING_DATASOURCE_URL to be passed as a build-arg or runtime env variable
ARG SPRING_DATASOURCE_URL
ENV SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
