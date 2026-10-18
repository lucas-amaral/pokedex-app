# syntax=docker/dockerfile:1

# Build stage: compile and package with the Gradle wrapper on a full JDK.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Build files first, so dependencies are downloaded again only when they change.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon --quiet > /dev/null

COPY src src
# Tests run in CI and locally (they need Docker for Testcontainers); the image only packages the application.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon --quiet \
    && java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination build/extracted

# Runtime stage: only a JRE and the application, run as a non-root user.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S pokedex && adduser -S -G pokedex pokedex

# One layer per Spring Boot layer, least to most often changed: a code change rebuilds only the last one.
COPY --from=build /workspace/build/extracted/dependencies/ ./
COPY --from=build /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/build/extracted/application/ ./

USER pokedex
EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
