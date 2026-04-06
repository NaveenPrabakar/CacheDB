# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy project files and build
COPY pom.xml .
COPY src ./src
RUN mvn -B dependency:copy-dependencies package -DincludeScope=runtime

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/cachedb-*.jar /app/app.jar
COPY --from=build /app/target/dependency /app/lib

ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "cachedb.Main"]
