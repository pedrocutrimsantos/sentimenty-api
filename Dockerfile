FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/sentiment-service-0.1.0.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh","-c","java $JAVA_OPTS -jar app.jar"]
