# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Get-Process | ForEach-Object {
    try {
        $cmd = $_.CommandLine
        if ($cmd -and ($cmd.Contains("vls-server") -or $cmd.Contains("VLStream-Cloud"))) {
            Write-Host "Process Name: $($_.ProcessName) | PID: $($_.Id) | Command: $cmd"
        }
    } catch {
        # Ignore
    }
}
