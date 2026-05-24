# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -q -DskipTests package -pl application-container -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/application-container/target/backend.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
