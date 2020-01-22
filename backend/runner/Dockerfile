# We just need java 11
FROM openjdk:11-slim

# Add the jar
COPY target/runner.jar /home/velcom/runner.jar

# Config dir for runner configuration file
VOLUME ["/home/velcom/config"]

# Starts the runner, needs the path to config as the sole argument
ENTRYPOINT ["java", "-jar", "/home/velcom/runner.jar"]
