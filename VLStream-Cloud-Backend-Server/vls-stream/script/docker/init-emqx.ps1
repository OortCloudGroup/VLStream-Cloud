[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot ".env"),
    [string]$BaseUri = "http://127.0.0.1:18083"
)

$ErrorActionPreference = "Stop"

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Environment file not found: $Path. Copy .env.example to .env first."
    }

    foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -le 0) {
            continue
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if (-not (Test-Path "Env:$name")) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

function Get-RequiredEnvironmentValue {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required environment variable is empty: $Name"
    }
    return $value
}

Import-DotEnv -Path $EnvFile

$dashboardPassword = Get-RequiredEnvironmentValue -Name "EMQX_DASHBOARD_PASSWORD"
$mqttUsername = Get-RequiredEnvironmentValue -Name "VLSTREAM_MQTT_USERNAME"
$mqttPassword = Get-RequiredEnvironmentValue -Name "VLSTREAM_MQTT_PASSWORD"
$BaseUri = $BaseUri.TrimEnd("/")

$ready = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        Invoke-WebRequest -Uri "$BaseUri/status" -Method Get -TimeoutSec 2 | Out-Null
        $ready = $true
        break
    }
    catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    throw "EMQX Dashboard did not become ready at $BaseUri within 60 seconds."
}

try {
    $loginBody = @{
        username = "admin"
        password = $dashboardPassword
    } | ConvertTo-Json
    $login = Invoke-RestMethod `
        -Uri "$BaseUri/api/v5/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody
}
catch {
    throw "EMQX Dashboard login failed. If /docker/emqx/data already existed, reset it with: docker exec emqx /opt/emqx/bin/emqx ctl admins passwd admin <new-password>"
}

if ([string]::IsNullOrWhiteSpace($login.token)) {
    throw "EMQX login response did not contain a bearer token."
}

$headers = @{
    Authorization = "Bearer $($login.token)"
}
$authenticatorId = "password_based%3Abuilt_in_database"
$authenticators = Invoke-RestMethod `
    -Uri "$BaseUri/api/v5/authentication" `
    -Method Get `
    -Headers $headers

$authenticatorExists = @($authenticators) |
    Where-Object { $_.id -eq "password_based:built_in_database" }
if (-not $authenticatorExists) {
    throw "The password_based:built_in_database authenticator is missing. Check EMQX_AUTHENTICATION in docker-compose.yml and the EMQX container logs."
}

$escapedUser = [Uri]::EscapeDataString($mqttUsername)
$usersUri = "$BaseUri/api/v5/authentication/$authenticatorId/users"
$userUri = "$usersUri/$escapedUser"
$userExists = $false

try {
    Invoke-RestMethod -Uri $userUri -Method Get -Headers $headers | Out-Null
    $userExists = $true
}
catch {
    $statusCode = [int]$_.Exception.Response.StatusCode
    if ($statusCode -ne 404) {
        throw
    }
}

$userBody = @{
    password = $mqttPassword
    is_superuser = $false
}
if (-not $userExists) {
    $userBody.user_id = $mqttUsername
}

$request = @{
    Headers = $headers
    ContentType = "application/json"
    Body = ($userBody | ConvertTo-Json)
}

if ($userExists) {
    Invoke-RestMethod -Uri $userUri -Method Put @request | Out-Null
    Write-Host "Updated local MQTT user '$mqttUsername'."
}
else {
    Invoke-RestMethod -Uri $usersUri -Method Post @request | Out-Null
    Write-Host "Created local MQTT user '$mqttUsername'."
}

Write-Host "EMQX 5.4 password authentication and the local MQTT user are ready."
