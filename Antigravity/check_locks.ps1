# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$paths = @(
    "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean",
    "D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server"
)

foreach ($path in $paths) {
    if (Test-Path $path) {
        Write-Host "Checking path: $path"
        Get-ChildItem -Path $path -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
            $fullName = $_.FullName
            if (-not $_.PSIsContainer) {
                try {
                    $fileStream = [System.IO.File]::OpenWrite($fullName)
                    $fileStream.Close()
                } catch {
                    Write-Host "Locked file: $fullName"
                    Write-Host "Error: $_"
                }
            }
        }
    } else {
        Write-Host "Path does not exist: $path"
    }
}
