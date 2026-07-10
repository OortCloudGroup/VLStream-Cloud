# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$shellNames = @("cmd", "pwsh", "powershell", "bash", "wsl", "ssh")
$results = @()
Get-Process | Where-Object { $shellNames -contains $_.ProcessName } | ForEach-Object {
    $cmd = ""
    try { $cmd = $_.CommandLine } catch {}
    $results += [PSCustomObject]@{
        Id          = $_.Id
        ProcessName = $_.ProcessName
        CommandLine = $cmd
    }
}
$results | ConvertTo-Json
