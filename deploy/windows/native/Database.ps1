#Requires -Version 7.2

function New-VlsNativeProcessInfo {
    param([string]$FilePath, [string[]]$ArgumentList)

    $info = [Diagnostics.ProcessStartInfo]::new($FilePath)
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    foreach ($item in $ArgumentList) { $info.ArgumentList.Add($item) }
    return $info
}

function Invoke-VlsNativeProcess {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$InputText,
        [string]$InputFile,
        [string]$WorkingDirectory,
        [string]$IgnoreMySqlLoginFile
    )

    $info = New-VlsNativeProcessInfo $FilePath $ArgumentList
    $info.RedirectStandardInput = [bool]($InputText -or $InputFile)
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardInputEncoding = if ($info.RedirectStandardInput) { [Text.UTF8Encoding]::new($false) }
    if ($WorkingDirectory) { $info.WorkingDirectory = $WorkingDirectory }
    if ($IgnoreMySqlLoginFile) { $info.Environment['MYSQL_TEST_LOGIN_FILE'] = $IgnoreMySqlLoginFile }
    $process = [Diagnostics.Process]::Start($info)
    try {
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if ($InputFile) {
            $stream = [IO.File]::OpenRead($InputFile)
            try { $stream.CopyTo($process.StandardInput.BaseStream) } finally { $stream.Dispose() }
        } elseif ($InputText) {
            $process.StandardInput.Write($InputText)
        }
        if ($info.RedirectStandardInput) { $process.StandardInput.Close() }
        $process.WaitForExit()
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output   = $stdout.GetAwaiter().GetResult().Trim()
            Error    = $stderr.GetAwaiter().GetResult().Trim()
        }
    } finally {
        if (-not $process.HasExited) { $process.Kill($true) }
        $process.Dispose()
    }
}

