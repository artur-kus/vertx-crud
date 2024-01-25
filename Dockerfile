FROM openjdk:17-slim

EXPOSE 8080
ENV APP_NAME=vertx-crud-backend.jar
ADD target/vertx-crud-1.0.0-SNAPSHOT-fat.jar /$APP_NAME

CMD ["sh", "-c", "java -jar /$APP_NAME"]
