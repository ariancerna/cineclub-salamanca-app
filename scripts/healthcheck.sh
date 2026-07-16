#!/usr/bin/env bash
# Consulta /actuator/health y devuelve un codigo de salida para cron o alertas.
# Salida: 0 = UP, 1 = DOWN o inaccesible.
#
# Uso:  ./scripts/healthcheck.sh
#       URL_BASE=https://cineclub.example.com ./scripts/healthcheck.sh

set -uo pipefail

URL_BASE="${URL_BASE:-http://localhost:8080}"
ENDPOINT="${URL_BASE}/actuator/health"
TIEMPO_MAX="${TIEMPO_MAX:-10}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

RESPUESTA="$(curl --silent --show-error --max-time "${TIEMPO_MAX}" "${ENDPOINT}" 2>&1)"
CODIGO_CURL=$?

if [[ ${CODIGO_CURL} -ne 0 ]]; then
    log "CRITICO: la aplicacion no responde en ${ENDPOINT} (curl=${CODIGO_CURL}) — ${RESPUESTA}"
    exit 1
fi

# Respuesta: {"status":"UP",...}
ESTADO="$(echo "${RESPUESTA}" | grep -o '"status":"[A-Z_]*"' | head -1 | cut -d'"' -f4)"

case "${ESTADO}" in
    UP)
        log "OK: la aplicacion responde UP."
        exit 0
        ;;
    "")
        log "CRITICO: respuesta no reconocida de ${ENDPOINT} — ${RESPUESTA}"
        exit 1
        ;;
    *)
        log "CRITICO: la aplicacion reporta estado ${ESTADO} — ${RESPUESTA}"
        exit 1
        ;;
esac
