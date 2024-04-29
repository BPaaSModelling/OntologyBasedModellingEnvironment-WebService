#!/bin/bash

# Navigate to the directory containing the jar and war files if necessary
# cd /path/to/files

java -jar -Ddebug=true ./target/dependency/webapp-runner.jar ./target/OntologyBasedModellingEnvironment-WebService-0.0.1-SNAPSHOT.war -AmaxHttpHeaderSize="65536"
chmod +x runJava.sh