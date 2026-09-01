# syntax=docker/dockerfile:1.7
# LieShouCloud · User Service (port 8081) multi-stage
# docker build context = 仓库根(含 framework submodule + services/,mvn -f pom.xml 一次聚合构建)

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY . /workspace/

COPY services/.m2-settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -f pom.xml -pl services/user -am clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /workspace/services/user/target/lieshoucloud-user.jar /app/app.jar

# 用户头像目录（2026-09 · compose 挂 dwjk-user-avatars volume，首次挂载继承此所有权）
RUN mkdir -p /data/avatars && chown -R app:app /data/avatars

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
USER app

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -Y off -qO- http://127.0.0.1:8081/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
