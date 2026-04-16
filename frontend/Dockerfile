# ============================================================
# frontend Dockerfile
# 多阶段构建: Node 构建 -> nginx 服务
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# 复制 package 文件
COPY package.json package-lock.json* ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建（VITE_API_BASE_URL 在 docker-compose 中通过 ARG 传入）
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_URL=ws://gateway:8080
ARG VITE_ENV=production

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_ENV=$VITE_ENV

RUN npm run build

# ========== nginx 运行时镜像 ==========
FROM nginx:alpine

# 删除默认 nginx 配置
RUN rm -rf /etc/nginx/conf.d/default.conf

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 favicon
COPY --from=builder /app/public/favicon.svg /usr/share/nginx/html/favicon.svg

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
