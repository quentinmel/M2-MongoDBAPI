FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/target/medislot.jar /app/app.jar
ADD --chmod=0444 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.33.1/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
