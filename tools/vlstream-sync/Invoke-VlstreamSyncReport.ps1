[CmdletBinding()]
param(
    [string]$SourceRepositoryRoot,
    [string]$TargetRepositoryRoot,
    [string]$BaseCommit,
    [string]$OutputDirectory,
    [switch]$Inventory,
    [switch]$CommittedOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Repository,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = @(& git -C $Repository @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Git命令执行失败: git -C `"$Repository`" $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return @($output | ForEach-Object { $_.ToString() })
}

function Convert-ToGitPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return $Path.Replace("\", "/").TrimStart("/")
}

function Resolve-AbsolutePath {
    param(
        [Parameter(Mandatory = $true)][string]$BasePath,
        [Parameter(Mandatory = $true)][string]$Path
    )

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }
    return [System.IO.Path]::GetFullPath((Join-Path $BasePath $Path))
}

function Test-PathPattern {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$Patterns
    )

    foreach ($pattern in $Patterns) {
        if ($Path -like (Convert-ToGitPath $pattern)) {
            return $true
        }
    }
    return $false
}

function Find-Rule {
    param(
        [Parameter(Mandatory = $true)][string]$ModuleRelativePath,
        [Parameter(Mandatory = $true)][object[]]$Rules
    )

    foreach ($rule in $Rules) {
        if (Test-PathPattern -Path $ModuleRelativePath -Patterns @($rule.sourcePatterns)) {
            return $rule
        }
    }
    return $null
}

function Convert-NameStatusLines {
    param(
        [Parameter(Mandatory = $true)]
        [AllowNull()]
        [AllowEmptyCollection()]
        [string[]]$Lines,
        [Parameter(Mandatory = $true)][string]$Origin
    )

    $records = [System.Collections.Generic.List[object]]::new()
    foreach ($line in @($Lines)) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $fields = $line -split "`t"
        if ($fields.Count -lt 2) {
            continue
        }

        $status = $fields[0]
        $previousPath = $null
        $path = $fields[1]
        if (($status.StartsWith("R") -or $status.StartsWith("C")) -and $fields.Count -ge 3) {
            $previousPath = Convert-ToGitPath $fields[1]
            $path = $fields[2]
        }

        $records.Add([pscustomobject]@{
            Status       = $status
            Path         = Convert-ToGitPath $path
            PreviousPath = $previousPath
            Origin       = $Origin
        })
    }
    return @($records)
}

function Merge-ChangeRecords {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [object[]]$Records
    )

    $merged = @{}
    foreach ($record in $Records) {
        if ($merged.ContainsKey($record.Path)) {
            $existing = $merged[$record.Path]
            $existing.Origin = "$($existing.Origin)+$($record.Origin)"
            $existing.Status = $record.Status
            if ($null -ne $record.PreviousPath) {
                $existing.PreviousPath = $record.PreviousPath
            }
        } else {
            $merged[$record.Path] = $record
        }
    }
    return @($merged.Values | Sort-Object Path)
}

function Convert-ToMarkdownCell {
    param([AllowNull()][object]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return $Value.ToString().Replace("|", "\|").Replace("`r", " ").Replace("`n", " ")
}

$toolRoot = $PSScriptRoot
$manifestPath = Join-Path $toolRoot "sync-manifest.json"
$baselinePath = Join-Path $toolRoot "sync-baseline.json"
$manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
$baseline = Get-Content -Raw -LiteralPath $baselinePath | ConvertFrom-Json

if ([string]::IsNullOrWhiteSpace($SourceRepositoryRoot)) {
    $SourceRepositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $toolRoot "../.."))
} else {
    $SourceRepositoryRoot = [System.IO.Path]::GetFullPath($SourceRepositoryRoot)
}

$actualSourceRoot = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @("rev-parse", "--show-toplevel") |
    Select-Object -First 1
$SourceRepositoryRoot = [System.IO.Path]::GetFullPath($actualSourceRoot)

if ([string]::IsNullOrWhiteSpace($TargetRepositoryRoot)) {
    $TargetRepositoryRoot = Resolve-AbsolutePath -BasePath $SourceRepositoryRoot -Path $manifest.targetRepositoryRelativePath
} else {
    $TargetRepositoryRoot = [System.IO.Path]::GetFullPath($TargetRepositoryRoot)
}

if (-not (Test-Path -LiteralPath $TargetRepositoryRoot -PathType Container)) {
    throw "多租户仓库不存在: $TargetRepositoryRoot"
}
$actualTargetRoot = Invoke-Git -Repository $TargetRepositoryRoot -Arguments @("rev-parse", "--show-toplevel") |
    Select-Object -First 1
