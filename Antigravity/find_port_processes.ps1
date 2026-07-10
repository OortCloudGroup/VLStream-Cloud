# [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Get-Process | ForEach-Object {
    try {
        $cmd = $_.CommandLine
        if ($cmd -and ($cmd.Contains("3001") -or $cmd.Contains("3002"))) {
            [PSCustomObject]@{
                Id          = $_.Id
                ProcessName = $_.ProcessName
                CommandLine = $cmd
            }
        }
    } catch {
        # Ignore errors accessing properties of system/protected processes
    }
} | Format-List
