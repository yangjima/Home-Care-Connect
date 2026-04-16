#!/bin/bash
# ============================================================
# 居服通 Docker 启动脚本 (Bash)
# 使用方法:
#   启动所有:  ./start.sh
#   仅基础设施: SCOPE=infrastructure ./start.sh
#   仅服务:    SCOPE=services ./start.sh
#   停止:      ./start.sh down
#   重启:      ./start.sh restart
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[步骤]${NC} $1"; }
ok()    { echo -e "${GREEN}[成功]${NC} $1"; }
warn()  { echo -e "${YELLOW}[警告]${NC} $1"; }
err()   { echo -e "${RED}[错误]${NC} $1"; exit 1; }

ACTION="${1:-up}"
SCOPE="${SCOPE:-all}"

# 前置检查
info "检查 Docker..."
docker version > /dev/null 2>&1 || err "Docker 未运行或未安装，请先安装 Docker Desktop"

# .env 文件
if [ ! -f .env ]; then
    info "未找到 .env 文件，从示例创建..."
    cp .env.example .env
    ok "已创建 .env 文件"
    warn "请编辑 .env 填入实际配置值（特别是 DASHSCOPE_API_KEY）"
fi

case "$ACTION" in
    up)
        info "启动服务..."
        case "$SCOPE" in
            infrastructure)
                info "启动范围: 基础设施 (MySQL, Redis, MinIO, Nacos)"
                docker compose -f docker-compose.yml up -d mysql redis minio nacos
                ;;
            services)
                info "启动范围: 微服务 + 前端"
                docker compose -f docker-compose.yml up -d --build gateway user-service property-service service-order-service asset-service ai-service frontend
                ;;
            all|*)
                info "启动全部服务（基础设施 + 微服务 + 前端）"
                docker compose -f docker-compose.yml up -d --build
                ;;
        esac

        ok "服务已启动"
        echo ""
        info "等待基础设施就绪..."
        for svc in mysql redis nacos; do
            echo -n "  等待 $svc..."
            while [ "$(docker inspect --format='{{.State.Health.Status}}' "homecare-$svc" 2>/dev/null)" != "healthy" ]; do
                sleep 3
                echo -n "."
            done
            ok "$svc 已就绪"
        done

        echo ""
        ok "居服通平台已启动!"
        echo ""
        echo "访问地址:"
        echo "  前端:      http://localhost"
        echo "  API 网关:  http://localhost:8080"
        echo "  Nacos:     http://localhost:8848/nacos  (账号: nacos / nacos)"
        echo "  MinIO:     http://localhost:9000      (账号: homecareadmin)"
        echo "  AI 服务:   http://localhost:8000/docs"
        echo ""
        echo "测试账号: admin / 123456, user1 / 123456"
        ;;
    down)
        info "停止所有服务..."
        docker compose -f docker-compose.yml down
        ok "所有服务已停止"
        ;;
    restart)
        info "重启所有服务..."
        docker compose -f docker-compose.yml restart
        ok "所有服务已重启"
        ;;
    logs)
        shift
        docker compose -f docker-compose.yml logs -f "$@"
        ;;
    ps)
        docker compose -f docker-compose.yml ps
        ;;
    build)
        info "构建所有镜像..."
        docker compose -f docker-compose.yml build --no-cache
        ok "镜像构建完成"
        ;;
    *)
        echo "用法: $0 [up|down|restart|logs|ps|build] [SCOPE=infrastructure|services|all]"
        ;;
esac
