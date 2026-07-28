# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /data
ENV FITTRACK_DB_PATH=/data/fittrack.db
ENV SERVER_PORT=8080
EXPOSE 8080
COPY --from=build /workspace/target/backend-*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
