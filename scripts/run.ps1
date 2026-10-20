param([ValidateSet('up','init','credentials','stop','logs')][string]$Action = 'up')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Install Docker Desktop with Compose v2 first.' }
docker compose version | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose is unavailable.' }
if (-not (Test-Path .env)) {
  function New-LocalPassword {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
  }
  $configuration = @"
DB_USERNAME=salary
DB_PASSWORD=$(New-LocalPassword)
HR_USERNAME=hr
HR_PASSWORD=$(New-LocalPassword)
APP_PORT=8080
COOKIE_SECURE=false
DEMO_SEED=true
"@
  [IO.File]::WriteAllText((Join-Path (Get-Location) '.env'), $configuration + "`n", [Text.UTF8Encoding]::new($false))
  Write-Host 'Created local configuration in .env; keep it private.'
}
switch ($Action) {
  'init' { Write-Host 'Local configuration is ready.' }
  'credentials' { Get-Content .env | Where-Object { $_ -match '^HR_(USERNAME|PASSWORD)=' } }
  'stop' { docker compose down }
  'logs' { docker compose logs --tail=100 -f app }
  'up' {
    docker compose up --build --detach --wait --wait-timeout 180 app
    if ($LASTEXITCODE -ne 0) { throw 'Application startup failed. Inspect docker compose logs.' }
    $port = if ($env:APP_PORT) { $env:APP_PORT } else { ((Get-Content .env | Where-Object { $_ -match '^APP_PORT=' }) -split '=', 2)[1] }
    Write-Host "Salary workspace is ready: http://localhost:$port"
    Write-Host 'Run .\scripts\run.ps1 credentials for your local HR sign-in.'
  }
}
if ($LASTEXITCODE -ne 0) { throw 'Docker command failed.' }
