FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV TZ=Asia/Seoul

RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring build/docker/app.jar /app/app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]