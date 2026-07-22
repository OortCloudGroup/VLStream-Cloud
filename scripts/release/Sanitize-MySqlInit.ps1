param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"
$AllowedInsertTables = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
@(
    "ACT_GE_PROPERTY",
    "FLW_EV_DATABASECHANGELOG",
    "FLW_EV_DATABASECHANGELOGLOCK"
) | ForEach-Object { [void]$AllowedInsertTables.Add($_) }

# Keep only engine schema metadata. Application users, menus, dictionaries, and
# required demo workflows are recreated idempotently by SqlScriptExecutor.
function Test-KeepInsert {
    param([string]$Line)

    if ($Line -notmatch '^INSERT INTO `([^`]+)`') {
        return $true
    }

    return $AllowedInsertTables.Contains($Matches[1])
}

$ResolvedInput = (Resolve-Path -LiteralPath $InputPath).Path
$ResolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$OutputDirectory = Split-Path -Parent $ResolvedOutput
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$Utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$Reader = [System.IO.StreamReader]::new($ResolvedInput, $Utf8NoBom, $true)
$Writer = [System.IO.StreamWriter]::new($ResolvedOutput, $false, $Utf8NoBom)
$SkippedInsertCount = 0
$KeptInsertCount = 0
$InExportHeader = $false
$FirstLine = $true

try {
    $Writer.WriteLine("-- VLStream Cloud sanitized MySQL initialization schema")
    $Writer.WriteLine("-- Retains Flowable schema metadata only; runtime seeds restore required application data.")
    $Writer.WriteLine()

    while (($Line = $Reader.ReadLine()) -ne $null) {
        # Remove the Navicat export header because it contains source server details.
        if ($FirstLine -and $Line.Trim() -eq "/*") {
            $InExportHeader = $true
            $FirstLine = $false
            continue
        }
        $FirstLine = $false
        if ($InExportHeader) {
            if ($Line.Trim() -eq "*/") {
                $InExportHeader = $false
            }
            continue
        }

        if ($Line -match '^INSERT INTO `([^`]+)`') {
            if (-not (Test-KeepInsert -Line $Line)) {
                $SkippedInsertCount++
                continue
            }
            $KeptInsertCount++
        }

        # Views in the source dump are legacy adapters for private databases
        # that are not part of the standalone release.
        if ($Line -match '^-- View structure for ' -or
            $Line -match '^DROP VIEW ' -or
            $Line -match '^CREATE .* VIEW ') {
            continue
        }

        if ($Line -match '^-- VLStream Cloud sanitized MySQL initialization schema$' -or
            $Line -match '^-- Retains Flowable schema metadata only; runtime seeds restore required application data\.$') {
            continue
        }

        $Writer.WriteLine($Line)
    }
}
finally {
    $Reader.Dispose()
    $Writer.Dispose()
}

Write-Output "Sanitized initialization SQL: kept $KeptInsertCount metadata inserts; removed $SkippedInsertCount data inserts."