function Invoke-VlsOwnedMySql {
    param(
        [string]$Client,
        [string]$State,
        [int]$Port,
        [string]$RootPassword,
        [string]$Sql,
        [string]$SqlFile,
        [string]$Database,
        [switch]$Shutdown
    )

    $optionPath = Join-Path $State ('client-' + [Guid]::NewGuid().ToString('N') + '.ini')
    try {
        # Compartmentalize mysql.exe from any global configuration and existing login-path file.
        $authArg = '--no-defaults'
        if ($RootPassword) {
            [IO.File]::WriteAllText($optionPath, "[client]`npassword=$RootPassword`n")
            $authArg = "--defaults-file=$optionPath"
        }
        $arguments = @($authArg, '--protocol=TCP', '--host=127.0.0.1', "--port=$Port", '--user=root', '--connect-timeout=3')
        if ($Shutdown) {
            $arguments += 'shutdown'
        } else {
            $arguments += '--batch', '--skip-column-names', '--default-character-set=utf8mb4'
            if ($Database) { $arguments += "--database=$Database" }
        }
        Invoke-VlsNativeProcess -FilePath $Client -ArgumentList $arguments -InputText $Sql -InputFile $SqlFile `
            -IgnoreMySqlLoginFile (Join-Path $State 'no-global-login.cnf')
    } finally {
        [IO.File]::Delete($optionPath)
    }
}

function Initialize-VlsNativeDatabase {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$InstallDirectory,
        [Parameter(Mandatory)][string]$DataDirectory,
        [ValidateRange(1024, 65535)][int]$MySqlPort = 17306
    )

    $ErrorActionPreference = 'Stop'
    $mysqlRuntime = Join-Path $InstallDirectory 'runtime/mysql'
    $mysqlDaemon = Join-Path $mysqlRuntime 'bin/mysqld.exe'
    $mysqlClient = Join-Path $mysqlRuntime 'bin/mysql.exe'
    $mysqlAdmin = Join-Path $mysqlRuntime 'bin/mysqladmin.exe'
    $vlsSeed = Join-Path $InstallDirectory 'sql/10-oortcloud-workflowforms-vls.sql'
    $vlsLocalAccessSeed = Join-Path $InstallDirectory 'sql/11-oortcloud-local-user-system-seed.sql'
    $wvpSeed = Join-Path $InstallDirectory 'sql/10-ry-wvp.sql'
    foreach ($asset in $mysqlDaemon, $mysqlClient, $mysqlAdmin, $vlsSeed, $vlsLocalAccessSeed, $wvpSeed) {
        if (-not [IO.File]::Exists($asset)) { throw "Missing installation asset: $asset" }
    }

    $state = Join-Path $DataDirectory 'mysql'
    $datadir = Join-Path $state 'data'
    $credentialsFile = Join-Path $state 'credentials.xml'
    $databaseReady = Join-Path $state 'databases.ready'
    $configuration = Join-Path $state 'my.ini'
    [IO.Directory]::CreateDirectory($state) | Out-Null
    $lock = [IO.File]::Open((Join-Path $state 'provision.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        if (-not [IO.File]::Exists($credentialsFile)) {
            if ([IO.Directory]::Exists($datadir) -or [IO.File]::Exists($configuration) -or [IO.File]::Exists($databaseReady)) {
                throw "Existing MySQL data or configuration has no matching VLStream installation record: $state"
            }
            $credentials = [ordered]@{ Port = $MySqlPort }
            foreach ($user in 'root', 'vls_app', 'wvp_app') {
                $random = [Security.Cryptography.RandomNumberGenerator]::GetBytes(24)
                $password = ConvertTo-SecureString ([Convert]::ToHexString($random)) -AsPlainText -Force
                $credentials[$user] = [PSCredential]::new($user, $password)
            }
            $pendingCredentials = Join-Path $state 'credentials.pending.xml'
            Export-Clixml -InputObject $credentials -LiteralPath $pendingCredentials
            [IO.File]::Move($pendingCredentials, $credentialsFile)
        } else {
            $credentials = Import-Clixml -LiteralPath $credentialsFile
            if ($credentials.Port -ne $MySqlPort) {
                throw "The existing MySQL installation is configured to use port $($credentials.Port)."
            }
        }

        $settings = [pscustomobject]@{
            Port               = $MySqlPort
            MySqlConfig        = $configuration
            MySqlDataDirectory = $datadir
            VlsDatabase        = 'oortcloud_workflowforms_vls'
            VlsUser            = 'vls_app'
            VlsPassword        = $credentials.vls_app.GetNetworkCredential().Password
            WvpDatabase        = 'ry-wvp'
            WvpUser            = 'wvp_app'
            WvpPassword        = $credentials.wvp_app.GetNetworkCredential().Password
            NewInstallation    = -not [IO.File]::Exists((Join-Path $DataDirectory 'first-login.ready'))
        }
        if ([IO.File]::Exists($databaseReady)) {
            if (-not [IO.File]::Exists($configuration) -or -not [IO.File]::Exists((Join-Path $datadir 'mysql.ibd'))) {
                throw 'The installed MySQL configuration or data is missing.'
            }
            return $settings
        }

        $socket = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $MySqlPort)
        try {
            $socket.Start()
        } catch {
            throw "本机数据库无法绑定 127.0.0.1:$MySqlPort。确认该端口可用后再重试。"
        } finally {
            $socket.Stop()
        }
        [IO.Directory]::CreateDirectory($datadir) | Out-Null
        $runtimePath = $mysqlRuntime.Replace('\', '/')
        $dataPath = $datadir.Replace('\', '/')
        $statePath = $state.Replace('\', '/')
        $configText = @"
[mysqld]
basedir="$runtimePath"
datadir="$dataPath"
log-error="$statePath/mysqld.log"
pid-file="$statePath/mysqld.pid"
bind-address=127.0.0.1
port=$MySqlPort
mysqlx=OFF
character-set-server=utf8mb4
skip-log-bin
"@
        if (-not [IO.File]::Exists($configuration)) { [IO.File]::WriteAllText($configuration, "$configText`n") }
        $defaultsFile = "--defaults-file=$configuration"
        if (-not [IO.File]::Exists((Join-Path $datadir 'mysql.ibd'))) {
            if (Get-ChildItem -LiteralPath $datadir -Force | Select-Object -First 1) {
                throw "MySQL initialization did not finish and left files in $datadir."
            }
            $initialized = Invoke-VlsNativeProcess -FilePath $mysqlDaemon -ArgumentList @($defaultsFile, '--initialize-insecure')
            if ($initialized.ExitCode) { throw "MySQL initialization failed. See $state/mysqld.log. $($initialized.Error)" }
        }

        $process = [Diagnostics.Process]::Start((New-VlsNativeProcessInfo $mysqlDaemon @($defaultsFile)))
        $rootPassword = $credentials.root.GetNetworkCredential().Password
        $currentPassword = ''
        $connected = $false
        try {
            foreach ($attempt in 1..30) {
                if ($process.HasExited) { throw "MySQL stopped during startup. See $state/mysqld.log." }
                foreach ($candidate in @($rootPassword, '')) {
                    $response = Invoke-VlsOwnedMySql -Client $mysqlClient -State $state -Port $MySqlPort -RootPassword $candidate `
                        -Sql 'SELECT @@datadir;'
                    if ($response.ExitCode -eq 0) {
                        $actualData = [IO.Path]::TrimEndingDirectorySeparator([IO.Path]::GetFullPath($response.Output))
                        if ($actualData -ne [IO.Path]::GetFullPath($datadir)) { throw 'The MySQL endpoint belongs to a different installation.' }
                        $currentPassword = $candidate
                        $connected = $true
                        break
                    }
                }
                if ($connected) { break }
                Start-Sleep -Milliseconds 500
            }
            if (-not $connected) { throw "Could not connect to the new MySQL instance. See $state/mysqld.log." }

            if (-not $currentPassword) {
                $updatedRoot = Invoke-VlsOwnedMySql -Client $mysqlClient -State $state -Port $MySqlPort `
                    -Sql "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';"
                if ($updatedRoot.ExitCode) { throw 'Could not secure the newly initialized MySQL root account.' }
                $currentPassword = $rootPassword
            }

            foreach ($database in @(
                @{ Schema = $settings.VlsDatabase; User = $settings.VlsUser; Password = $settings.VlsPassword; Seeds = @($vlsSeed, $vlsLocalAccessSeed) },
                @{ Schema = $settings.WvpDatabase; User = $settings.WvpUser; Password = $settings.WvpPassword; Seeds = @($wvpSeed) }
            )) {
                $existing = Invoke-VlsOwnedMySql -Client $mysqlClient -State $state -Port $MySqlPort -RootPassword $currentPassword `
                    -Sql "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$($database.Schema)';"
                if ($existing.ExitCode) { throw "Could not check the status of database $($database.Schema)." }
                $seeded = Join-Path $state "$($database.Schema).seeded"
                if ([IO.File]::Exists($seeded)) {
                    if ($existing.Output -ne '1') { throw "Previously initialized database is missing: $($database.Schema)" }
                    continue
                }
                $pending = "$seeded.pending"
                if (-not [IO.File]::Exists($pending)) {
                    if ($existing.Output -ne '0') { throw "Database $($database.Schema) already exists without an initialization record. Its data was preserved." }
                    # A failed import can only be retried against the unpublished schema reserved here.
                    [IO.File]::WriteAllText($pending, "reserved for initial import`n")
                }

                $create = @"
CREATE DATABASE IF NOT EXISTS ``$($database.Schema)`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$($database.User)'@'127.0.0.1' IDENTIFIED BY '$($database.Password)';
GRANT ALL PRIVILEGES ON ``$($database.Schema)``.* TO '$($database.User)'@'127.0.0.1';
"@
                $created = Invoke-VlsOwnedMySql -Client $mysqlClient -State $state -Port $MySqlPort -RootPassword $currentPassword -Sql $create
                if ($created.ExitCode) { throw "Could not create the dedicated database and user for $($database.Schema)." }
                # The tagged VLS local-user seed must precede Flyway, which expects the tenant's administrator role.
                foreach ($seed in $database.Seeds) {
                    $imported = Invoke-VlsOwnedMySql -Client $mysqlClient -State $state -Port $MySqlPort -RootPassword $currentPassword `
                        -Database $database.Schema -SqlFile $seed
                    if ($imported.ExitCode) { throw "Initialization of $($database.Schema) failed in $(Split-Path -Leaf $seed). $($imported.Error)" }
                }
                [IO.File]::WriteAllText($seeded, "seeded`n")
                [IO.File]::Delete($pending)
            }

            [IO.File]::WriteAllText($databaseReady, "initialized`n")
            return $settings
        } finally {
            try {
                if ($connected) {
                    Invoke-VlsOwnedMySql -Client $mysqlAdmin -State $state -Port $MySqlPort -RootPassword $currentPassword -Shutdown | Out-Null
                }
            } finally {
                if (-not $process.WaitForExit(10000)) { $process.Kill($true) }
                $process.Dispose()
            }
        }
    } finally {
        $lock.Dispose()
    }
}

