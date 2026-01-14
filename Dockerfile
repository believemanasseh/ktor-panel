FROM gradle:8-jdk17 AS builder

WORKDIR /app

COPY examples/exposed/build.gradle.kts /app/exposed/

WORKDIR /app/exposed
RUN gradle dependencies --no-daemon

COPY examples/exposed/src /app/exposed/src/
RUN gradle build --no-daemon --max-workers=1 -x test --stacktrace

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/exposed/build/libs/exposed-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]