$ErrorActionPreference = "Stop"

$GradleVersion = "9.3.1"
$ExpectedSha256 = "b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13"
$ProjectDir = Split-Path -Parent $PSScriptRoot
$WrapperDir = Join-Path $ProjectDir "gradle\wrapper"
$WrapperJar = Join-Path $WrapperDir "gradle-wrapper.jar"
$DownloadUrl = "https://raw.githubusercontent.com/gradle/gradle/v$GradleVersion/gradle/wrapper/gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path $WrapperDir | Out-Null

function Assert-WrapperChecksum([string]$Path) {
    $Actual = (Get-FileHash -Path $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($Actual -ne $ExpectedSha256) {
        throw "SHA-256 Gradle Wrapper tidak cocok. Expected: $ExpectedSha256; Actual: $Actual"
    }
}

if (Test-Path $WrapperJar) {
    Assert-WrapperChecksum $WrapperJar
    Write-Host "Gradle Wrapper $GradleVersion terverifikasi."
    exit 0
}

$TempFile = "$WrapperJar.tmp.$PID"
try {
    Write-Host "Mengunduh Gradle Wrapper $GradleVersion dari repository resmi Gradle..."
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $TempFile -MaximumRedirection 10
    Assert-WrapperChecksum $TempFile
    Move-Item -Force $TempFile $WrapperJar
    Write-Host "Gradle Wrapper $GradleVersion berhasil dipasang dan SHA-256 terverifikasi."
} finally {
    if (Test-Path $TempFile) { Remove-Item -Force $TempFile }
}
