# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Target directory
$dir = "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"

# 1. Kill pwsh PID 65540
$proc = Get-Process -Id 65540 -ErrorAction SilentlyContinue
if ($proc) {
    Write-Host "Killing remaining shell process PID 65540..."
    Stop-Process -Id 65540 -Force -ErrorAction SilentlyContinue
}

# Wait a second
Start-Sleep -Seconds 1

# 2. Try deleting vls-server to Recycle Bin
if (Test-Path $dir) {
    Write-Host "Trying to move $dir to Recycle Bin..."
    try {
        Add-Type -AssemblyName 'Microsoft.VisualBasic'
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory($dir, 'OnlyErrorDialogs', 'SendToRecycleBin')
        Write-Host "Successfully moved to Recycle Bin!"
    } catch {
        Write-Host "Delete directory to Recycle Bin failed: $_"
        
        # Try standard Remove-Item
        Write-Host "Trying Force Remove-Item..."
        try {
            Remove-Item -Path $dir -Force -ErrorAction Stop
            Write-Host "Successfully deleted empty folder via Remove-Item!"
        } catch {
            Write-Host "Force Remove-Item also failed: $_"
        }
    }
} else {
    Write-Host "Folder does not exist: $dir"
}
