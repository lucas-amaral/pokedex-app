# syntax=docker/dockerfile:1

# Build stage: package with the Gradle wrapper on a full JDK. Tests are not run here: they need Docker themselves.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon --quiet > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon --quiet \
    && java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination build/extracted

# Runtime stage: JRE only, Spring Boot layers from least to most often changed, non-root user.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S tasks && adduser -S -G tasks tasks
COPY --from=build /workspace/build/extracted/dependencies/ ./
COPY --from=build /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/build/extracted/application/ ./
USER tasks
EXPOSE 8090
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
