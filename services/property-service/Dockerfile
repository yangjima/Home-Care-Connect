# ============================================================
# property-service Dockerfile
# 多阶段构建: Maven 构建 -> JRE 运行
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制父 POM 并安装到本地 Maven 仓库
COPY pom.xml ./parent-pom.xml
RUN mvn install -B -f parent-pom.xml -N

COPY property-service/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY property-service/src ./src
RUN mvn clean package -DskipTests -B

# ========== 运行时镜像 ==========
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl ffmpeg

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
