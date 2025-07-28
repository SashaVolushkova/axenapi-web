# Use OpenJDK 22 as the base image
FROM nexus-common.ru-central1.internal:5000/base/openjdk:22-slim
MAINTAINER axenix_innovation

WORKDIR /app

# Сейчас генерируется 2 артефакта
# build/libs/axenapi-web-1.0-SNAPSHOT-plain.jar
# build/libs/axenapi-web-1.0-SNAPSHOT.jar
COPY build/libs/axenapi-web-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080/tcp

ENTRYPOINT ["java", "-jar", "app.jar"]
