# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$pidsToKill = @(11092, 48616, 60332, 52784, 67692, 22760, 45172)

# Kill known processes
foreach ($id in $pidsToKill) {
    $proc = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "Stopping Process: $($proc.ProcessName) (PID: $id)..."
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
}

# Scan again for any command line containing 3001 or 3002
$allProcs = Get-Process
foreach ($proc in $allProcs) {
    try {
        $cmd = $proc.CommandLine
        if ($cmd -and ($cmd.Contains("3001") -or $cmd.Contains("3002"))) {
            if ($proc.Id -eq $PID -or $proc.ProcessName -like "*pwsh*" -or $proc.ProcessName -like "*powershell*") {
                continue
            }
            Write-Host "Stopping matching process: $($proc.ProcessName) (PID: $($proc.Id)) - Command: $cmd"
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    } catch {
        # Ignore access denied errors
    }
}

# Wait for locks to release
Start-Sleep -Seconds 2

# Move vls-server to Recycle Bin
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
