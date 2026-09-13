# Stage 1: build + tests (Java 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workdir

COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q package

# Stage 2: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workdir/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]