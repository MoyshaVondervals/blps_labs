# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
RUN chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/*.war /app/app.war

EXPOSE 8080
ENV JAVA_OPTS=""

USER spring:spring
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]
