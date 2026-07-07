# Build script for Spleef plugin
# Use local JDK 25 if JAVA_HOME is not already set (e.g., in CI)
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Users\Matthew\OneDrive\Desktop\mc-plugins\jdk25"
}

mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}