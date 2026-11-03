# Run from the root of EscapeShift, or from anywhere using this script's path.
param([int]$Level = 0)
$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Set-Location $projectRoot

Write-Host "1/3 - Static validation of all JSON files"
py tools/levels/level_tools.py check
if ($LASTEXITCODE -ne 0) { throw "JSON validation failed" }

Write-Host "2/3 - Real Kotlin physics + unit tests (Gradle)"
& .\gradlew.bat :app:testDebugUnitTest --rerun-tasks
if ($LASTEXITCODE -ne 0) { throw "Gradle test failed. Check app/build/reports/tests/testDebugUnitTest/index.html" }

Write-Host "3/3 - Virtual player with the V20 physics JAR and CSV reports"
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) { $javaExe = $java.Source }
else {
    $javaExe = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    if (-not (Test-Path $javaExe)) { throw "Java not found; use Android Studio JBR." }
}
$argsList = @("-jar", "tools/levels/level_simulator.jar", "app/src/main/assets/levels")
if ($Level -ge 1 -and $Level -le 20) { $argsList += "$Level" }
& $javaExe @argsList
if ($LASTEXITCODE -ne 0) { throw "The virtual player failed. Check the JSON and its route." }
Write-Host "PASS - all 20 levels are reachable. CSV: tools/levels/reports/"
