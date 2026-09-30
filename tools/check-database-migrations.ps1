[CmdletBinding()]
param(
    # Compatibility alias for checking the historical top-level lineage against a base ref.
    [string]$BaseRef,
    [string]$MainlineBaseRef,
    [string]$ReleaseBaseRef
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$migrationRelative = 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration'
$migrationRoot = Join-Path $repositoryRoot $migrationRelative
$allowedGeneratedPrefix = 'deploy/release/sql/init/'
$lineages = @('mainline', 'release')

function Invoke-GitText {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & git -C $repositoryRoot @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($Arguments -join ' ')"
    }
    return @($output)
}

function Assert-BaselineUnchanged {
    param(
        [Parameter(Mandatory = $true)][string]$Lineage,
        [Parameter(Mandatory = $true)][string]$Reference
    )

    $basePaths = Invoke-GitText -Arguments @('ls-tree', '-r', '--name-only', $Reference, '--', $migrationRelative)
    $baselineFiles = @($basePaths | Where-Object {
        $_.StartsWith("$migrationRelative/", [System.StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path $_ -Parent).Replace('\', '/') -eq $migrationRelative -and
        $_.EndsWith('.sql', [System.StringComparison]::OrdinalIgnoreCase)
    })
    if ($baselineFiles.Count -eq 0) {
        throw "No top-level migration files found at baseline '$Reference'."
    }

    foreach ($basePath in $baselineFiles) {
        $name = Split-Path $basePath -Leaf
        $currentPath = Join-Path (Join-Path $migrationRoot $Lineage) $name
        if (-not (Test-Path -LiteralPath $currentPath -PathType Leaf)) {
            throw "Immutable $Lineage migration is missing: $name (baseline $Reference)."
        }

        $baseBlob = (Invoke-GitText -Arguments @('rev-parse', "$($Reference):$basePath") | Select-Object -First 1).Trim()
        $currentRelative = "$migrationRelative/$Lineage/$name"
        $currentBlob = (Invoke-GitText -Arguments @('hash-object', "--path=$currentRelative", '--', $currentPath) | Select-Object -First 1).Trim()
        if ($baseBlob -ne $currentBlob) {
            throw "Immutable $Lineage migration content changed: $name (baseline $Reference)."
        }
    }
}

if ($BaseRef) {
    if (-not $MainlineBaseRef) {
        $MainlineBaseRef = $BaseRef
    }
}

$migrationFiles = @(Get-ChildItem -LiteralPath $migrationRoot -Recurse -File -Filter '*.sql')
$invalidMigrationLocations = @()
$invalidMigrationNames = @()
$duplicateVersions = @()
$versionsByLineage = @{}

foreach ($file in $migrationFiles) {
    $relative = [System.IO.Path]::GetRelativePath($migrationRoot, $file.FullName).Replace('\', '/')
    $parts = $relative.Split('/')
    if ($parts.Count -ne 2 -or $parts[0] -notin $lineages) {
        $invalidMigrationLocations += $relative
        continue
    }

    if ($file.Name -notmatch '^V\d+_\d+_\d+_\d{3}__[a-z0-9_]+\.sql$') {
        $invalidMigrationNames += $relative
        continue
    }

    $version = [regex]::Match($file.Name, '^(V\d+_\d+_\d+_\d{3})__').Groups[1].Value
    if (-not $versionsByLineage.ContainsKey($parts[0])) {
        $versionsByLineage[$parts[0]] = [System.Collections.Generic.List[string]]::new()
    }
    $versionsByLineage[$parts[0]].Add($version)
}

foreach ($lineage in $lineages) {
    if (-not $versionsByLineage.ContainsKey($lineage)) { continue }
    $duplicates = @($versionsByLineage[$lineage] | Group-Object | Where-Object Count -gt 1)
    foreach ($duplicate in $duplicates) {
        $duplicateVersions += "$lineage/$($duplicate.Name)"
    }
}

$changedFiles = @(
    (Invoke-GitText -Arguments @('diff', '--name-only', 'HEAD'))
    (Invoke-GitText -Arguments @('diff', '--cached', '--name-only'))
    (Invoke-GitText -Arguments @('ls-files', '--others', '--exclude-standard'))
) | ForEach-Object { $_ -replace '\\', '/' } | Select-Object -Unique

$invalidSqlFiles = @(
    $changedFiles |
        Where-Object {
            $_.EndsWith('.sql', [System.StringComparison]::OrdinalIgnoreCase) -and
            -not $_.StartsWith("$migrationRelative/", [System.StringComparison]::OrdinalIgnoreCase) -and
            -not $_.StartsWith($allowedGeneratedPrefix, [System.StringComparison]::OrdinalIgnoreCase)
        } |
        Sort-Object
)

if ($invalidSqlFiles.Count -gt 0) {
    throw "Incremental SQL must be placed in the Flyway migration directory:`n - $($invalidSqlFiles -join "`n - ")"
}
if ($invalidMigrationLocations.Count -gt 0) {
    throw "Flyway migrations must be under the mainline or release lineage directory:`n - $($invalidMigrationLocations -join "`n - ")"
}
if ($invalidMigrationNames.Count -gt 0) {
    throw "Flyway migration names must match Vx_y_z_NNN__description.sql:`n - $($invalidMigrationNames -join "`n - ")"
}
if ($duplicateVersions.Count -gt 0) {
    throw "Flyway migration versions must be unique within each lineage:`n - $($duplicateVersions -join "`n - ")"
}

if ($MainlineBaseRef) {
    Assert-BaselineUnchanged -Lineage 'mainline' -Reference $MainlineBaseRef
}
if ($ReleaseBaseRef) {
    Assert-BaselineUnchanged -Lineage 'release' -Reference $ReleaseBaseRef
}

Write-Host 'Database migration path and lineage checks passed.'
