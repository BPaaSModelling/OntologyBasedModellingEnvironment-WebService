#!/bin/bash

#If this script doesn't work, try to give it the right permissions like follows:
#cd /path/to/your/script
#chmod +x start_webserver.sh
#./start_webserver.sh

java -jar -debug ./target/dependency/webapp-runner.jar ./target/OntologyBasedModellingEnvironment-WebService-0.0.1-SNAPSHOT.war -AmaxHttpHeaderSize=65536