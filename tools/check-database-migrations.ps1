[CmdletBinding()]
param(
    [string]$BaseRef
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$migrationPrefix = 'VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/'
$allowedGeneratedPrefix = 'deploy/release/sql/init/'

Push-Location $repositoryRoot
try {
    $candidateFiles = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )

    if ($BaseRef) {
        $addedFiles = & git diff --name-only --diff-filter=A "$BaseRef...HEAD"
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to compare the current branch with '$BaseRef'."
        }
        foreach ($file in $addedFiles) {
            [void]$candidateFiles.Add(($file -replace '\\', '/'))
        }

        $changedExistingMigrations = @(
            & git diff --name-only --diff-filter=DMRT "$BaseRef...HEAD" -- $migrationPrefix |
                ForEach-Object { $_ -replace '\\', '/' }
        )
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to check existing migrations against '$BaseRef'."
        }
        if ($changedExistingMigrations.Count -gt 0) {
            Write-Error (
                "Existing Flyway migrations are immutable; add a new migration instead:`n - " +
                ($changedExistingMigrations -join "`n - ")
            )
        }
    }

    $untrackedFiles = & git ls-files --others --exclude-standard
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to list untracked files.'
    }
    foreach ($file in $untrackedFiles) {
        [void]$candidateFiles.Add(($file -replace '\\', '/'))
    }

    $invalidSqlFiles = @(
        $candidateFiles |
            Where-Object {
                $_.EndsWith('.sql', [System.StringComparison]::OrdinalIgnoreCase) -and
                -not $_.StartsWith($migrationPrefix, [System.StringComparison]::OrdinalIgnoreCase) -and
                -not $_.StartsWith($allowedGeneratedPrefix, [System.StringComparison]::OrdinalIgnoreCase)
            } |
            Sort-Object
    )

    $migrationFiles = @(
        $candidateFiles |
            Where-Object {
                $_.StartsWith($migrationPrefix, [System.StringComparison]::OrdinalIgnoreCase) -and
                $_.EndsWith('.sql', [System.StringComparison]::OrdinalIgnoreCase)
            }
    )

    $invalidMigrationNames = @(
        $migrationFiles |
            Where-Object {
                (Split-Path $_ -Leaf) -notmatch '^V\d+_\d+_\d+_\d{3}__[a-z0-9_]+\.sql$'
            } |
            Sort-Object
    )

    $allMigrationNames = @(
        Get-ChildItem -LiteralPath (Join-Path $repositoryRoot $migrationPrefix) -Filter '*.sql' -File |
            Select-Object -ExpandProperty Name
    )
    $duplicateVersions = @(
        $allMigrationNames |
            Where-Object { $_ -match '^(V\d+_\d+_\d+_\d{3})__' } |
            ForEach-Object { [regex]::Match($_, '^(V\d+_\d+_\d+_\d{3})__').Groups[1].Value } |
            Group-Object |
            Where-Object Count -gt 1 |
            Select-Object -ExpandProperty Name
    )

    if (
        $invalidSqlFiles.Count -gt 0 -or
        $invalidMigrationNames.Count -gt 0 -or
        $duplicateVersions.Count -gt 0
    ) {
        if ($invalidSqlFiles.Count -gt 0) {
            Write-Error (
                "Incremental SQL must be placed in the Flyway migration directory:`n - " +
                ($invalidSqlFiles -join "`n - ")
            )
        }
        if ($invalidMigrationNames.Count -gt 0) {
            Write-Error (
                "Flyway migration names must match Vx_y_z_NNN__description.sql:`n - " +
                ($invalidMigrationNames -join "`n - ")
            )
        }
        if ($duplicateVersions.Count -gt 0) {
            Write-Error (
                "Flyway migration versions must be unique:`n - " +
                ($duplicateVersions -join "`n - ")
            )
        }
        exit 1
    }

    Write-Host 'Database migration path check passed.'
}
finally {
    Pop-Location
}
