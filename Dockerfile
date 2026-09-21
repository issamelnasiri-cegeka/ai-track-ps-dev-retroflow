# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress dependency:go-offline

# Tests remain in the repository for CI; the image packages production sources only.
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests --batch-mode --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app -s /sbin/nologin app

WORKDIR /app
COPY --from=build --chmod=0444 /build/target/*.jar ./app.jar

ENV SPRING_H2_CONSOLE_ENABLED=false \
    SERVER_SHUTDOWN=graceful

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --noproxy '*' --fail --silent --show-error --max-time 3 \
        http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