function Stop-VlsNativeDatabase {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$InstallDirectory,
        [Parameter(Mandatory)][string]$DataDirectory
    )

    $ErrorActionPreference = 'Stop'
    $state = Join-Path $DataDirectory 'mysql'
    $credentials = Import-Clixml -LiteralPath (Join-Path $state 'credentials.xml')
    $rootPassword = $credentials.root.GetNetworkCredential().Password
    $runtime = Join-Path $InstallDirectory 'runtime/mysql/bin'
    $connection = @{ State = $state; Port = $credentials.Port; RootPassword = $rootPassword }
    $running = Invoke-VlsOwnedMySql -Client (Join-Path $runtime 'mysql.exe') @connection -Sql 'SELECT @@datadir;'
    if ($running.ExitCode) { throw 'Cannot connect to the VLStream-owned MySQL instance.' }
    $realPath = [IO.Path]::TrimEndingDirectorySeparator([IO.Path]::GetFullPath($running.Output))
    if ($realPath -ne [IO.Path]::GetFullPath((Join-Path $state 'data'))) {
        throw 'The MySQL endpoint belongs to a different installation.'
    }
    $stopped = Invoke-VlsOwnedMySql -Client (Join-Path $runtime 'mysqladmin.exe') @connection -Shutdown
    if ($stopped.ExitCode) { throw 'MySQL shutdown did not complete.' }
}

