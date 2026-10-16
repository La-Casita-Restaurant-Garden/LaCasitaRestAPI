
# Etapa 1: Construcción
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jdk
WORKDIR /app

ENV TZ=America/Panama

# cwebp: S3Service convierte cada imagen subida a WebP
RUN apt-get update \
    && apt-get install -y --no-install-recommends webp \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8443
ENV PORT=8443
ENTRYPOINT ["java", "-Duser.timezone=America/Panama", "-jar", "app.jar"]