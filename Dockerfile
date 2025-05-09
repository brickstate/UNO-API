FROM eclipse-temurin:17
COPY target/app.jar /app.jar
EXPOSE 7000  
ENTRYPOINT ["java", "-jar", "/app.jar"]