function Complete-VlsNativeFirstLogin {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$InstallDirectory,
        [Parameter(Mandatory)][string]$DataDirectory,
        [Parameter(Mandatory)][uri]$VlsBaseUri
    )

    $ErrorActionPreference = 'Stop'
    $complete = Join-Path $DataDirectory 'first-login.ready'
    if ([IO.File]::Exists($complete)) { return }
    if (-not [IO.File]::Exists((Join-Path $DataDirectory 'mysql/databases.ready'))) {
        throw 'The VLStream installation database has not been initialized.'
    }
    if (-not $VlsBaseUri.IsAbsoluteUri -or $VlsBaseUri.Host -ne '127.0.0.1' -or $VlsBaseUri.Scheme -ne 'http' -or $VlsBaseUri.UserInfo) {
        throw 'First login must use the installer-owned VLS backend over IPv4 loopback.'
    }

    $base = $VlsBaseUri.AbsoluteUri.TrimEnd('/')
    $java = Join-Path $InstallDirectory 'runtime/java/bin/java.exe'
    $native = Join-Path $InstallDirectory 'native'
    $javaClassPath = "$native$([IO.Path]::PathSeparator)$(Join-Path $native 'bcprov.jar')"
    # This is the v1.2.12 public encryption key, also used by the standard login page.
    $publicKey = '049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64'

    function Invoke-InitialAdminLogin([string]$Password) {
        $encrypted = Invoke-VlsNativeProcess -FilePath $java -ArgumentList @('-cp', $javaClassPath, 'NativeSm2Password', $publicKey) `
            -InputText "$Password`n"
        if ($encrypted.ExitCode -ne 0 -or $encrypted.Output -notmatch '^04[0-9a-f]+$') {
            throw 'Could not encrypt the local bootstrap login.'
        }
        $login = Invoke-RestMethod -Uri "$base/blade-auth/token" -Method Post -ContentType 'application/x-www-form-urlencoded' -NoProxy -MaximumRedirection 0 `
            -Body @{ grantType = 'password'; account = 'admin'; password = $encrypted.Output } -TimeoutSec 10
        if ($login.code -ne 200) { return $null }
        if ($login.data.account -ne 'admin' -or $login.data.tenantId -ne '000000' -or -not $login.data.accessToken) {
            throw 'Bootstrap login returned an unexpected account or tenant.'
        }
        return $login.data.accessToken
    }

    $tokens = [Collections.Generic.List[string]]::new()
    try {
        $initial = Invoke-InitialAdminLogin '123456'
        if (-not $initial) {
            $existing = Invoke-InitialAdminLogin 'Codex@123456'
            if (-not $existing) { throw 'The fresh local administrator seed did not accept its initial password.' }
            $tokens.Add($existing)
            $updated = Invoke-RestMethod -Uri "$base/system/user/profile/updatePwd" -Method Put -ContentType 'application/json' -NoProxy -MaximumRedirection 0 `
                -Headers @{ Authorization = "Bearer $existing" } `
                -Body (@{ oldPassword = 'Codex@123456'; newPassword = '123456' } | ConvertTo-Json -Compress) -TimeoutSec 10
            if ($updated.code -ne 200) { throw 'VLS did not accept the requested initial administrator password.' }
            $initial = Invoke-InitialAdminLogin '123456'
            if (-not $initial) { throw 'VLS did not verify the new administrator credentials.' }
        }
        $tokens.Add($initial)
        [IO.File]::WriteAllText($complete, "verified`n")
    } finally {
        foreach ($token in $tokens) {
            try {
                Invoke-RestMethod -Uri "$base/blade-auth/logout" -Method Post -Headers @{ Authorization = "Bearer $token" } `
                    -NoProxy -MaximumRedirection 0 -TimeoutSec 5 | Out-Null
            } catch {
                Write-Verbose 'The temporary bootstrap session could not be logged out.'
            }
        }
    }
}
