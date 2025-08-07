FROM gradle:8-jdk17

WORKDIR /app

COPY . .

RUN gradle build --no-daemon --max-workers=1 -x test

CMD ["gradle", ":exposed-example:run"]