$TargetRepositoryRoot = [System.IO.Path]::GetFullPath($actualTargetRoot)

$sourceModulePath = Resolve-AbsolutePath -BasePath $SourceRepositoryRoot -Path $manifest.sourceModule
$sourceModuleGitPath = Convert-ToGitPath $manifest.sourceModule
if (-not (Test-Path -LiteralPath $sourceModulePath -PathType Container)) {
    throw "单租户业务模块不存在: $sourceModulePath"
}

if ([string]::IsNullOrWhiteSpace($BaseCommit)) {
    $BaseCommit = $baseline.sourceCommit
}
[void](Invoke-Git -Repository $SourceRepositoryRoot -Arguments @("rev-parse", "--verify", "$BaseCommit^{commit}"))
$sourceHead = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @("rev-parse", "HEAD") |
    Select-Object -First 1
$targetHead = Invoke-Git -Repository $TargetRepositoryRoot -Arguments @("rev-parse", "HEAD") |
    Select-Object -First 1

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $SourceRepositoryRoot "codex/sync"
} else {
    $OutputDirectory = Resolve-AbsolutePath -BasePath $SourceRepositoryRoot -Path $OutputDirectory
}
[void](New-Item -ItemType Directory -Force -Path $OutputDirectory)

$changeRecords = [System.Collections.Generic.List[object]]::new()
if ($Inventory) {
    $trackedFiles = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @(
        "ls-files",
        "--",
        $sourceModuleGitPath
    )
    foreach ($trackedFile in $trackedFiles) {
        $changeRecords.Add([pscustomobject]@{
            Status       = "I"
            Path         = Convert-ToGitPath $trackedFile
            PreviousPath = $null
            Origin       = "inventory"
        })
    }
} else {
    $committedLines = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @(
        "diff",
        "--name-status",
        "--find-renames",
        "$BaseCommit..HEAD",
        "--",
        $sourceModuleGitPath
    )
    foreach ($record in (Convert-NameStatusLines -Lines $committedLines -Origin "committed")) {
        $changeRecords.Add($record)
    }

    if (-not $CommittedOnly) {
        $workingLines = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @(
            "diff",
            "--name-status",
            "--find-renames",
            "HEAD",
            "--",
            $sourceModuleGitPath
        )
        foreach ($record in (Convert-NameStatusLines -Lines $workingLines -Origin "working-tree")) {
            $changeRecords.Add($record)
        }

        $untrackedFiles = Invoke-Git -Repository $SourceRepositoryRoot -Arguments @(
            "ls-files",
            "--others",
            "--exclude-standard",
            "--",
            $sourceModuleGitPath
        )
        foreach ($untrackedFile in $untrackedFiles) {
            $changeRecords.Add([pscustomobject]@{
                Status       = "U"
                Path         = Convert-ToGitPath $untrackedFile
                PreviousPath = $null
                Origin       = "working-tree"
            })
        }
    }
}
$changes = Merge-ChangeRecords -Records @($changeRecords)

$targetIndex = @{}
foreach ($targetSearchRoot in @($manifest.targetSearchRoots)) {
    $absoluteSearchRoot = Resolve-AbsolutePath -BasePath $TargetRepositoryRoot -Path $targetSearchRoot
    if (-not (Test-Path -LiteralPath $absoluteSearchRoot -PathType Container)) {
        continue
    }

    foreach ($targetFile in (Get-ChildItem -LiteralPath $absoluteSearchRoot -Recurse -File)) {
        if (-not $targetIndex.ContainsKey($targetFile.Name)) {
            $targetIndex[$targetFile.Name] = [System.Collections.Generic.List[string]]::new()
        }
        $relativeTargetPath = [System.IO.Path]::GetRelativePath($TargetRepositoryRoot, $targetFile.FullName)
        $targetIndex[$targetFile.Name].Add((Convert-ToGitPath $relativeTargetPath))
    }
}

