# ============================================================
# 居服通 Docker 启动脚本 (PowerShell)
# 使用方法:
#   启动所有服务:   .\start.ps1
#   仅启动基础设施: .\start.ps1 -Scope Infrastructure
#   停止所有服务:   .\start.ps1 -Action Down
#   查看日志:       docker compose -f .\docker-compose.yml logs -f
# ============================================================

param(
    [ValidateSet("All", "Infrastructure", "Services")]
    [string]$Scope = "All",

    [ValidateSet("Up", "Down", "Restart")]
    [string]$Action = "Up",

    [switch]$SkipBuild,

    [switch]$SkipNacosInit
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir ".env"

# 颜色输出函数
function Write-Step { param([string]$Msg) Write-Host "[步骤] $Msg" -ForegroundColor Cyan }
function Write-Success { param([string]$Msg) Write-Host "[成功] $Msg" -ForegroundColor Green }
function Write-Warn { param([string]$Msg) Write-Host "[警告] $Msg" -ForegroundColor Yellow }
function Write-Err { param([string]$Msg) Write-Host "[错误] $Msg" -ForegroundColor Red }

Set-Location $ScriptDir

# ===== 前置检查 =====
Write-Step "检查 Docker 是否运行..."
try {
    $null = docker version 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Docker 未运行或未安装" }
    Write-Success "Docker 已安装并运行"
} catch {
    Write-Err "Docker 检查失败: $_"
    Write-Host "请确保已安装 Docker Desktop 并已启动"
    exit 1
}

# ===== 环境变量文件 =====
if (-not (Test-Path $EnvFile)) {
    Write-Step "未找到 .env 文件，从示例创建..."
    Copy-Item "$EnvFile.example" $EnvFile -Force
    Write-Success "已创建 .env 文件，请编辑 $EnvFile 填入实际配置值"
    Write-Host "  - 特别是 DASHSCOPE_API_KEY (AI 服务必需)"
    Write-Host "  - JWT_SECRET 和数据库密码请根据需要修改"
}

# ===== 确定要启动的服务 =====
$services = @()
switch ($Scope) {
    "Infrastructure" { $services = @("mysql", "redis", "minio", "nacos") }
    "Services"      { $services = @("gateway", "user-service", "property-service", "service-order-service", "asset-service", "ai-service", "frontend") }
    "All"           { $services = @() }  # 空数组表示启动所有服务
}

# ===== 执行操作 =====
switch ($Action) {
    "Up" {
        Write-Step "启动服务..."
        if ($Scope -eq "All") {
            Write-Host "启动全部服务（基础设施 + 微服务 + 前端）"
        } else {
            Write-Host "启动范围: $($services -join ', ')"
        }

        $composeArgs = @("-f", ".\docker-compose.yml", "up", "-d")
        if ($Scope -ne "All" -and $services.Count -gt 0) {
            $composeArgs += @("--profile", $Scope.ToLower())
        }
        if (-not $SkipBuild) {
            $composeArgs += "--build"
        }
        if ($services.Count -gt 0) {
            $composeArgs += $services
        }

        docker compose $composeArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Docker Compose 启动失败"
            exit 1
        }

        Write-Success "所有服务已启动"

        # 等待基础设施就绪
        Write-Step "等待基础设施服务就绪..."
        $maxWait = 120
        $waited = 0

        $infraHealth = @{
            "mysql" = $false
            "redis" = $false
            "nacos" = $false
        }

        while ($waited -lt $maxWait) {
            Start-Sleep -Seconds 5
            $waited += 5

            foreach ($svc in $infraHealth.Keys) {
                if (-not $infraHealth[$svc]) {
                    $status = docker inspect --format='{{.State.Health.Status}}' "homecare-$svc" 2>$null
                    if ($status -eq "healthy") {
                        $infraHealth[$svc] = $true
                        Write-Success "$svc 已就绪"
                    }
                }
            }

            if ($infraHealth.Values -notcontains $false) {
                break
            }
            Write-Host "等待中... ($waited/$maxWait)s"
        }

        # 等待微服务就绪
        Write-Step "等待微服务注册到 Nacos..."
        Start-Sleep -Seconds 15

        # 显示服务状态
        Write-Step "服务状态:"
        docker compose -f .\docker-compose.yml ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

        Write-Host ""
        Write-Success "居服通平台已启动!"
        Write-Host ""
        Write-Host "访问地址:"
        Write-Host "  前端:      http://localhost"
        Write-Host "  API 网关:  http://localhost:8080"
        Write-Host "  Nacos:     http://localhost:8848/nacos  (账号: nacos / nacos)"
        Write-Host "  MinIO:     http://localhost:9000      (账号: homecareadmin)"
        Write-Host "  AI 服务:   http://localhost:8000/docs"
        Write-Host ""
        Write-Host "测试账号:"
        Write-Host "  管理员:    admin / 123456"
        Write-Host "  用户:      user1 / 123456"
    }

    "Down" {
        Write-Step "停止所有服务..."
        docker compose -f .\docker-compose.yml down
        Write-Success "所有服务已停止"
    }

    "Restart" {
        Write-Step "重启所有服务..."
        docker compose -f .\docker-compose.yml restart
        Write-Success "所有服务已重启"
    }
}
