FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM amazoncorretto:26-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS=""

COPY --from=build --chown=app:app /app/target/WhatToEat-0.0.1-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
