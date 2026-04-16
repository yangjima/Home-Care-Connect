# ============================================================
# 居服通 Makefile - 简化部署操作
# ============================================================

# 默认目标
.PHONY: help
help:
	@echo "居服通 - 可用命令:"
	@echo ""
	@echo "  make up          启动所有服务（构建 + 启动）"
	@echo "  make down        停止所有服务"
	@echo "  make restart     重启所有服务"
	@echo "  make ps          查看服务状态"
	@echo "  make logs        查看所有日志"
	@echo "  make logs-svc    查看服务日志 (SVC=gateway|user-service|...)"
	@echo "  make build       仅构建镜像"
	@echo "  make clean       清理（停止 + 删除数据卷）"
	@echo ""
	@echo "环境变量说明:"
	@echo "  SCOPE=infrastructure  仅启动基础设施"
	@echo "  SCOPE=services        仅启动微服务"
	@echo ""

# 启动所有服务
.PHONY: up
up:
	docker compose -f infrastructure/docker/docker-compose.yml up -d --build

# 启动指定范围
.PHONY: infra services
infra:
	docker compose -f infrastructure/docker/docker-compose.yml up -d mysql redis minio nacos
services:
	docker compose -f infrastructure/docker/docker-compose.yml up -d --build gateway user-service property-service service-order-service asset-service ai-service frontend

# 停止所有服务
.PHONY: down
down:
	docker compose -f infrastructure/docker/docker-compose.yml down

# 重启
.PHONY: restart
restart:
	docker compose -f infrastructure/docker/docker-compose.yml restart

# 查看状态
.PHONY: ps
ps:
	docker compose -f infrastructure/docker/docker-compose.yml ps

# 查看日志
.PHONY: logs
logs:
	docker compose -f infrastructure/docker/docker-compose.yml logs -f

# 查看指定服务日志
logs-svc:
	@if [ -z "$(SVC)" ]; then echo "用法: make logs-svc SVC=gateway"; exit 1; fi
	docker compose -f infrastructure/docker/docker-compose.yml logs -f $(SVC)

# 构建镜像
.PHONY: build
build:
	docker compose -f infrastructure/docker/docker-compose.yml build

# 清理
.PHONY: clean
clean:
	docker compose -f infrastructure/docker/docker-compose.yml down -v
	@echo "已清理所有服务和数据卷"

# 构建前端（本地开发）
.PHONY: frontend-build
frontend-build:
	cd frontend && npm install && npm run build
