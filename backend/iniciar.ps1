# Script para iniciar el backend con variables de entorno desde .env

# Cargue las variables del archivo .env
$envFile = "..\\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "Cargado: $key"
        }
    }
    Write-Host ""
}

# Inicie el backend
Write-Host "Iniciando CineClub Salamanca Backend..."
Write-Host "Backend en: http://localhost:8080"
Write-Host "Swagger en: http://localhost:8080/swagger-ui/index.html"
Write-Host ""

$mavenPath = "$PSScriptRoot\mvnw.cmd"
& $mavenPath spring-boot:run -Dspring-boot.run.profiles=dev
