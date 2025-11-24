FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew

RUN --mount=type=secret,id=gpr.user \
    --mount=type=secret,id=gpr.key \
    mkdir -p /root/.gradle && \
    echo "gpr.user=$(cat /run/secrets/gpr.user)" >> /root/.gradle/gradle.properties && \
    echo "gpr.key=$(cat /run/secrets/gpr.key)"  >> /root/.gradle/gradle.properties && \
    ./gradlew --no-daemon clean bootJar && \
    rm -f /root/.gradle/gradle.properties

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
COPY newrelic/newrelic.jar /app/newrelic.jar
COPY newrelic/newrelic.yml /app/newrelic.yml

ENTRYPOINT ["java", "-Dspring.profiles.active=production", "-javaagent:/app/newrelic.jar", "-jar", "/app/app.jar"]