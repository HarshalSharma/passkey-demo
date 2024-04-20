FROM eclipse-temurin:21-jre-alpine

LABEL authors="harshalsharma"

WORKDIR /app

# Copy the Spring Boot application JAR
COPY backendserv/target/*.jar app.jar

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

#SAMPLE RUN COMMAND:
#docker run -p 8080:8080 -e JAVA_TOOL_OPTIONS='-Dwebauthn.rpId=localhost:8080 -Dwebauthn.rpName=localhost -Dwebauthn.origin=http://localhost:8080' passkey-demo:1
