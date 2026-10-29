# syntax=docker/dockerfile:1

# ---------- Stage 1: build the executable JAR ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests \
    && cp target/calcit-*.jar target/app.jar

# ---------- Stage 2: slim runtime image ----------
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="calcit" \
      org.opencontainers.image.description="CalcIt Calculator REST API" \
      org.opencontainers.image.source="https://github.com/MJTheGreat3/CalcIt"

# Run as an unprivileged user
RUN addgroup -S calcit && adduser -S calcit -G calcit
WORKDIR /app

COPY --from=build --chown=calcit:calcit /workspace/target/app.jar app.jar

USER calcit
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# busybox wget ships with alpine, so no extra packages are needed for the probe
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
