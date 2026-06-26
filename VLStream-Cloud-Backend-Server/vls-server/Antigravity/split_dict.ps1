[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$dictPath = "d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity\chinese_dictionary.txt"
$lines = Get-Content -Path $dictPath -Encoding utf8
$chunkSize = 400
$chunkIndex = 0
for ($i = 0; $i -lt $lines.Count; $i += $chunkSize) {
    $chunk = $lines[$i..($i + $chunkSize - 1)] | Where-Object { $_ -ne $null }
    $chunkFile = "d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity\dict_chunk_$chunkIndex.txt"
    $chunk | Out-File -FilePath $chunkFile -Encoding utf8
    $chunkIndex++
}
Write-Host "Created $chunkIndex chunk files."
