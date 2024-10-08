FROM maven:3.8.1-adoptopenjdk AS build
# Copy the source code into the container
COPY ./src /app/src
COPY ./WebContent /app/WebContent
COPY ./pom.xml /app
# Set the working directory in the container
WORKDIR /app
# Build the application
RUN mvn clean package

# Use the official Eclipse Temurin image as the base image
FROM eclipse-temurin:11-jdk

# Set the working directory in the container
WORKDIR /app
# Copy the WebArchive file into the container
COPY --from=build /app /app

# Expose the port your application runs on
EXPOSE 8080
# Command to run the application
CMD ["java", "-jar", "target/dependency/webapp-runner.jar", "--port", "8080", "-AmaxHttpHeaderSize=65536", "target/OntologyBasedModellingEnvironment-WebService-0.0.1-SNAPSHOT.war"]