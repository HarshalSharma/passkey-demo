FROM eclipse-temurin:21-jre-alpine

LABEL authors="harshalsharma"

WORKDIR /app

# Copy the Spring Boot application JAR
COPY backendserv/target/*.jar app.jar

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

#SAMPLE RUN COMMAND:
# local server :
# docker run -p 8080:8080 -e JAVA_TOOL_OPTIONS='-Dwebauthn.rpId=localhost -Dwebauthn.rpName=localhost -Dwebauthn.origin=http://localhost:8080' harshalworks/passkey-demo-harshalsharma:v2
#docker run -p 8080:8080 -e JAVA_TOOL_OPTIONS='-Dwebauthn.rpId=localhost:8080 -Dwebauthn.rpName=localhost -Dwebauthn.origin=http://localhost:8080' passkey-demo:1
#docker tag local-image:tagname new-repo:tagname
#docker push harshalworks/passkey-demo-harshalsharma:v1
#docker run -p 8080:8080 harshalworks/passkey-demo-harshalsharma:v1
