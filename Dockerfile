FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

# Download ONNX model from GitHub
RUN mkdir -p src/main/resources/models
RUN curl -L -f -o src/main/resources/models/sentiment_model.onnx \
    https://github.com/SentimentONE/sentimentIA/raw/refs/heads/main/03-models/sentiment_model.onnx

RUN mvn clean package -DskipTests


FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY --from=build --chown=nonroot:nonroot /app/target/*.jar app.jar

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=80.0", "-jar", "app.jar"]