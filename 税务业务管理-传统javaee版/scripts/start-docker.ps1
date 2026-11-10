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
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker Engine 尚未运行。请打开 Docker Desktop，等待显示 Engine running。' -ForegroundColor Red
    exit 1
}

docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host '当前 Docker 不包含 Compose V2，请安装最新版 Docker Desktop。' -ForegroundColor Red
    exit 1
}

$appPort = Read-EnvValue -Name 'APP_PORT' -DefaultValue '18082'
$appUrl = "http://localhost:$appPort"
$loginUrl = "$appUrl/auth/login.action"

Write-Host '正在构建传统 Java EE 镜像并启动 Struts + JSP + MySQL...'
Write-Host '首次运行需要下载 Maven、Tomcat 与 MySQL 镜像，请耐心等待。'
docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Host 'docker compose up 执行失败。' -ForegroundColor Red
    docker compose ps
    exit 1
}

Write-Host '容器已创建，正在等待 Struts 登录页响应...'
$healthy = $false
for ($attempt = 0; $attempt -lt 120; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $loginUrl -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200 -and $response.Content -match '税务业务管理平台') {
            $healthy = $true
            break
        }
    } catch {
        # Tomcat, Struts and MySQL may still be initialising.
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    Write-Host '系统未在预期时间内就绪。请查看容器状态与日志：' -ForegroundColor Red
    docker compose ps
    docker compose logs --tail=160 app
    docker compose logs --tail=80 mysql
    exit 1
}

docker compose ps
Write-Host ''
Write-Host "系统已启动：$appUrl" -ForegroundColor Green
Write-Host '演示账号（密码区分大小写）：'
Write-Host '  admin / Admin@123        系统管理员'
Write-Host '  clerk1 / Clerk@123       申报经办员'
Write-Host '  reviewer1 / Reviewer@123 申报审核员'
Write-Host '  analyst1 / Analyst@123   统计分析员'
Write-Host '  viewer1 / Viewer@123     只读访客'
Write-Host ''
Write-Host '技术栈：Struts 2.5.33 + JSP/JSTL + JDBC + MySQL 8 + Tomcat 9（全程 javax.servlet）'
Write-Host '登录页支持自助注册；新账号默认分配只读访客（VIEWER）角色。'

if (-not $NoBrowser) {
    Start-Process $appUrl
}
