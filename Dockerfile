# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency files first for better caching
COPY pom.xml .

# Download dependencies with cache mount for faster rebuilds
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

COPY src ./src

# Download ONNX model from GitHub
RUN mkdir -p src/main/resources/models
ADD --chown=root:root https://github.com/SentimentONE/sentimentIA/raw/refs/heads/main/03-models/sentiment_model.onnx \
    src/main/resources/models/sentiment_model.onnx

# Build application with Maven cache
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy AS final

# Install ONNX Runtime dependencies and locales
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

WORKDIR /app

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=80.0", "-jar", "app.jar"]
