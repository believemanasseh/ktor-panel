# Dockerfile for running the examples/exposed demo app
FROM gradle:8-jdk17

WORKDIR /app

COPY examples/exposed/ ./exposed/

WORKDIR /app/exposed

RUN gradle build --no-daemon --max-workers=1 -x test --stacktrace

CMD ["gradle", "run"]