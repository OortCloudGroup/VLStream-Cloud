# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$targetDirs = @(
    "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean",
    "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"
)

# 1. Kill java process 1076 if it is running
$javaProc = Get-Process -Id 1076 -ErrorAction SilentlyContinue
if ($javaProc) {
    Write-Host "Stopping Java process 1076 running from apaas-workflowforms-clean..."
    Stop-Process -Id 1076 -Force -ErrorAction SilentlyContinue
}

# 2. Scan all processes for target directory names in their command line
Write-Host "Scanning processes for target directories..."
$allProcs = Get-Process
foreach ($proc in $allProcs) {
    try {
        $cmdLine = $proc.CommandLine
        if ($cmdLine) {
            foreach ($dir in $targetDirs) {
                # Case insensitive check
                if ($cmdLine.ToLower().Contains($dir.ToLower()) -or $cmdLine.ToLower().Contains("vls-server") -or $cmdLine.ToLower().Contains("apaas-workflowforms-clean")) {
                    # Skip our own powershell/pwsh processes to avoid killing ourselves!
                    if ($proc.Id -eq $PID -or $proc.ProcessName -like "*pwsh*" -or $proc.ProcessName -like "*powershell*") {
                        continue
                    }
                    Write-Host "Stopping Process: $($proc.ProcessName) (PID: $($proc.Id)) - Command: $cmdLine"
                    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                }
            }
        }
    } catch {
        # Ignore permission/access errors for system processes
    }
}

# Wait a second for processes to release locks
Start-Sleep -Seconds 2

# 3. Move directories to Recycle Bin
Add-Type -AssemblyName 'Microsoft.VisualBasic'

foreach ($dir in $targetDirs) {
    if (Test-Path $dir) {
        Write-Host "Moving to Recycle Bin: $dir"
        try {
            [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory($dir, 'OnlyErrorDialogs', 'SendToRecycleBin')
            Write-Host "Successfully moved to Recycle Bin: $dir"
        } catch {
            Write-Host "Failed to delete: $dir"
            Write-Host "Error details: $_"
        }
    } else {
        Write-Host "Directory already deleted or does not exist: $dir"
    }
}
