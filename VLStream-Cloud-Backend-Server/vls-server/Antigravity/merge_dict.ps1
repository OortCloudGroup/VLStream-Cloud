[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$baseDir = "d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity"
$outputFile = Join-Path $baseDir "chinese_dictionary.txt"

# Clear or create the output file
New-Item -ItemType File -Path $outputFile -Force

for ($i = 0; $i -lt 10; $i++) {
    $chunkFile = Join-Path $baseDir "dict_chunk_$i.txt"
    if (Test-Path $chunkFile) {
        Write-Host "Merging $chunkFile..."
        # We read content as UTF8. To avoid extra blank lines, we filter out null/empty lines.
        $lines = Get-Content -Path $chunkFile -Encoding utf8 | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
        $lines | Add-Content -Path $outputFile -Encoding utf8
    } else {
        Write-Warning "Chunk file not found: $chunkFile"
    }
}

Write-Host "Successfully merged all chunks into $outputFile"
