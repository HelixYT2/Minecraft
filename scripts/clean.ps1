# Helix Bridge Clean Script

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ModDir = Join-Path $RepoRoot "mod-fabric"
$HubDir = Join-Path $RepoRoot "hub-desktop"
$DistDir = Join-Path $RepoRoot "dist"

Write-Host "Cleaning build artifacts..." -ForegroundColor Yellow

# Clean mod
if (Test-Path (Join-Path $ModDir "build")) {
    Write-Host "Cleaning mod-fabric/build..."
    Remove-Item -Recurse -Force (Join-Path $ModDir "build")
}

if (Test-Path (Join-Path $ModDir ".gradle")) {
    Write-Host "Cleaning mod-fabric/.gradle..."
    Remove-Item -Recurse -Force (Join-Path $ModDir ".gradle")
}

# Clean hub
$hubBin = Join-Path $HubDir "HelixHub" "bin"
$hubObj = Join-Path $HubDir "HelixHub" "obj"
$testBin = Join-Path $HubDir "HelixHub.Tests" "bin"
$testObj = Join-Path $HubDir "HelixHub.Tests" "obj"

foreach ($dir in @($hubBin, $hubObj, $testBin, $testObj)) {
    if (Test-Path $dir) {
        Write-Host "Cleaning $dir..."
        Remove-Item -Recurse -Force $dir
    }
}

# Clean dist (but keep .gitkeep)
if (Test-Path $DistDir) {
    Get-ChildItem -Path $DistDir -File | Where-Object { $_.Name -ne ".gitkeep" } | Remove-Item -Force
}

Write-Host "Clean complete!" -ForegroundColor Green
