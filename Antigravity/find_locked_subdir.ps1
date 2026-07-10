# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$basePath = "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"

function Test-Rename($path) {
    $tempPath = "$path-testtemp"
    try {
        Rename-Item -Path $path -NewName (Split-Path $tempPath -Leaf) -ErrorAction Stop
        # Rename back
        Rename-Item -Path $tempPath -NewName (Split-Path $path -Leaf) -ErrorAction Stop
        return $true
    } catch {
        Write-Host "Locked path: $path"
        Write-Host "Error details: $_"
        return $false
    }
}

Write-Host "Testing rename of base directory..."
Test-Rename $basePath

Write-Host "`nTesting rename of subdirectories..."
Get-ChildItem -Path $basePath -Directory -Recurse | ForEach-Object {
    Test-Rename $_.FullName
}