$details = [System.Collections.Generic.List[object]]::new()
foreach ($change in $changes) {
    if (-not $change.Path.StartsWith("$sourceModuleGitPath/", [System.StringComparison]::OrdinalIgnoreCase)) {
        continue
    }

    $moduleRelativePath = $change.Path.Substring($sourceModuleGitPath.Length + 1)
    $excludeRule = Find-Rule -ModuleRelativePath $moduleRelativePath -Rules @($manifest.excludeRules)
    $candidateRule = Find-Rule -ModuleRelativePath $moduleRelativePath -Rules @($manifest.candidateRules)
    $classification = "out-of-scope"
    $ruleId = ""
    $ruleDescription = "未匹配同步规则"
    if ($null -ne $excludeRule) {
        $classification = "excluded"
        $ruleId = $excludeRule.id
        $ruleDescription = $excludeRule.description
    } elseif ($null -ne $candidateRule) {
        $classification = "candidate"
        $ruleId = $candidateRule.id
        $ruleDescription = $candidateRule.description
    }

    $sourceAbsolutePath = Join-Path $SourceRepositoryRoot $change.Path
    $fileName = [System.IO.Path]::GetFileName($change.Path)
    $targetMatches = @()
    if ($targetIndex.ContainsKey($fileName)) {
        $targetMatches = @($targetIndex[$fileName])
    }

    $targetMatchStatus = "missing"
    if ($targetMatches.Count -eq 1) {
        $targetMatchStatus = "matched"
    } elseif ($targetMatches.Count -gt 1) {
        $targetMatchStatus = "ambiguous"
    }

    $contentStatus = "not-compared"
    if ((Test-Path -LiteralPath $sourceAbsolutePath -PathType Leaf) -and $targetMatches.Count -eq 1) {
        $targetAbsolutePath = Join-Path $TargetRepositoryRoot $targetMatches[0]
        $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceAbsolutePath).Hash
        $targetHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $targetAbsolutePath).Hash
        $contentStatus = if ($sourceHash -eq $targetHash) { "identical" } else { "different" }
    } elseif (-not (Test-Path -LiteralPath $sourceAbsolutePath -PathType Leaf)) {
        $contentStatus = "source-deleted"
    }

    $risks = [System.Collections.Generic.List[string]]::new()
    if (Test-Path -LiteralPath $sourceAbsolutePath -PathType Leaf) {
        $sourceContent = Get-Content -Raw -LiteralPath $sourceAbsolutePath
        foreach ($riskPattern in @($manifest.riskPatterns)) {
            if ([regex]::IsMatch($sourceContent, $riskPattern.pattern)) {
                $risks.Add($riskPattern.id)
            }
        }
    }

    $details.Add([pscustomobject]@{
        classification    = $classification
        rule              = $ruleId
        ruleDescription   = $ruleDescription
        status            = $change.Status
        origin            = $change.Origin
        sourcePath        = $change.Path
        previousPath      = $change.PreviousPath
        targetMatchStatus = $targetMatchStatus
        targetPaths       = @($targetMatches)
        contentStatus     = $contentStatus
        risks             = @($risks)
    })
}

$candidateDetails = @($details | Where-Object classification -eq "candidate")
$excludedDetails = @($details | Where-Object classification -eq "excluded")
$outOfScopeDetails = @($details | Where-Object classification -eq "out-of-scope")
$mode = if ($Inventory) { "全量盘点" } else { "基线增量" }
$generatedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"

