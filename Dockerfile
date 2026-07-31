# syntax=docker/dockerfile:1

# --- Stage 1: Angular SPA ---
FROM node:24-bookworm AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
# Output: /frontend/dist/frontend/browser

# --- Stage 2: Spring Boot (embed SPA into classpath:/static/) ---
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /workspace
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
COPY backend/src src
COPY --from=frontend /frontend/dist/frontend/browser/ src/main/resources/static/
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# --- Stage 3: runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /data
ENV DB_PATH=/data/fittrack.db
ENV FRONTEND_URL=http://localhost:8080
ENV SERVER_PORT=8080
EXPOSE 8080
COPY --from=backend /workspace/target/backend-*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
