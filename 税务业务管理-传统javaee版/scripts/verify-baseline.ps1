param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string]$Message)
    $failures.Add($Message)
}

$pomPath = Join-Path $ProjectRoot 'pom.xml'
$dockerfilePath = Join-Path $ProjectRoot 'Dockerfile'
$composePath = Join-Path $ProjectRoot 'compose.yaml'
$sourcePath = Join-Path $ProjectRoot 'src'

$pom = Get-Content -LiteralPath $pomPath -Raw
$dockerfile = Get-Content -LiteralPath $dockerfilePath -Raw
$compose = Get-Content -LiteralPath $composePath -Raw

if ($pom -notmatch '<packaging>war</packaging>') {
    Add-Failure 'pom.xml packaging must be war.'
}
if ($pom -notmatch '<maven\.compiler\.release>11</maven\.compiler\.release>') {
    Add-Failure 'maven.compiler.release must be 11.'
}
if ($pom -notmatch '<groupId>javax\.servlet</groupId>') {
    Add-Failure 'javax.servlet-api dependency is missing.'
}
if ($dockerfile -notmatch 'eclipse-temurin-11') {
    Add-Failure 'Docker Maven build must use Eclipse Temurin 11.'
}
if ($dockerfile -notmatch 'eclipse-temurin:11-jre') {
    Add-Failure 'Docker runtime must use Eclipse Temurin 11 JRE.'
}
if ($dockerfile -notmatch 'apache-tomcat-9\.0\.123\.zip') {
    Add-Failure 'Docker runtime must use Tomcat 9.0.123.'
}
if ($compose -notmatch 'mysql:8\.0\.46') {
    Add-Failure 'compose.yaml must pin MySQL 8.0.46.'
}

$badWebPrefix = 'jak' + 'arta.'
$sourceFiles = Get-ChildItem -LiteralPath $sourcePath -Recurse -File -Include '*.java', '*.jsp', '*.jspf'
$badImports = $sourceFiles | Select-String -SimpleMatch $badWebPrefix
if ($badImports) {
    Add-Failure "Forbidden Java Web namespace found in $($badImports.Count) source lines."
}

$warPath = Join-Path $ProjectRoot 'target\tax-business-system.war'
if (Test-Path -LiteralPath $warPath) {
    $entries = & jar tf $warPath
    if ($LASTEXITCODE -ne 0) {
        Add-Failure 'Unable to inspect the WAR with jar tf.'
    } else {
        $badJars = $entries | Select-String -SimpleMatch $badWebPrefix
        if ($badJars) {
            Add-Failure "Forbidden runtime JAR or class found in WAR: $($badJars -join ', ')."
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Host 'Java EE 8 baseline check FAILED:' -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host 'Java EE 8 baseline check PASSED.' -ForegroundColor Green
Write-Host 'Java 11 | Servlet 4.0 / javax.servlet | Tomcat 9.0.123 | Maven 3.8+ | MySQL 8.0.46'
