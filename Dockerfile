# Build stage
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jdk-jammy
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-app.jar
ENTRYPOINT ["java", "-jar", "/app/spring-boot-app.jar"]
