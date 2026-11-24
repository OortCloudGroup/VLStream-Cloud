#Requires -Version 7.0
[CmdletBinding(DefaultParameterSetName = 'Install')]
param(
    [Parameter(Mandatory, ParameterSetName = 'Install')]
    [string]$ReleaseDirectory,

    [Parameter(Mandatory)]
    [string]$DataDirectory,

    [Parameter(ParameterSetName = 'Install')]
    [ValidateRange(1024, 65533)]
    [int]$Port = 17880,

    [Parameter(ParameterSetName = 'Install')]
    [ValidatePattern('^[a-z0-9][a-z0-9-]{0,30}$')]
    [string]$ProjectPrefix = 'vlstream-desktop',

    [Parameter(ParameterSetName = 'Install')]
    [switch]$ConfigureOnly,

    [Parameter(Mandatory, ParameterSetName = 'Stop')]
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$DataDirectory = [IO.Path]::GetFullPath($DataDirectory)
$cloudDirectory = Join-Path $DataDirectory 'VLStream-Cloud-v1.2.12'
$wvpDirectory = Join-Path $DataDirectory 'VLStream-wvp-lite-v1.0.12'
$ownerFile = Join-Path $DataDirectory '.compose-project'
$initializedFile = Join-Path $DataDirectory '.cloud-initialized'

function Invoke-Docker {
    param([string[]]$Arguments, [string]$Action, [string]$StandardInput)

    if ($StandardInput) {
        $StandardInput | & $dockerPath @Arguments
    } else {
        & $dockerPath @Arguments
    }
    if ($LASTEXITCODE -ne 0) {
        throw "$Action 失败，Docker 退出码 $LASTEXITCODE。"
    }
}

function Invoke-Compose {
    param([string]$Directory, [string[]]$Arguments, [string]$Action, [string]$StandardInput)

    $composeArguments = @(
        'compose', '--project-directory', $Directory,
        '--env-file', (Join-Path $Directory '.env'),
        '-f', (Join-Path $Directory 'compose.yaml'),
        '-f', (Join-Path $Directory 'compose.override.yaml')
    )
    Invoke-Docker ($composeArguments + $Arguments) $Action $StandardInput
}

function New-Secret {
    $bytes = [byte[]]::new(24)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}

function Install-ReleaseFiles {
    param([string]$Destination, [string]$OverrideName, [string]$ExpectedSha256)

    if (-not (Test-Path -LiteralPath (Join-Path $Destination 'compose.yaml') -PathType Leaf)) {
        if (Test-Path -LiteralPath $Destination) {
            throw "现有发布目录 $Destination 中缺少 compose.yaml，请从同版本发布包恢复文件。"
        }
        $releaseName = Split-Path -Leaf $Destination
        $archive = Join-Path $ReleaseDirectory "$releaseName.zip"
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $ExpectedSha256) {
            throw "发布包 $releaseName.zip 的 SHA-256 校验失败。"
        }
        $temporaryDirectory = Join-Path $DataDirectory ('.extract-' + [guid]::NewGuid().ToString('N'))
        Expand-Archive -LiteralPath $archive -DestinationPath $temporaryDirectory
        $extracted = Join-Path $temporaryDirectory $releaseName
        if (-not (Test-Path -LiteralPath (Join-Path $extracted 'compose.yaml') -PathType Leaf)) {
            throw "发布包 $releaseName.zip 中缺少 compose.yaml。"
        }
        Move-Item -LiteralPath $extracted -Destination $Destination
        Remove-Item -LiteralPath $temporaryDirectory
    }

    $installedOverride = Join-Path $Destination 'compose.override.yaml'
    if (-not (Test-Path -LiteralPath $installedOverride -PathType Leaf)) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $OverrideName) -Destination $installedOverride
    }
}

function Write-DeploymentEnvironment {
    param([string]$Directory, [System.Collections.IDictionary]$Values)

    $path = Join-Path $Directory '.env'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $pending = Join-Path $Directory '.env.pending'
        $Values.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } |
            Set-Content -LiteralPath $pending -Encoding utf8NoBOM
        Move-Item -LiteralPath $pending -Destination $path
    }
}

function Clear-InheritedComposeVariables {
    param([string[]]$Directories)

    $names = @('COMPOSE_FILE', 'COMPOSE_PROFILES', 'COMPOSE_PROJECT_NAME')
    foreach ($directory in $Directories) {
        foreach ($file in @('compose.yaml', 'compose.override.yaml')) {
            $yaml = Get-Content -LiteralPath (Join-Path $directory $file) -Raw
            $names += [regex]::Matches($yaml, '(?<!\$)\$\{([A-Z][A-Z0-9_]+)') |
                ForEach-Object { $_.Groups[1].Value }
        }
    }
    foreach ($name in ($names | Sort-Object -Unique)) {
        Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
    }
}

