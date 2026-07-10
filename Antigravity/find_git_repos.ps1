[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$gitDirs = Get-ChildItem -Path "d:\work\ide\WorkSpace\VLStream-Cloud" -Filter ".git" -Recurse -Force -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer }
foreach ($gitDir in $gitDirs) {
    $repoPath = $gitDir.Parent.FullName
    Write-Output "=================================================="
    Write-Output "Repository: $repoPath"
    
    # Run git remote -v in the repository directory
    $remotes = git -C $repoPath remote -v
    if ($remotes) {
        foreach ($remote in $remotes) {
            Write-Output "  $remote"
        }
    } else {
        Write-Output "  No remotes found"
    }
}
