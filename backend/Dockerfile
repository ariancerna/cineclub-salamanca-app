# Imagen del backend. Construccion multietapa: la etapa build compila con Maven y la
# imagen final solo lleva el JAR sobre un JRE, sin codigo fuente ni JDK.

# ---------- Etapa 1: construccion ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# El pom se copia antes que src para que Docker cachee las dependencias
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

# Las pruebas ya corren en el pipeline
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre-alpine

# curl lo necesita el HEALTHCHECK. tzdata es obligatorio en Alpine para que TZ funcione:
# sin el paquete, la JVM ignora la variable y se queda en UTC.
RUN apk add --no-cache curl tzdata

RUN addgroup -S cineclub && adduser -S cineclub -G cineclub

WORKDIR /app

COPY --from=build /build/target/backend-*.jar app.jar

RUN mkdir -p /app/logs && chown -R cineclub:cineclub /app

USER cineclub

EXPOSE 8080

# Las fechas de las funciones se guardan como LocalDateTime, sin zona. El contenedor debe
# correr en la hora del cineclub o una funcion de las 19:30 se daria por pasada a las 14:30.
ENV TZ=America/Lima \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