function Assert-EngineReady {
    $platform = (Invoke-Docker @('info', '--format', '{{.OSType}}/{{.Architecture}}') '检查 Docker 引擎' | Out-String).Trim()
    if ($platform -notmatch '^linux/(x86_64|amd64)$') {
        throw "需要 Docker Desktop 的 Linux x86-64 容器引擎，当前为 $platform。"
    }
}

function Assert-InitialAdministrator {
    $client = 'MYSQL_PWD=$MYSQL_PASSWORD exec mysql --protocol=socket --batch --skip-column-names --user=$MYSQL_USER --database=$MYSQL_DATABASE'
    $query = "SELECT COUNT(*) FROM sys_user WHERE user_name = 'admin' AND tenant_id = '000000' AND del_flag = '0';"
    $count = (Invoke-Compose $cloudDirectory @('exec', '-T', 'mysql', 'sh', '-c', $client) '核对 VLS 初始管理员' $query |
        Out-String).Trim()
    if ($count -notmatch '^[1-9]\d*$') {
        throw 'VLS 未生成单租户初始管理员，请检查后端初始化日志。'
    }
}

function Initialize-DeploymentOwnership {
    if (Test-Path -LiteralPath $ownerFile -PathType Leaf) {
        if ((Get-Content -LiteralPath $ownerFile -Raw).Trim() -ne $ProjectPrefix) {
            throw '该服务数据目录已绑定其他 Compose 项目。请使用安装时的项目名重试。'
        }
        return
    }

    foreach ($project in @("$ProjectPrefix-wvp", "$ProjectPrefix-cloud")) {
        $label = "label=com.docker.compose.project=$project"
        $containers = Invoke-Docker @('ps', '-aq', '--filter', $label) '检查是否存在同名容器'
        $volumes = Invoke-Docker @('volume', 'ls', '-q', '--filter', $label) '检查是否存在同名数据卷'
        if ($containers -or $volumes) {
            throw "Docker 项目 $project 已被使用。请指定独立的服务数据目录和 -ProjectPrefix。"
        }
    }
    Set-Content -LiteralPath $ownerFile -Value $ProjectPrefix -Encoding utf8NoBOM
}

function Initialize-DesktopNetwork {
    param([string]$NetworkName)

    $networks = Invoke-Docker @('network', 'ls', '--format', '{{.Name}}') '检查 VLStream 容器网络'
    if ($NetworkName -notin $networks) {
        Invoke-Docker @('network', 'create', '--driver', 'bridge', '--internal', $NetworkName) '创建 VLStream 容器网络' | Out-Null
        return
    }
    $internal = Invoke-Docker @('network', 'inspect', '--format', '{{.Internal}}', $NetworkName) '检查 VLStream 容器网络隔离'
    if ($internal -ne 'true') {
        throw "网络 $NetworkName 已存在且并非内部网络。请使用另一个 -ProjectPrefix。"
    }
}

if ($Stop -and -not (Test-Path -LiteralPath $ownerFile -PathType Leaf)) {
    Write-Host '没有已部署的 VLStream 服务。'
    return
}

