# Helix Bridge Build Script
# Builds both the Fabric mod and Desktop Hub

param(
    [switch]$SkipMod,
    [switch]$SkipHub,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ModDir = Join-Path $RepoRoot "mod-fabric"
$HubDir = Join-Path $RepoRoot "hub-desktop"
$DistDir = Join-Path $RepoRoot "dist"
$SharedDir = Join-Path $RepoRoot "shared"

Write-Host "=== Helix Bridge Build Script ===" -ForegroundColor Cyan
Write-Host "Repository root: $RepoRoot"
Write-Host ""

# Create dist directory
if (!(Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir | Out-Null
}

# Clean if requested
if ($Clean) {
    Write-Host "Cleaning build artifacts..." -ForegroundColor Yellow
    
    if (Test-Path (Join-Path $ModDir "build")) {
        Remove-Item -Recurse -Force (Join-Path $ModDir "build")
    }
    if (Test-Path (Join-Path $HubDir "HelixHub" "bin")) {
        Remove-Item -Recurse -Force (Join-Path $HubDir "HelixHub" "bin")
    }
    if (Test-Path (Join-Path $HubDir "HelixHub" "obj")) {
        Remove-Item -Recurse -Force (Join-Path $HubDir "HelixHub" "obj")
    }
    
    Get-ChildItem -Path $DistDir -File | Remove-Item -Force
    
    Write-Host "Clean complete." -ForegroundColor Green
    Write-Host ""
}

# Build Fabric Mod
if (!$SkipMod) {
    Write-Host "=== Building Fabric Mod ===" -ForegroundColor Cyan
    
    Push-Location $ModDir
    try {
        # Check for Java
        $javaVersion = & java -version 2>&1 | Select-String "version"
        Write-Host "Java: $javaVersion"
        
        # Run Gradle build
        Write-Host "Running Gradle build..."
        
        if ($IsWindows -or $env:OS -match "Windows") {
            & .\gradlew.bat build --no-daemon
        } else {
            & ./gradlew build --no-daemon
        }
        
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
        
        # Find and copy the JAR
        $jarFile = Get-ChildItem -Path "build\libs" -Filter "helix-bridge-*.jar" | 
                   Where-Object { $_.Name -notmatch "sources|javadoc" } |
                   Sort-Object LastWriteTime -Descending |
                   Select-Object -First 1
        
        if ($jarFile) {
            $destPath = Join-Path $DistDir "HelixMinecraftBridge-1.21.1.jar"
            Copy-Item $jarFile.FullName $destPath -Force
            Write-Host "Mod JAR copied to: $destPath" -ForegroundColor Green
        } else {
            throw "Could not find built JAR file"
        }
    }
    finally {
        Pop-Location
    }
    
    Write-Host ""
}

# Build Desktop Hub
if (!$SkipHub) {
    Write-Host "=== Building Desktop Hub ===" -ForegroundColor Cyan
    
    Push-Location $HubDir
    try {
        # Check for .NET SDK
        $dotnetVersion = & dotnet --version 2>&1
        Write-Host ".NET SDK: $dotnetVersion"
        
        # Copy plan schema to output
        $schemaSource = Join-Path $SharedDir "plan.schema.json"
        $schemaDest = Join-Path $HubDir "HelixHub" "plan.schema.json"
        if (Test-Path $schemaSource) {
            Copy-Item $schemaSource $schemaDest -Force
        }
        
        # Restore packages
        Write-Host "Restoring NuGet packages..."
        & dotnet restore HelixHub.sln
        
        if ($LASTEXITCODE -ne 0) {
            throw "NuGet restore failed"
        }
        
        # Build and publish
        Write-Host "Publishing single-file executable..."
        & dotnet publish HelixHub/HelixHub.csproj `
            -c Release `
            -r win-x64 `
            --self-contained true `
            -p:PublishSingleFile=true `
            -p:IncludeNativeLibrariesForSelfExtract=true `
            -p:EnableCompressionInSingleFile=true `
            -o (Join-Path $DistDir "hub-publish")
        
        if ($LASTEXITCODE -ne 0) {
            throw "dotnet publish failed"
        }
        
        # Copy the exe to dist
        $exeSource = Join-Path $DistDir "hub-publish" "HelixHub.exe"
        $exeDest = Join-Path $DistDir "HelixHub.exe"
        
        if (Test-Path $exeSource) {
            Copy-Item $exeSource $exeDest -Force
            Write-Host "Hub EXE copied to: $exeDest" -ForegroundColor Green
            
            # Copy schema alongside exe
            if (Test-Path $schemaSource) {
                Copy-Item $schemaSource (Join-Path $DistDir "plan.schema.json") -Force
            }
            
            # Clean up publish folder
            Remove-Item -Recurse -Force (Join-Path $DistDir "hub-publish")
        } else {
            throw "Could not find published exe at $exeSource"
        }
    }
    finally {
        Pop-Location
    }
    
    Write-Host ""
}

# Run tests
Write-Host "=== Running Tests ===" -ForegroundColor Cyan
Push-Location $HubDir
try {
    & dotnet test HelixHub.Tests/HelixHub.Tests.csproj --no-build -v minimal
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: Some tests failed" -ForegroundColor Yellow
    }
}
finally {
    Pop-Location
}

Write-Host ""

# Summary
Write-Host "=== Build Summary ===" -ForegroundColor Cyan
Write-Host "Output directory: $DistDir"
Write-Host ""

$outputs = @(
    @{ Name = "HelixMinecraftBridge-1.21.1.jar"; Required = !$SkipMod },
    @{ Name = "HelixHub.exe"; Required = !$SkipHub }
)

$allPresent = $true
foreach ($output in $outputs) {
    $path = Join-Path $DistDir $output.Name
    if (Test-Path $path) {
        $size = (Get-Item $path).Length / 1MB
        Write-Host "  [OK] $($output.Name) ({0:N2} MB)" -f $size -ForegroundColor Green
    } elseif ($output.Required) {
        Write-Host "  [MISSING] $($output.Name)" -ForegroundColor Red
        $allPresent = $false
    } else {
        Write-Host "  [SKIPPED] $($output.Name)" -ForegroundColor Yellow
    }
}

Write-Host ""

if ($allPresent) {
    Write-Host "Build completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Build completed with errors." -ForegroundColor Red
    exit 1
}
