[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$workspace = "d:\work\ide\WorkSpace\vls-open-source\vls-server"
$src_path = Join-Path $workspace "src"
$doc_path = Join-Path $workspace "doc"
$chinese_regex = "[\p{IsCJKUnifiedIdeographs}]"

$files = Get-ChildItem -Path @($src_path, $doc_path) -Recurse -File | Where-Object { $_.Extension -in ".java", ".xml", ".yml", ".sql", ".properties", ".txt" }

$chinese_files = @()
foreach ($file in $files) {
    try {
        $content = Get-Content -Path $file.FullName -Raw -Encoding utf8
        if ($content -match $chinese_regex) {
            $chinese_files += $file
        }
    } catch {
        # ignore read errors
    }
}

Write-Host "Total files with Chinese characters: $($chinese_files.Count)"
$groups = @{}
foreach ($file in $chinese_files) {
    $rel_path = $file.FullName.Substring($workspace.Length + 1)
    $parts = $rel_path.Split([System.IO.Path]::DirectorySeparatorChar)
    $group_name = ""
    if ($parts.Length -ge 4) {
        $group_name = ($parts[0..3] -join "/")
    } else {
        $group_name = ($parts[0..($parts.Length-2)] -join "/")
    }
    if ($groups.ContainsKey($group_name)) {
        $groups[$group_name] = [int]$groups[$group_name] + 1
    } else {
        $groups[$group_name] = 1
    }
}

foreach ($key in ($groups.Keys | Sort-Object { $groups[$_] } -Descending)) {
    Write-Host "  $key : $($groups[$key])"
}
