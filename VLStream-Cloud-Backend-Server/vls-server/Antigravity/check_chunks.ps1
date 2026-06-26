[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$baseDir = "d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity"
for ($i = 0; $i -lt 10; $i++) {
    $filename = "dict_chunk_$i.txt"
    $filepath = Join-Path $baseDir $filename
    if (-not (Test-Path $filepath)) {
        Write-Host "$filename : DOES NOT EXIST"
        continue
    }
    
    $lines = Get-Content -Path $filepath -Encoding utf8
    $total = $lines.Count
    $translated = 0
    $untranslated = 0
    
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $parts = $line -split " \|\|\| "
        if ($parts.Count -ge 3 -and -not [string]::IsNullOrWhiteSpace($parts[2])) {
            $translated++
        } else {
            $untranslated++
        }
    }
    
    Write-Host "$filename : Total=$total, Translated=$translated, Untranslated=$untranslated"
}
