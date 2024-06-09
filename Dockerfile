# Use the official Eclipse Temurin image as the base image
FROM tomcat:9-jdk17-temurin

# Set the working directory in the container
WORKDIR /usr/local/tomcat
# Copy the WebArchive file into the container
COPY target/OntologyBasedModellingEnvironment-WebService-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
# Expose the port your application runs on
EXPOSE 8080
# Command to run the application
CMD ["catalina.sh", "run"]
