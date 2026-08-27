# NexaMart backend — multi-stage build
# Build:  docker build -t nexamart-backend .
# Run:    docker run -p 8081:8081 --env-file .env -e SPRING_PROFILES_ACTIVE=docker nexamart-backend
# (Not required for the Elastic Beanstalk path in docs/DEPLOYMENT.md — this is here for anyone who
#  prefers a container-based deploy, e.g. AWS App Runner from an ECR image, or local `docker run`.)

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S nexamart && adduser -S nexamart -G nexamart
COPY --from=build /app/target/*.jar app.jar
RUN chown nexamart:nexamart app.jar
USER nexamart
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
