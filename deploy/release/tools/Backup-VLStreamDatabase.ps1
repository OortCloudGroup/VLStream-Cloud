#Requires -Version 7.0

[CmdletBinding(DefaultParameterSetName = 'Compose')]
param(
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$OutputDirectory,

    [ValidatePattern('^[A-Za-z0-9_][A-Za-z0-9_-]{0,63}$')]
    [string]$Database = 'oortcloud_workflowforms_vls',

    [Parameter(ParameterSetName = 'Compose')]
    [string]$ComposeDirectory = (Split-Path -Parent $PSScriptRoot),

    [Parameter(Mandatory, ParameterSetName = 'External')]
    [ValidatePattern('^[A-Za-z0-9_][A-Za-z0-9_.-]*$')]
    [string]$LoginPath,

    [Parameter(Mandatory, ParameterSetName = 'Native')]
    [string]$NativeDataRoot,

    [Parameter(ParameterSetName = 'External')]
    [Parameter(ParameterSetName = 'Native')]
    [string]$MysqlDumpPath = 'mysqldump'
)

$ErrorActionPreference = 'Stop'

$dumpArguments = @(
    '--single-transaction'
    '--quick'
    '--no-tablespaces'
    '--set-gtid-purged=OFF'
    '--column-statistics=0'
    '--default-character-set=utf8mb4'
    '--hex-blob'
    '--routines'
    '--events'
    '--triggers'
    '--databases'
    $Database
)

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true

if ($PSCmdlet.ParameterSetName -eq 'Compose') {
    $composeProject = Get-Item -LiteralPath $ComposeDirectory
    if (-not $composeProject.PSIsContainer) {
        throw "ComposeDirectory must be a directory: $ComposeDirectory"
    }

    $client = 'docker'
    $startInfo.WorkingDirectory = $composeProject.FullName
    $containerCommand = @'
if [ -n "$MYSQL_ROOT_PASSWORD_FILE" ]; then
    MYSQL_ROOT_PASSWORD=$(cat "$MYSQL_ROOT_PASSWORD_FILE") || exit 1
fi
if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
    echo 'The MySQL container has no configured root password.' >&2
    exit 1
fi
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
exec mysqldump --no-defaults --protocol=socket --user=root "$@"
'@
    $containerCommand = $containerCommand.Replace("`r`n", "`n")
    $clientArguments = @('compose', 'exec', '-T', 'mysql', 'sh', '-c', $containerCommand, 'mysqldump')
} elseif ($PSCmdlet.ParameterSetName -eq 'Native') {
    $mysqlState = Join-Path $NativeDataRoot 'mysql'
    if (-not (Test-Path -LiteralPath (Join-Path $mysqlState 'databases.ready') -PathType Leaf)) {
        throw "The native VLStream databases are not initialized: $mysqlState"
    }
    $configured = Import-Clixml -LiteralPath (Join-Path $mysqlState 'credentials.xml')
    $port = 0
    if ($configured.root -isnot [pscredential] -or
        -not [int]::TryParse([string]$configured.Port, [ref]$port) -or $port -lt 1024 -or $port -gt 65535) {
        throw 'The native MySQL configuration is invalid.'
    }

    $client = $MysqlDumpPath
    if (-not $PSBoundParameters.ContainsKey('MysqlDumpPath')) {
        $client = Join-Path (Split-Path -Parent $PSScriptRoot) 'runtime/mysql/bin/mysqldump.exe'
    }
    $clientArguments = @('--no-defaults', '--protocol=TCP', '--host=127.0.0.1', "--port=$port", "--user=$($configured.root.UserName)")
    $startInfo.Environment['MYSQL_PWD'] = $configured.root.GetNetworkCredential().Password
    $startInfo.Environment['MYSQL_TEST_LOGIN_FILE'] = Join-Path $mysqlState ('.unused-login-' + [Guid]::NewGuid().ToString('N'))
} else {
    $client = $MysqlDumpPath
    $clientArguments = @("--login-path=$LoginPath")
}

$startInfo.FileName = (Get-Command -Name $client -CommandType Application | Select-Object -First 1).Source
foreach ($argument in $clientArguments + $dumpArguments) {
    $startInfo.ArgumentList.Add($argument)
}

$outputPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null
$timestamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmssfffZ')
$backupPath = Join-Path $outputPath "$Database-$timestamp.sql"
$partialPath = "$backupPath.$([Guid]::NewGuid().ToString('N')).partial"
$temporaryBackup = [System.IO.File]::Open($partialPath, [System.IO.FileMode]::CreateNew)

try {
    try {
        $dumpProcess = [System.Diagnostics.Process]::Start($startInfo)
        try {
            $readDiagnostics = $dumpProcess.StandardError.ReadToEndAsync()
            # Keep the SQL byte stream intact, including its original encoding and line endings.
            $dumpProcess.StandardOutput.BaseStream.CopyTo($temporaryBackup)
            $dumpProcess.WaitForExit()
            $diagnostics = $readDiagnostics.GetAwaiter().GetResult().Trim()

            if ($dumpProcess.ExitCode -ne 0) {
                throw "MySQL backup failed with exit code $($dumpProcess.ExitCode). $diagnostics"
            }
            if ($temporaryBackup.Length -eq 0) {
                throw 'MySQL backup returned no data.'
            }
            if ($diagnostics) {
                Write-Warning $diagnostics
            }
        } finally {
            $dumpProcess.Dispose()
        }
    } finally {
        $temporaryBackup.Dispose()
    }

    [System.IO.File]::Move($partialPath, $backupPath)
    Get-Item -LiteralPath $backupPath
} finally {
    [System.IO.File]::Delete($partialPath)
}
