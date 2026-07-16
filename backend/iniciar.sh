#!/bin/bash

# Cargue las variables del archivo .env
if [ -f "../.env" ]; then
    export $(grep -v '^#' ../.env | xargs)
    echo "Variables de entorno cargadas desde .env"
    echo ""
fi

# Inicie el backend
echo "Iniciando CineClub Salamanca Backend..."
echo "Backend en: http://localhost:8080"
echo "Swagger en: http://localhost:8080/swagger-ui/index.html"
echo ""

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
