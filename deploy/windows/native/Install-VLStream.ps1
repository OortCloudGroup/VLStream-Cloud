#Requires -Version 7.2
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('Install', 'Start', 'Stop')]
    [string]$Action,
    [Parameter(Mandatory)]
    [string]$InstallRoot,
    [Parameter(Mandatory)]
    [string]$DataRoot
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$InstallRoot = [IO.Path]::GetFullPath($InstallRoot)
$DataRoot = [IO.Path]::GetFullPath($DataRoot)
$ports = @{Console=17880; Vls=17780; Wvp=17781; MySql=17306; Redis=17379; Mqtt=17883; S3=17900; Zlm=17980; WebRtc=19080; Sip=18116}
$runtime = Join-Path $InstallRoot 'runtime'
$logs = Join-Path $DataRoot 'logs'
$config = Join-Path $DataRoot 'config'
$processFile = Join-Path $DataRoot 'processes.json'
$secretFile = Join-Path $config 'service-secrets.json'
$seedFile = Join-Path $DataRoot 'vls-seed-complete'
$processes = [ordered]@{}

function Write-AtomicText {
    param([string]$Path, [string]$Content)
    [IO.File]::WriteAllText("$Path.pending", $Content)
    [IO.File]::Move("$Path.pending", $Path, $true)
}

function Write-InitialText {
    param([string]$Path, [string]$Content)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Write-AtomicText $Path $Content
    }
}

function Save-Processes {
    Write-AtomicText $processFile (ConvertTo-Json -InputObject $processes -Depth 4)
}

function Get-OwnedProcess {
    param([string]$Name)
    $record = $processes[$Name]
    if ($record) {
        $process = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
        if ($process -and $process.Path -eq $record.Executable -and
            $process.StartTime.ToUniversalTime().Ticks -eq [long]$record.StartTicks) {
            return $process
        }
    }
}

