# ByteHR AI – Rebuild and Start Backend
# Run this script in PowerShell on Windows

$ErrorActionPreference = "Stop"
$java = "C:\Program Files\Java\jdk-21\bin\java.exe"
$mvn  = "C:\Program Files\Apache\Maven\apache-maven-3.9.12\bin\mvn.cmd"
$wslSrc = "\\wsl.localhost\Ubuntu\home\tgode\env\byte-me"
$buildDir = "C:\Temp\bytehr-build"
$jarDest = "\\wsl.localhost\Ubuntu\home\tgode\env\byte-me\target"

Write-Host "=== ByteHR AI Rebuild ===" -ForegroundColor Cyan

# 1. Kill any existing backend
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

# 2. Copy source
Write-Host "[1/4] Copying source files..." -ForegroundColor Yellow
if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Path $buildDir | Out-Null
Copy-Item "$wslSrc\src"   $buildDir -Recurse
Copy-Item "$wslSrc\pom.xml" $buildDir

# 3. Maven build
Write-Host "[2/4] Building with Maven..." -ForegroundColor Yellow
Set-Location $buildDir
& $mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }

# 4. Copy JAR back
Write-Host "[3/4] Copying JAR..." -ForegroundColor Yellow
if (-not (Test-Path $jarDest)) { New-Item -ItemType Directory -Path $jarDest | Out-Null }
Copy-Item "$buildDir\target\*.jar" $jarDest -Force

# 5. Start backend
Write-Host "[4/4] Starting backend..." -ForegroundColor Yellow
$jarPath = (Get-ChildItem "$jarDest\*.jar" | Select-Object -First 1).FullName
$args = @(
    "-jar", $jarPath,
    "--spring.datasource.url=jdbc:postgresql://localhost:5432/bytehr",
    "--spring.datasource.username=bytehr",
    "--spring.datasource.password=bytehr",
    "--ollama.base-url=http://[::1]:11434",
    "--sharepoint.sync-enabled=false",
    "--sharepoint.tenant-id=placeholder",
    "--sharepoint.client-id=placeholder",
    "--sharepoint.client-secret=placeholder",
    "--sharepoint.site-id=placeholder",
    "--sharepoint.drive-id=placeholder",
    "--teams.app-id=placeholder",
    "--teams.app-password=placeholder"
)
$proc = Start-Process -FilePath $java -ArgumentList $args `
    -WindowStyle Hidden `
    -RedirectStandardOutput "C:\Temp\bytehr-backend.log" `
    -RedirectStandardError  "C:\Temp\bytehr-backend-err.log" `
    -PassThru
Write-Host "Backend started as PID $($proc.Id)" -ForegroundColor Green
Write-Host "Logs: C:\Temp\bytehr-backend.log" -ForegroundColor Gray
Write-Host "Waiting 75s for startup..."
Start-Sleep -Seconds 75
try {
    $r = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5
    Write-Host "Health: $($r.status)" -ForegroundColor Green
} catch {
    Write-Host "Health check failed – check C:\Temp\bytehr-backend-err.log" -ForegroundColor Red
}