try {
    $dockerPath = (Get-Command docker.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $composeVersion = (Invoke-Docker @('compose', 'version', '--short') '检查 Docker Compose 版本' | Out-String).Trim()
    if ($composeVersion -notmatch '^v?(\d+\.\d+\.\d+)' -or [version]$Matches[1] -lt [version]'2.24.4') {
        throw "需要 Docker Compose 2.24.4 或更新版本，当前为 $composeVersion。"
    }

    New-Item -ItemType Directory -Path $DataDirectory -Force | Out-Null
    $lockFile = [IO.File]::Open((Join-Path $DataDirectory '.setup.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        if ($Stop) {
            Clear-InheritedComposeVariables @($cloudDirectory, $wvpDirectory)
            Assert-EngineReady
            Invoke-Compose $cloudDirectory @('stop') '停止 VLS'
            Invoke-Compose $wvpDirectory @('stop') '停止 WVP'
            Write-Host 'VLStream 服务已停止，数据保留。'
            return
        }

        Install-ReleaseFiles $cloudDirectory 'cloud.override.yaml' '289df901bb2ada4e5128a3f58729c4ee38546b50307f134d1c7a852b4997186f'
        Install-ReleaseFiles $wvpDirectory 'wvp.override.yaml' '1211bdbba114f171099bfbb8660204fECEF633b68b015af9bedda25e7a57c49e'
        $networkName = "$ProjectPrefix-internal"
        Write-DeploymentEnvironment $cloudDirectory ([ordered]@{
            COMPOSE_PROJECT_NAME = "$ProjectPrefix-cloud"
            VLS_DESKTOP_NETWORK = $networkName
            VLS_DESKTOP_PORT = $Port
            VLS_DESKTOP_MINIO_PORT = $Port + 1
            VLS_DESKTOP_MINIO_CONSOLE_PORT = $Port + 2
            MYSQL_ROOT_PASSWORD = New-Secret
            MYSQL_PASSWORD = New-Secret
            REDIS_PASSWORD = New-Secret
            MINIO_ROOT_PASSWORD = New-Secret
            VLSTREAM_COMPUTE_ENCRYPTION_KEY = New-Secret
            VLS_DESKTOP_WVP_COMPAT_DB_PASSWORD = New-Secret
            VLS_DESKTOP_MODEL_SIGNING_SECRET = New-Secret
            VLS_DESKTOP_JWT_SECRET = New-Secret
            VLSTREAM_DEVICE_MEDIA_OSS_CONFIG_KEY = 'minio-device-media'
        })
        Write-DeploymentEnvironment $wvpDirectory ([ordered]@{
            COMPOSE_PROJECT_NAME = "$ProjectPrefix-wvp"
            VLS_DESKTOP_NETWORK = $networkName
            MYSQL_ROOT_PASSWORD = New-Secret
            DB_PASSWORD = New-Secret
            REDIS_PASSWORD = New-Secret
            ZLM_SECRET = New-Secret
            TOKEN_SECRET = New-Secret
            SIP_PASSWORD = New-Secret
            VLSTREAM_DEVICE_TENANT_BINDING_SECRET = New-Secret
        })
        Clear-InheritedComposeVariables @($cloudDirectory, $wvpDirectory)
        Invoke-Compose $cloudDirectory @('config', '--quiet') '校验 VLS 配置'
        Invoke-Compose $wvpDirectory @('config', '--quiet') '校验 WVP 配置'
        if ($ConfigureOnly) {
            Write-Host 'VLS 和 WVP Compose 配置校验通过。'
            return
        }

        Assert-EngineReady
        $cloudEnvironment = Get-Content -LiteralPath (Join-Path $cloudDirectory '.env') -Raw
        if ($cloudEnvironment -notmatch "(?m)^COMPOSE_PROJECT_NAME=$ProjectPrefix-cloud\r?$") {
            throw '服务配置已绑定其他 Compose 项目。请使用安装时的项目名重试。'
        }
        if ($cloudEnvironment -notmatch '(?m)^VLS_DESKTOP_PORT=(\d+)\r?$') {
            throw '服务配置缺少 VLS_DESKTOP_PORT。'
        }
        $installedPort = $Matches[1]
        Initialize-DeploymentOwnership
        Initialize-DesktopNetwork $networkName

        Write-Host '正在联网获取组件并启动 WVP，首次安装需要下载和构建镜像。'
        Invoke-Compose $wvpDirectory @('up', '-d', '--build', '--wait', '--wait-timeout', '600') '启动 WVP'

        Write-Host '正在启动 VLS 并等待数据库和后端就绪。'
        $initialize = -not (Test-Path -LiteralPath $initializedFile -PathType Leaf)
        try {
            if ($initialize) {
                $env:VLS_DESKTOP_INITIALIZE = 'true'
            }
            Invoke-Compose $cloudDirectory @('up', '-d', '--wait', '--wait-timeout', '600') '启动 VLS'
        } finally {
            Remove-Item -LiteralPath 'Env:VLS_DESKTOP_INITIALIZE' -ErrorAction SilentlyContinue
        }

        if ($initialize) {
            Assert-InitialAdministrator
            Set-Content -LiteralPath $initializedFile -Value 'true' -Encoding utf8NoBOM
            # Recreate the backend with its first-run data initialization disabled.
            Invoke-Compose $cloudDirectory @('up', '-d', '--wait', '--wait-timeout', '600') '完成 VLS 首次初始化'
        }
        $baseUrl = "http://localhost:$installedPort"
        $mode = Invoke-RestMethod -Uri "$baseUrl/bus/vls-server/sso/v1/mode" -TimeoutSec 15
        if ($mode.data.tenantType -ne 'single') {
            throw 'VLS 尚未返回单租户运行状态，请检查服务日志。'
        }
        Write-Host "VLStream 服务已就绪：$baseUrl/bus/vls-ui/"
    } finally {
        $lockFile.Dispose()
    }
} catch {
    if ($Stop) {
        Write-Host '停止服务失败。请确认 Docker Desktop 正在运行后重试。' -ForegroundColor Yellow
    } else {
        Write-Host '服务部署未完成。请启动 Docker Desktop，确认网络可访问镜像仓库、所需端口可用，然后运行安装程序提供的部署入口重试。' -ForegroundColor Yellow
    }
    Write-Error -ErrorRecord $_ -ErrorAction Continue
    exit 1
}
