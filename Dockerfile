# syntax=docker/dockerfile:1

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency files first for better caching
COPY pom.xml .

# Download dependencies with cache mount for faster rebuilds
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Create a temporary directory for the model (OUTSIDE src/main/resources)
RUN mkdir -p /build-models

# Download ONNX model from GitHub
ADD --chown=root:root https://github.com/SentimentONE/sentimentIA/raw/refs/heads/main/03-models/sentiment_model.onnx \
    /build-models/sentiment_model.onnx

COPY src ./src

# Build application with Maven cache
# The JAR will be lightweight because it DOES NOT contain the .onnx file
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests


# --- Stage 2: Final Image ---
FROM eclipse-temurin:21-jre-jammy AS final

# Install ONNX Runtime dependencies (libgomp1 is required for multi-threading)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    locales \
    libgomp1 \
    && locale-gen en_US.UTF-8 && \
    update-locale LANG=en_US.UTF-8 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Configure locale environment
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Create non-privileged user following Docker best practices
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

WORKDIR /app

# 1. Copy the application JAR
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# 2. Copy the ONNX model to the specific path expected by Java
# This places the file at /app/models/sentiment_model.onnx
RUN mkdir models && chown appuser:appuser models
COPY --from=build --chown=appuser:appuser /build-models/sentiment_model.onnx /app/models/sentiment_model.onnx

# Set explicit environment variable SENTIMENT_MODEL_PATH
ENV SENTIMENT_MODEL_PATH=/app/models/sentiment_model.onnx

USER appuser

EXPOSE 8080

# Java Options for Low RAM Environments:
# -XX:+UseSerialGC: Use the simplest Garbage Collector to save memory overhead.
# -XX:MaxRAMPercentage=45.0: Restrict Java Heap to 45% of total RAM.
#   This leaves ~55% of RAM free for the ONNX C++ Native Runtime (which lives outside the Heap).
# -Xss256k: Reduce thread stack size.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=45.0", "-Xss256k", "-jar", "app.jar"]