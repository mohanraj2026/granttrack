<#
  Launches the full GrantTrack microservices stack, each in its own PowerShell window,
  in the correct start order:
    eureka-server -> core-service (migrates DB) -> auth/notification/finance -> api-gateway

  Prerequisites: JDK 21, Maven 3.9+, MySQL 8 running locally with a reachable `granttrack` DB.
  Usage:  ./run-all.ps1            (uses bundled dev secrets from each application-local.yml)

  Stop everything by closing the spawned windows (or Ctrl+C in each).
#>

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

function Start-Service([string]$module, [int]$waitSeconds) {
    Write-Host "Starting $module ..." -ForegroundColor Cyan
    Start-Process -FilePath "powershell" -ArgumentList @(
        "-NoExit", "-Command",
        "cd '$root'; Write-Host '=== $module ===' -ForegroundColor Green; mvn -pl $module spring-boot:run"
    ) | Out-Null
    if ($waitSeconds -gt 0) {
        Write-Host "  waiting ${waitSeconds}s for $module to come up..." -ForegroundColor DarkGray
        Start-Sleep -Seconds $waitSeconds
    }
}

# 1) Service registry (give it time to accept registrations)
Start-Service "eureka-server" 25
# 2) Core migrates the shared schema + seeds roles; let it finish before the rest validate
Start-Service "core-service" 40
# 3) The three microservices
Start-Service "auth-service" 5
Start-Service "notification-service" 5
Start-Service "finance-service" 5
# 4) Gateway last
Start-Service "api-gateway" 0

Write-Host ""
Write-Host "All services launching. Check the Eureka dashboard at http://localhost:8761" -ForegroundColor Yellow
Write-Host "Gateway (frontend target): http://localhost:8080" -ForegroundColor Yellow
