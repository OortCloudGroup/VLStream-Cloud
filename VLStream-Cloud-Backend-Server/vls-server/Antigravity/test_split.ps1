[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$line = "IOS4quWunuS+iw== |||  个实例 ||| "
$parts = $line -split " \|\|\| "
Write-Host "Count: $($parts.Count)"
for ($i = 0; $i -lt $parts.Count; $i++) {
    Write-Host "parts[$i]: '$($parts[$i])'"
}
Write-Host "IsNullOrWhiteSpace parts[2]: $([string]::IsNullOrWhiteSpace($parts[2]))"