$reportLines = [System.Collections.Generic.List[string]]::new()
$reportLines.Add("# VLStream代码同步报告")
$reportLines.Add("")
$reportLines.Add("- 生成时间：$generatedAt")
$reportLines.Add("- 扫描模式：$mode")
$reportLines.Add("- 单租户仓库：``$SourceRepositoryRoot``")
$reportLines.Add("- 单租户HEAD：``$sourceHead``")
$reportLines.Add("- 多租户仓库：``$TargetRepositoryRoot``")
$reportLines.Add("- 多租户HEAD：``$targetHead``")
$reportLines.Add("- 比较基线：``$BaseCommit``")
$reportLines.Add("- 基线状态：$(if ($baseline.confirmed) { '已确认' } else { '待确认，不能视为已完成同步' })")
$reportLines.Add("- 执行约束：只读业务代码；未创建分支；未复制或修改多租户文件")
$reportLines.Add("")
$reportLines.Add("## 汇总")
$reportLines.Add("")
$reportLines.Add("| 项目 | 数量 |")
$reportLines.Add("| --- | ---: |")
$reportLines.Add("| 扫描文件/变化 | $($details.Count) |")
$reportLines.Add("| 核心同步候选 | $($candidateDetails.Count) |")
$reportLines.Add("| 明确排除 | $($excludedDetails.Count) |")
$reportLines.Add("| 未匹配规则 | $($outOfScopeDetails.Count) |")
$reportLines.Add("| 候选中找到唯一同名目标 | $(@($candidateDetails | Where-Object targetMatchStatus -eq 'matched').Count) |")
$reportLines.Add("| 候选中目标缺失 | $(@($candidateDetails | Where-Object targetMatchStatus -eq 'missing').Count) |")
$reportLines.Add("| 候选中目标重名 | $(@($candidateDetails | Where-Object targetMatchStatus -eq 'ambiguous').Count) |")
$reportLines.Add("")
$reportLines.Add("## 核心同步候选")
$reportLines.Add("")
if ($candidateDetails.Count -eq 0) {
    $reportLines.Add("当前范围内没有需要审核的核心业务变化。")
} else {
    $reportLines.Add("| 状态 | 规则 | 来源文件 | 目标匹配 | 内容 | 风险提示 |")
    $reportLines.Add("| --- | --- | --- | --- | --- | --- |")
    foreach ($detail in $candidateDetails) {
        $targetText = if ($detail.targetPaths.Count -eq 0) {
            "缺失"
        } elseif ($detail.targetPaths.Count -eq 1) {
            $detail.targetPaths[0]
        } else {
            "重名: " + ($detail.targetPaths -join ", ")
        }
        $reportLines.Add("| $(Convert-ToMarkdownCell $detail.status) | $(Convert-ToMarkdownCell $detail.rule) | ``$(Convert-ToMarkdownCell $detail.sourcePath)`` | $(Convert-ToMarkdownCell $targetText) | $(Convert-ToMarkdownCell $detail.contentStatus) | $(Convert-ToMarkdownCell ($detail.risks -join ', ')) |")
    }
}
$reportLines.Add("")
$reportLines.Add("## 明确排除")
$reportLines.Add("")
if ($excludedDetails.Count -eq 0) {
    $reportLines.Add("本次没有命中排除规则的变化。")
} else {
    $reportLines.Add("| 规则 | 原因 | 来源文件 |")
    $reportLines.Add("| --- | --- | --- |")
    foreach ($detail in $excludedDetails) {
        $reportLines.Add("| $(Convert-ToMarkdownCell $detail.rule) | $(Convert-ToMarkdownCell $detail.ruleDescription) | ``$(Convert-ToMarkdownCell $detail.sourcePath)`` |")
    }
}
$reportLines.Add("")
$reportLines.Add("## 未匹配规则")
$reportLines.Add("")
if ($outOfScopeDetails.Count -eq 0) {
    $reportLines.Add("所有扫描内容均已被同步规则或排除规则覆盖。")
} else {
    foreach ($detail in $outOfScopeDetails) {
        $reportLines.Add("- ``$(Convert-ToMarkdownCell $detail.sourcePath)``")
    }
}
$reportLines.Add("")
$reportLines.Add("## 审核原则")
$reportLines.Add("")
$reportLines.Add("1. 本报告只负责识别变化和定位同名文件，不表示两个文件可以直接覆盖。")
$reportLines.Add("2. 出现若依、SpringBlade、MyBatis或Spring Web依赖时，必须人工确认框架和租户差异。")
$reportLines.Add("3. Entity、Mapper、Controller、SQL和租户代码不进入第一阶段自动同步范围。")
$reportLines.Add("4. 基线只有在对应功能已完成多租户适配并审核后才能更新。")

$reportPath = Join-Path $OutputDirectory "vlstream-sync-report.md"
$jsonPath = Join-Path $OutputDirectory "vlstream-sync-details.json"
[System.IO.File]::WriteAllLines($reportPath, $reportLines, [System.Text.UTF8Encoding]::new($false))
$jsonOutput = [pscustomobject]@{
    generatedAt      = $generatedAt
    mode             = $mode
    sourceRepository = $SourceRepositoryRoot
    sourceHead       = $sourceHead
    targetRepository = $TargetRepositoryRoot
    targetHead       = $targetHead
    baseCommit       = $BaseCommit
    baselineConfirmed = [bool]$baseline.confirmed
    summary          = [pscustomobject]@{
        scanned    = $details.Count
        candidates = $candidateDetails.Count
        excluded   = $excludedDetails.Count
        outOfScope = $outOfScopeDetails.Count
    }
    details          = @($details)
} | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText($jsonPath, $jsonOutput, [System.Text.UTF8Encoding]::new($false))

Write-Output "同步报告已生成：$reportPath"
Write-Output "结构化明细已生成：$jsonPath"
Write-Output "候选=$($candidateDetails.Count) 排除=$($excludedDetails.Count) 未匹配=$($outOfScopeDetails.Count)"
