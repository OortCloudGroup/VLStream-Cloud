# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$dir = "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"

if (Test-Path $dir) {
    Write-Host "Deleting contents of: $dir"
    Get-ChildItem -Path $dir -Force | ForEach-Object {
        $p = $_.FullName
        try {
            Write-Host "Deleting: $p"
            Remove-Item -Path $p -Recurse -Force -ErrorAction Stop
        } catch {
            Write-Host "Failed to delete: $p"
            Write-Host "Error: $_"
        }
    }
    
    # Try deleting the parent folder itself
    try {
        Write-Host "Deleting base folder: $dir"
        Remove-Item -Path $dir -Force -ErrorAction Stop
        Write-Host "Successfully deleted base folder!"
    } catch {
        Write-Host "Failed to delete base folder: $dir"
        Write-Host "Error: $_"
    }
} else {
    Write-Host "Directory does not exist: $dir"
}
