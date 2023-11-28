FROM eclipse-temurin:21

# Yggdrasil build configuration
ENV YGGDRASIL_VERSION 0.0.0

# Build environment preparation
ENV LANG C.UTF-8

# Copy the jar
RUN mkdir /opt/app
COPY build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar /opt/app
COPY conf/docker_disk_config.json /opt/app

# The default http port
EXPOSE 8080
# The port for interacting with CArtAgO
EXPOSE 8088

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar -conf /opt/app/config.json
