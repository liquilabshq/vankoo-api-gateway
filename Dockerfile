# Etapa 1: Build
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
# Instalamos curl para el healthcheck de Docker
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring
COPY --from=build /app/target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]