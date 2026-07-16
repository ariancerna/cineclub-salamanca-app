# ==========================================================================
#  CineClub Salamanca — Imagen del backend (Spring Boot)
# --------------------------------------------------------------------------
#  Construccion multietapa: la etapa 'build' compila con Maven y la etapa
#  final solo conserva el JAR sobre un JRE. Asi la imagen publicada no
#  incluye ni el codigo fuente ni Maven ni el JDK completo.
# ==========================================================================

# ---------- Etapa 1: construccion con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Las dependencias se resuelven en una capa aparte: mientras el pom.xml no
# cambie, Docker reutiliza esta capa y la compilacion no vuelve a descargarlas.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

# Las pruebas ya se ejecutan en el pipeline; aqui solo se empaqueta.
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: imagen de ejecucion ----------
FROM eclipse-temurin:21-jre-alpine

# curl es necesario para el HEALTHCHECK
RUN apk add --no-cache curl

# La aplicacion no necesita privilegios de root: se ejecuta con un usuario propio
RUN addgroup -S cineclub && adduser -S cineclub -G cineclub

WORKDIR /app

COPY --from=build /build/target/backend-*.jar app.jar

# Directorio de logs, propiedad del usuario sin privilegios
RUN mkdir -p /app/logs && chown -R cineclub:cineclub /app

USER cineclub

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

# Docker consulta la sonda de salud de Actuator para saber si el contenedor sirve trafico
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
