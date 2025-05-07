FROM eclipse-temurin:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
COPY target/app.jar /app.jar
EXPOSE 7000
EXPOSE 3306
ENTRYPOINT ["java","-jar","/app.jar"]
