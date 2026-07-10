# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Kill all node_repl processes
Write-Host "Killing all node_repl processes..."
Get-Process -Name node_repl -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "Killing node_repl PID: $($_.Id)"
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}

# Wait a moment
Start-Sleep -Seconds 2

# Try to move vls-server to Recycle Bin
$dir = "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"
if (Test-Path $dir) {
    Write-Host "Moving to Recycle Bin: $dir"
    try {
        Add-Type -AssemblyName 'Microsoft.VisualBasic'
        [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory($dir, 'OnlyErrorDialogs', 'SendToRecycleBin')
        Write-Host "Successfully moved to Recycle Bin: $dir"
    } catch {
        Write-Host "Failed to delete: $dir"
        Write-Host "Error details: $_"
    }
} else {
    Write-Host "Directory already deleted or does not exist: $dir"
}