function Test-LocalPort {
    param([int]$Port)
    $client = [Net.Sockets.TcpClient]::new()
    try {
        return $client.ConnectAsync('127.0.0.1', $Port).Wait(300) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Stop-Owned {
    param([string]$Name)
    $process = Get-OwnedProcess $Name
    if ($process) {
        if ($Name -eq 'mysql') {
            Stop-VlsNativeDatabase -InstallDirectory $InstallRoot -DataDirectory $DataRoot
        } elseif ($Name -eq 'nginx') {
            $prefix = (Join-Path $DataRoot 'nginx').Replace('\', '/') + '/'
            & $process.Path -p $prefix -c (Join-Path $config 'nginx.conf') -s quit | Out-Null
        } elseif ($Name -eq 'redis') {
            $prior = [Environment]::GetEnvironmentVariable('REDISCLI_AUTH', 'Process')
            try {
                $env:REDISCLI_AUTH = (Get-Content -LiteralPath $secretFile -Raw | ConvertFrom-Json).Redis
                & (Join-Path $runtime 'redis/redis-cli.exe') -h 127.0.0.1 -p $ports.Redis SHUTDOWN SAVE | Out-Null
            } finally {
                $env:REDISCLI_AUTH = $prior
            }
        }
        if (-not $process.WaitForExit(3000)) {
            Stop-Process -InputObject $process -ErrorAction Stop
            if (-not $process.WaitForExit(3000)) {
                throw "停止 $Name 超时。"
            }
        }
    }
    $processes.Remove($Name)
    Save-Processes
}

function ConvertTo-WindowsArgument {
    param([AllowEmptyString()][string]$Value)
    if ($Value -ne '' -and $Value -notmatch '[\s"]') {
        return $Value
    }
    return '"' + ($Value -replace '(\\*)"', '$1$1\"' -replace '(\\+)$', '$1$1') + '"'
}

function Start-Owned {
    param(
        [string]$Name, [string]$Executable, [string[]]$Arguments,
        [int]$Port, [string]$Directory, [hashtable]$EnvironmentVariables = @{},
        [int]$TimeoutSeconds = 45
    )
    if ($Name -in @('vls','wvp','nginx') -and (Get-OwnedProcess $Name) -and -not (Test-LocalPort $Port)) {
        Stop-Owned $Name
    }
    if (-not (Get-OwnedProcess $Name)) {
        if (Test-LocalPort $Port) {
            throw "端口 $Port 已被其他进程使用，无法启动 $Name。"
        }
        $prior = @{}
        try {
            foreach ($item in $EnvironmentVariables.GetEnumerator()) {
                $prior[$item.Key] = [Environment]::GetEnvironmentVariable($item.Key, 'Process')
                [Environment]::SetEnvironmentVariable($item.Key, [string]$item.Value, 'Process')
            }
            $launch = @{
                FilePath = $Executable
                ArgumentList = ($Arguments | ForEach-Object { ConvertTo-WindowsArgument $_ }) -join ' '
                WorkingDirectory = $Directory
                WindowStyle = 'Hidden'
                PassThru = $true
                RedirectStandardOutput = Join-Path $logs "$Name.out.log"
                RedirectStandardError = Join-Path $logs "$Name.err.log"
            }
            $process = Start-Process @launch
            $processes[$Name] = @{
                Id = $process.Id; Executable = $Executable
                StartTicks = $process.StartTime.ToUniversalTime().Ticks
            }
            Save-Processes
        } finally {
            foreach ($item in $prior.GetEnumerator()) {
                if ($null -eq $item.Value) {
                    Remove-Item -LiteralPath "Env:$($item.Key)" -ErrorAction SilentlyContinue
                } else {
                    [Environment]::SetEnvironmentVariable($item.Key, $item.Value, 'Process')
                }
            }
        }
    }
    $started = [DateTime]::UtcNow
    $deadline = $started.AddSeconds($TimeoutSeconds)
    do {
        $owned = Get-OwnedProcess $Name
        if (-not $owned -and [DateTime]::UtcNow -gt $started.AddSeconds(5)) {
            throw "$Name 启动失败。请查看 $logs。"
        }
        if ($owned -and (Test-LocalPort $Port)) { return }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Name 在端口 $Port 等待就绪超时。请查看 $logs。"
}

function Wait-Http {
    param([string]$Name, [string]$Uri, [int]$TimeoutSeconds = 120, [string]$StartupLogMarker)
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        if (-not (Get-OwnedProcess $Name)) {
            throw "$Name 已退出。请查看 $logs。"
        }
        try {
            $response = Invoke-WebRequest -Uri $Uri -TimeoutSec 3 -NoProxy
            if ($response.StatusCode -eq 200 -and (-not $StartupLogMarker -or
                (Select-String -LiteralPath (Join-Path $logs "$Name.out.log") -Pattern $StartupLogMarker -SimpleMatch -Quiet))) {
                return $response
            }
        } catch { }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Name HTTP 服务就绪超时。请查看 $logs。"
}

function Initialize-Config {
    if (-not (Test-Path -LiteralPath $secretFile -PathType Leaf)) {
        $values = @{}
        foreach ($key in @('Redis','Mqtt','S3Access','S3Secret','Zlm','VlsJwt','WvpJwt','Model','Compute','Sip','DeviceBinding')) {
            $values[$key] = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)).ToLowerInvariant()
        }
        Write-AtomicText $secretFile ($values | ConvertTo-Json)
    }
    $values = Get-Content -LiteralPath $secretFile -Raw | ConvertFrom-Json
    foreach ($directory in @('redis','s3','zlm','nginx/logs','nginx/temp','vls','wvp')) {
        New-Item -ItemType Directory -Path (Join-Path $DataRoot $directory) -Force | Out-Null
    }
    Write-InitialText (Join-Path $config 'redis.conf') @"
bind 127.0.0.1
port $($ports.Redis)
protected-mode yes
dir ../redis
appendonly yes
requirepass $($values.Redis)
"@
    $mqttPassword = Join-Path $config 'mqtt-password'
    if (-not (Test-Path -LiteralPath $mqttPassword -PathType Leaf)) {
        $pending = "$mqttPassword.pending"
        [IO.File]::WriteAllText($pending, "vlstream:$($values.Mqtt)" + [Environment]::NewLine)
        try {
            & (Join-Path $runtime 'mqtt/mosquitto_passwd.exe') -U $pending | Out-Null
            if ($LASTEXITCODE -ne 0) { throw '无法配置 Mosquitto 密码文件。' }
            [IO.File]::Move($pending, $mqttPassword)
        } finally {
            if (Test-Path -LiteralPath $pending -PathType Leaf) { Remove-Item -LiteralPath $pending }
        }
    }
    Write-InitialText (Join-Path $config 'mqtt.conf') @"
listener $($ports.Mqtt) 127.0.0.1
allow_anonymous false
password_file $($mqttPassword.Replace('\', '/'))
persistence true
persistence_location $($config.Replace('\', '/'))/
"@
    $nginx = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'nginx.template.conf') -Raw
    $nginx = $nginx.Replace('__WEB_ROOT__', (Join-Path $InstallRoot 'web').Replace('\', '/'))
    $nginx = $nginx.Replace('__NGINX_MIME__', (Join-Path $runtime 'nginx/conf/mime.types').Replace('\', '/'))
    Write-InitialText (Join-Path $config 'nginx.conf') $nginx
    $zlm = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'zlm.template.ini') -Raw
    $zlm = $zlm.Replace('__ZLM_HOOK_HOST__:8080', "127.0.0.1:$($ports.Wvp)").Replace('__ZLM_HOOK_HOST__', '127.0.0.1')
    $zlm = $zlm -replace '(?m)^secret=.*$', "secret=$($values.Zlm)"
    $zlm = $zlm -replace '(?ms)(\[http\][^\[]*?)(?m:^port=)\d+', ('$1port=' + $ports.Zlm)
    $zlm = $zlm -replace '(?m)^\[general\]\r?$', "[general]$([Environment]::NewLine)listen_ip=127.0.0.1"
    $zlm = $zlm -replace '(?m)^\[http\]\r?$', "[http]$([Environment]::NewLine)sslport=0"
    $zlm = $zlm -replace '(?m)^\[rtp\]\r?$', "[rtp]$([Environment]::NewLine)port=19400"
    $zlm += [Environment]::NewLine + @'
[rtsp]
port=18554
sslport=0
[rtmp]
port=19350
sslport=0
[rtp_proxy]
port=19400
port_range=19400-19500
[rtc]
port=19501
tcpPort=19501
[shell]
port=0
[srt]
port=0
'@
    Write-InitialText (Join-Path $config 'zlm.ini') $zlm
    return $values
}

function Install-WvpSdk {
    $relativeDirectory = 'ruoyi-isup/win-lib'
    $source = Join-Path $InstallRoot "wvp/$relativeDirectory"
    $destination = Join-Path $DataRoot "wvp/$relativeDirectory"
    foreach ($file in (Get-ChildItem -LiteralPath $source -Recurse -File -Filter '*.dll')) {
        $relative = [IO.Path]::GetRelativePath($source, $file.FullName)
        $target = Join-Path $destination $relative
        if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
            New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
            Copy-Item -LiteralPath $file.FullName -Destination $target
        }
    }
    return $destination
}

function Start-Platform {
    $bin = @{
        Java = Join-Path $runtime 'java/bin/java.exe'; MySql = Join-Path $runtime 'mysql/bin/mysqld.exe'
        Redis = Join-Path $runtime 'redis/redis-server.exe'; Mqtt = Join-Path $runtime 'mqtt/mosquitto.exe'
        S3 = Join-Path $runtime 's3/weed.exe'; Zlm = Join-Path $runtime 'zlm/MediaServer.exe'
        WebRtc = Join-Path $runtime 'webrtc/bin/webrtc-streamer.exe'; Nginx = Join-Path $runtime 'nginx/nginx.exe'
    }
    $vlsJar = Join-Path $InstallRoot 'vls/vls-stream.jar'
    $wvpJar = Join-Path $InstallRoot 'wvp/wvp.jar'
    foreach ($file in @($bin.Values) + @($vlsJar, $wvpJar, (Join-Path $InstallRoot 'web/index.html'))) {
        if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
            throw "安装包缺少 $file。请使用完整安装包修复。"
        }
    }
    $secrets = Initialize-Config
    $wvpSdk = Install-WvpSdk
    $db = Initialize-VlsNativeDatabase -InstallDirectory $InstallRoot -DataDirectory $DataRoot -MySqlPort $ports.MySql
    Start-Owned mysql $bin.MySql @("--defaults-file=$($db.MySqlConfig)") $ports.MySql (Split-Path $bin.MySql)
    Start-Owned redis $bin.Redis @('redis.conf') $ports.Redis $config
    Start-Owned mqtt $bin.Mqtt @('-c', (Join-Path $config 'mqtt.conf')) $ports.Mqtt (Split-Path $bin.Mqtt)
    $s3Args = @('mini', "-dir=$(Join-Path $DataRoot 's3')", '-ip=127.0.0.1', '-ip.bind=127.0.0.1',
        '-master.port=19333', '-volume.port=19334', '-filer.port=19335', "-s3.port=$($ports.S3)", '-bucket=vlstream')
    Start-Owned s3 $bin.S3 $s3Args $ports.S3 (Split-Path $bin.S3) @{
        AWS_ACCESS_KEY_ID=$secrets.S3Access; AWS_SECRET_ACCESS_KEY=$secrets.S3Secret
    }
    $zlmLogs = Join-Path $logs 'zlm'
    New-Item -ItemType Directory -Path $zlmLogs -Force | Out-Null
    Start-Owned zlm $bin.Zlm @('-c', (Join-Path $config 'zlm.ini'), '--log-dir', $zlmLogs, '--log-size', '8', '--log-slice', '5') $ports.Zlm (Join-Path $DataRoot 'zlm')
    Start-Owned webrtc $bin.WebRtc @('-H', "127.0.0.1:$($ports.WebRtc)") $ports.WebRtc (Split-Path $bin.WebRtc)
    $vlsUrl = "http://127.0.0.1:$($ports.Vls)"
    $bootstrap = -not (Test-Path -LiteralPath $seedFile -PathType Leaf)
    $vlsArgs = @('-Xms256m','-Xmx2048m','-Dfile.encoding=UTF-8','-jar',$vlsJar,'--spring.profiles.active=prod')
    $vlsEnv = @{
        SERVER_ADDRESS='127.0.0.1'; SERVER_PORT=$ports.Vls
        SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL="jdbc:mysql://127.0.0.1:$($ports.MySql)/$($db.VlsDatabase)?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_general_ci&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&tinyInt1isBit=false"
        SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=$db.VlsUser
        SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=$db.VlsPassword
        SPRING_REDIS_HOST='127.0.0.1'; SPRING_REDIS_PORT=$ports.Redis; SPRING_REDIS_PASSWORD=$secrets.Redis
        FLYWAY_ENABLED='true'; VLS_SINGLE_TENANT_ID='000000'; VLS_APAAS_SYNC_ENABLED='false'
        SPRING_BOOT_ADMIN_CLIENT_ENABLED='false'
        VLS_DATA_INITIALIZATION_ENABLED=$bootstrap.ToString().ToLowerInvariant()
        SPRING_APPLICATION_JSON=(@{
            token=@{'tenant-type'='single'}; 'sa-token'=@{'jwt-secret-key'=$secrets.VlsJwt}
            UnifiedMessagingSend=@{url=''}
            xxl=@{job=@{enabled=$false; admin=@{addresses=''}}}
            dept=@{excludedUdid=''}
            auth=@{report=@{excludeClasses=''}}
            synchronise=@{'DEPT-SYNURL'=''; 'JOB-SYNURL'=''; 'USER-SYNURL'=''}
        } | ConvertTo-Json -Compress -Depth 5)
        MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED='true'; MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE='health'
        VLSTREAM_NATIVE_DEVICE_LEGACY_ENABLED='false'
        VLSTREAM_MQTT_HOST='127.0.0.1'; VLSTREAM_MQTT_PORT=$ports.Mqtt; VLSTREAM_MQTT_USERNAME='vlstream'
        VLSTREAM_MQTT_PASSWORD=$secrets.Mqtt; VLSTREAM_WVP_DEVICE_BASE_URL="http://127.0.0.1:$($ports.Wvp)"
        VLSTREAM_WEBRTC_ENABLED='true'; VLSTREAM_WEBRTC_PUBLIC_URL='/bus/webrtc-streamer-server'
        VLSTREAM_WEBRTC_INTERNAL_URL="http://127.0.0.1:$($ports.WebRtc)"
        VLSTREAM_COMPUTE_ENCRYPTION_KEY=$secrets.Compute; VLSTREAM_MODEL_DOWNLOAD_SIGNING_SECRET=$secrets.Model
        VLSTREAM_MODEL_PUBLIC_BASE_URL="http://127.0.0.1:$($ports.Console)/bus/vls-server"
        VLSTREAM_DEVICE_MEDIA_OSS_CONFIG_KEY='minio-device-media'
    }
    Start-Owned vls $bin.Java $vlsArgs $ports.Vls (Join-Path $DataRoot 'vls') $vlsEnv 180
    Wait-Http vls "$vlsUrl/actuator/health/readiness" 180 | Out-Null
    if ($bootstrap) {
        Complete-VlsNativeFirstLogin -InstallDirectory $InstallRoot -DataDirectory $DataRoot -VlsBaseUri $vlsUrl | Out-Null
        Stop-Owned 'vls'
        Write-AtomicText $seedFile 'complete'
        $vlsEnv.VLS_DATA_INITIALIZATION_ENABLED = 'false'
        Start-Owned vls $bin.Java $vlsArgs $ports.Vls (Join-Path $DataRoot 'vls') $vlsEnv 180
        Wait-Http vls "$vlsUrl/actuator/health/readiness" 180 | Out-Null
    }
    $wvpEnv = @{
        SERVER_ADDRESS='127.0.0.1'; SERVER_PORT=$ports.Wvp; DB_HOST='127.0.0.1'; DB_PORT=$ports.MySql
        DB_NAME=$db.WvpDatabase; DB_USERNAME=$db.WvpUser; DB_PASSWORD=$db.WvpPassword
        DB_MIGRATION_USERNAME=$db.WvpUser; DB_MIGRATION_PASSWORD=$db.WvpPassword
        REDIS_HOST='127.0.0.1'; REDIS_PORT=$ports.Redis; REDIS_PASSWORD=$secrets.Redis
        VLSTREAM_MQTT_HOST='127.0.0.1'; VLSTREAM_MQTT_PORT=$ports.Mqtt; VLSTREAM_MQTT_ENABLED='true'
        VLSTREAM_MQTT_USERNAME='vlstream'; VLSTREAM_MQTT_PASSWORD=$secrets.Mqtt
        VLSTREAM_DEVICE_DEFAULT_TENANT_ID='000000'; VLSTREAM_DEVICE_TENANT_BINDING_SECRET=$secrets.DeviceBinding
        WVP_AUTH_MODE='LOCAL'; VLSTREAM_VERIFY_TOKEN="$vlsUrl/blade-system/user/info"; TOKEN_SECRET=$secrets.WvpJwt
        SIP_IP='127.0.0.1'; SIP_PORT=$ports.Sip; SIP_PASSWORD=$secrets.Sip
        ZLM_HOST='127.0.0.1'; ZLM_HTTP_PORT=$ports.Zlm
        ZLM_HOOK_IP='127.0.0.1'; ZLM_PUBLIC_HOST='127.0.0.1'; ZLM_SECRET=$secrets.Zlm
        ZLM_RTP_PORT_RANGE='19400,19500'; ZLM_RTP_SEND_PORT_RANGE='19400,19500'
        PATH="$wvpSdk;$env:PATH"
        LOGGING_LEVEL_COM_RUOYI='INFO'
        SPRING_APPLICATION_JSON=(@{
            isup=@{
                IP='127.0.0.1'; cmsServer=@{Port=19660}
                smsServer=@{Port=19665; ListenIP='127.0.0.1'; ListenPort=19665}
                ssService=@{Port=19661}; alarmService=@{Port=19663}
                picServer=@{Port=19601}; smsBackServerListe=@{Port=19800}
            }
        } | ConvertTo-Json -Compress -Depth 5)
    }
    Start-Owned wvp $bin.Java @('-Xms256m','-Xmx1024m','-Dfile.encoding=UTF-8','-jar',$wvpJar) $ports.Wvp (Join-Path $DataRoot 'wvp') $wvpEnv 180
    Wait-Http wvp "http://127.0.0.1:$($ports.Wvp)/" -StartupLogMarker 'ruoyi-wvp启动成功' | Out-Null
    $prefix = (Join-Path $DataRoot 'nginx').Replace('\', '/') + '/'
    Start-Owned nginx $bin.Nginx @('-p',$prefix,'-c',(Join-Path $config 'nginx.conf'),'-g','daemon off;') $ports.Console (Split-Path $bin.Nginx)
    $console = "http://127.0.0.1:$($ports.Console)"
    Wait-Http nginx "$console/bus/vls-ui/" | Out-Null
    $mode = Wait-Http nginx "$console/bus/vls-server/sso/v1/mode"
    if (($mode.Content | ConvertFrom-Json).data.tenantType -ne 'single') {
        throw 'VLS API 未返回单租户状态。'
    }
    Write-Host "VLStream 已就绪。$console/bus/vls-ui/"
}

if ($Action -eq 'Stop' -and -not (Test-Path -LiteralPath $processFile -PathType Leaf)) {
    return
}
New-Item -ItemType Directory -Path $DataRoot, $logs, $config -Force | Out-Null
$lock = [IO.File]::Open((Join-Path $DataRoot 'instance.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
try {
    if (Test-Path -LiteralPath $processFile -PathType Leaf) {
        $processes = Get-Content -LiteralPath $processFile -Raw | ConvertFrom-Json -AsHashtable
    }
    . (Join-Path $PSScriptRoot 'Database.ps1')
    if ($Action -eq 'Stop') {
        foreach ($name in @('nginx','wvp','vls','webrtc','zlm','s3','mqtt','redis','mysql')) {
            Stop-Owned $name
        }
        Write-Host 'VLStream 服务已停止，数据已保留。'
    } else {
        Start-Platform
    }
} catch {
    Write-Host "VLStream 服务操作失败。请查看 $logs 后重试。" -ForegroundColor Yellow
    Write-Error -ErrorRecord $_ -ErrorAction Continue
    exit 1
} finally {
    $lock.Dispose()
}
