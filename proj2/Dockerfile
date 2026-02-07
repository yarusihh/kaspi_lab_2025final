# TODO: Provide production-ready Dockerfile.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY app/file-uploader-reactive/target/file-uploader-reactive-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
