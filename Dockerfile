FROM eclipse-temurin:21

# Yggdrasil build configuration
ENV YGGDRASIL_VERSION 0.0.0

# Build environment preparation
ENV LANG C.UTF-8

# Copy the jar
RUN mkdir /opt/app
COPY build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar /opt/app
# Copy the configuration
RUN mkdir /opt/app/conf
COPY conf/docker_disk_config.json /opt/app/conf/config.json

# The default http port
EXPOSE 8080

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar
