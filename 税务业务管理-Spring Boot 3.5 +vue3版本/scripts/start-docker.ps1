param(
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

function Read-EnvValue {
    param(
        [string]$Name,
        [string]$DefaultValue
    )

    $processValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($processValue)) {
        return $processValue
    }

    $envFile = Join-Path $projectRoot '.env'
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
            $trimmed = $line.Trim()
            if ($trimmed -eq '' -or $trimmed.StartsWith('#')) {
                continue
            }
            $parts = $trimmed.Split('=', 2)
            if ($parts.Count -eq 2 -and $parts[0].Trim() -eq $Name) {
                return $parts[1].Trim()
            }
        }
    }
    return $DefaultValue
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host '未找到 docker 命令。请先安装并启动 Docker Desktop。' -ForegroundColor Red
    Write-Host '安装完成后重新打开此脚本。'
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker Engine 尚未运行。请打开 Docker Desktop，等待状态变为 Engine running。' -ForegroundColor Red
    exit 1
}

docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host '当前 Docker 不包含 Compose V2，请安装最新版 Docker Desktop。' -ForegroundColor Red
    exit 1
}

$appPort = Read-EnvValue -Name 'APP_PORT' -DefaultValue '18080'
$backendPort = Read-EnvValue -Name 'BACKEND_PORT' -DefaultValue '18081'
$appUrl = "http://localhost:$appPort"
$healthUrl = "http://localhost:$backendPort/actuator/health"

Write-Host '正在构建并启动 MySQL、Spring Boot 与 Vue/Nginx 容器...'
Write-Host '首次运行需要下载镜像和 Maven/npm 依赖，请耐心等待。'
docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Host 'docker compose up 执行失败。' -ForegroundColor Red
    docker compose ps
    exit 1
}

Write-Host '容器已创建，正在等待后端健康检查通过...'
$healthy = $false
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
        if ($response.status -eq 'UP') {
            $healthy = $true
            break
        }
    } catch {
        # Docker Desktop may still be starting the backend container.
    }
    if (-not $healthy) {
        Start-Sleep -Seconds 2
    }
}

if (-not $healthy) {
    Write-Host '后端未在预期时间内就绪。请查看容器状态与日志：' -ForegroundColor Red
    docker compose ps
    docker compose logs --tail=120 backend
    exit 1
}

docker compose ps
Write-Host ''
Write-Host "系统已启动：$appUrl" -ForegroundColor Green
Write-Host "Swagger：http://localhost:$backendPort/swagger-ui.html"
Write-Host '演示账号：admin / Demo@1234'
Write-Host '登录页支持自助注册；新账号默认分配只读访客（VIEWER）角色。'
Write-Host '首次启动完成后，以后只需运行 docker-start.bat。'

if (-not $NoBrowser) {
    Start-Process $appUrl
}
