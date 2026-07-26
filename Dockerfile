FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.jar api-service.jar

ENTRYPOINT ["java","-jar","api-service.jar"]

EXPOSE 8080