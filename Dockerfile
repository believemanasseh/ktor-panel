FROM gradle:8-jdk17

WORKDIR /app

COPY . .

RUN gradle build -x test

CMD ["gradle", ":exposed-example:run"]