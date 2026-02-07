FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY app/file-uploader/pom.xml ./pom.xml
COPY app/file-uploader/.mvn ./.mvn
COPY app/file-uploader/mvnw ./mvnw

RUN chmod +x ./mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY app/file-uploader/src ./src

RUN ./mvnw -q -DskipTests clean package


FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /workspace/target/fileuploader-1.0.0.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
