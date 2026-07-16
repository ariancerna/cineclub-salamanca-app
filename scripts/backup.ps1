# Respaldo de la base de datos en Windows. Equivalente a backup.sh.
#
# Uso: .\scripts\backup.ps1
# Programar:
#   schtasks /create /tn "CineClub Backup" /tr "powershell -File C:\ruta\scripts\backup.ps1" /sc daily /st 02:00

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
$DirBackups = Join-Path $Raiz "backups"
$RetencionDias = if ($env:RETENCION_DIAS) { [int]$env:RETENCION_DIAS } else { 30 }
$Contenedor = if ($env:CONTENEDOR_DB) { $env:CONTENEDOR_DB } else { "cineclub-db" }

function Write-Log($Mensaje) {
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Mensaje"
}

$RutaEnv = Join-Path $Raiz ".env"
if (-not (Test-Path $RutaEnv)) {
    Write-Log "ERROR: no se encontro $RutaEnv"
    exit 1
}

Get-Content $RutaEnv | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        Set-Item -Path "env:$($Matches[1].Trim())" -Value $Matches[2].Trim()
    }
}

if (-not $env:POSTGRES_DB -or -not $env:POSTGRES_USER) {
    Write-Log "ERROR: faltan POSTGRES_DB o POSTGRES_USER en .env"
    exit 1
}

if (-not (Test-Path $DirBackups)) {
    New-Item -ItemType Directory -Path $DirBackups | Out-Null
}

$EnEjecucion = docker ps --format '{{.Names}}'
if ($EnEjecucion -notcontains $Contenedor) {
    Write-Log "ERROR: el contenedor '$Contenedor' no esta en ejecucion."
    exit 1
}

$Marca = Get-Date -Format "yyyyMMdd_HHmmss"
$Destino = Join-Path $DirBackups "cineclub_$Marca.sql.gz"

Write-Log "Iniciando respaldo de la base '$($env:POSTGRES_DB)'..."

# Se comprime dentro del contenedor para no depender de gzip en Windows
docker exec $Contenedor sh -c "pg_dump --username=$($env:POSTGRES_USER) --dbname=$($env:POSTGRES_DB) --clean --if-exists | gzip" `
    | Set-Content -Path $Destino -Encoding Byte

# Un volcado vacio significa que pg_dump fallo sin avisar
if (-not (Test-Path $Destino) -or (Get-Item $Destino).Length -eq 0) {
    Write-Log "ERROR: el respaldo quedo vacio. Se elimina el archivo."
    Remove-Item $Destino -ErrorAction SilentlyContinue
    exit 1
}

$TamanoKB = [math]::Round((Get-Item $Destino).Length / 1KB, 1)
Write-Log "Respaldo completado: $Destino ($TamanoKB KB)"

$Limite = (Get-Date).AddDays(-$RetencionDias)
$Antiguos = Get-ChildItem -Path $DirBackups -Filter "cineclub_*.sql.gz" | Where-Object { $_.LastWriteTime -lt $Limite }
foreach ($Archivo in $Antiguos) {
    Remove-Item $Archivo.FullName
    Write-Log "Eliminado por retencion: $($Archivo.Name)"
}

$Vigentes = (Get-ChildItem -Path $DirBackups -Filter "cineclub_*.sql.gz").Count
Write-Log "Retencion ($RetencionDias dias): $($Antiguos.Count) eliminado(s). Respaldos vigentes: $Vigentes"
