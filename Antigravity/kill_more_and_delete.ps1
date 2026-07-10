# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$pids = @(11152, 48228, 64636)

foreach ($id in $pids) {
    $proc = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "Stopping Process: $($proc.ProcessName) (PID: $id)..."
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
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
