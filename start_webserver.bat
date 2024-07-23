@echo off

REM Run the packaged WAR file with webapp-runner
java -jar -debug .\target\dependency\webapp-runner.jar .\target\OntologyBasedModellingEnvironment-WebService-0.0.1-SNAPSHOT.war -AmaxHttpHeaderSize="65536"

if %ERRORLEVEL% neq 0 (
    echo './target' directory is missing or is incompatible.
    echo Please make sure './aoame/OntologyBasedModellingEnvironment-WebService' contains './target' directory.
    echo To include the missing directory: Unzip target.zip from the current directory and move it to './aoame/OntologyBasedModellingEnvironment-WebService' root directory.
    echo Or compile the project with Intellij/Eclipse in 'aoame/OntologyBasedModellingEnvironment-WebService' using 'mvn package' command.
)


REM Keep the command prompt open after running the application
cmd.exe