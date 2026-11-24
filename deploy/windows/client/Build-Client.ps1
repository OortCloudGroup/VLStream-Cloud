#Requires -Version 7.0
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string] $OutputDirectory,

    [string] $SdkArchive
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$webView2Version = '1.0.3537.50'
$webView2Sha256 = '5EA526BBD728ADDA0DA4D31219267E96460494A427E4894C4E09D9F320F4B9AA'
$packageName = "microsoft.web.webview2.$webView2Version.nupkg"
$compiler = Join-Path $env:SystemRoot 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
if (-not (Test-Path -LiteralPath $compiler -PathType Leaf)) {
    throw 'The x64 Windows .NET Framework compiler was not found.'
}

$output = New-Item -Path $OutputDirectory -ItemType Directory -Force
if (-not $SdkArchive) {
    $cache = New-Item -Path (Join-Path $output.Parent.FullName 'sdk') -ItemType Directory -Force
    $SdkArchive = Join-Path $cache.FullName $packageName
    if (-not (Test-Path -LiteralPath $SdkArchive -PathType Leaf)) {
        $downloadFile = "$SdkArchive.download"
        try {
            $url = "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/$webView2Version/$packageName"
            Invoke-WebRequest -Uri $url -OutFile $downloadFile
            if ((Get-FileHash -LiteralPath $downloadFile -Algorithm SHA256).Hash -ne $webView2Sha256) {
                throw 'The downloaded Microsoft WebView2 SDK has an unexpected SHA-256 digest.'
            }
            Move-Item -LiteralPath $downloadFile -Destination $SdkArchive
        }
        finally {
            if (Test-Path -LiteralPath $downloadFile -PathType Leaf) {
                Remove-Item -LiteralPath $downloadFile
            }
        }
    }
}

$SdkArchive = (Resolve-Path -LiteralPath $SdkArchive).Path
if ((Get-FileHash -LiteralPath $SdkArchive -Algorithm SHA256).Hash -ne $webView2Sha256) {
    throw "Unexpected Microsoft WebView2 SDK SHA-256 digest in $SdkArchive."
}

$clientFiles = [ordered]@{
    'Microsoft.Web.WebView2.Core.dll' = 'lib/net462/Microsoft.Web.WebView2.Core.dll'
    'Microsoft.Web.WebView2.WinForms.dll' = 'lib/net462/Microsoft.Web.WebView2.WinForms.dll'
    'WebView2Loader.dll' = 'runtimes/win-x64/native/WebView2Loader.dll'
    'WebView2-LICENSE.txt' = 'LICENSE.txt'
    'WebView2-NOTICE.txt' = 'NOTICE.txt'
}

$archive = [System.IO.Compression.ZipFile]::OpenRead($SdkArchive)
try {
    foreach ($clientFile in $clientFiles.GetEnumerator()) {
        $entry = $archive.GetEntry($clientFile.Value)
        if ($null -eq $entry) {
            throw "Expected file $($clientFile.Value) is missing from the WebView2 SDK."
        }
        $destination = Join-Path $output.FullName $clientFile.Key
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
    }
}
finally {
    $archive.Dispose()
}

$executable = Join-Path $output.FullName 'VLStream.exe'
$coreAssembly = Join-Path $output.FullName 'Microsoft.Web.WebView2.Core.dll'
$winFormsAssembly = Join-Path $output.FullName 'Microsoft.Web.WebView2.WinForms.dll'
$source = Join-Path $PSScriptRoot 'VLStream.cs'
& $compiler /nologo /target:winexe /platform:x64 /optimize+ /codepage:65001 `
    "/out:$executable" /reference:System.Windows.Forms.dll /reference:System.Drawing.dll `
    "/reference:$coreAssembly" "/reference:$winFormsAssembly" $source
if ($LASTEXITCODE -ne 0) {
    throw "VLStream client compilation failed with exit code $LASTEXITCODE."
}

Get-Item -LiteralPath $executable
