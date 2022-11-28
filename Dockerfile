FROM openjdk:11

# Yggdrasil build configuration
ENV YGGDRASIL_VERSION 0.0

# Build environment preparation
ENV LANG C.UTF-8

# Build the jar
WORKDIR /app
COPY . /app/
RUN ./gradlew

# The default http port
EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "/usr/local/openjdk-11/bin/java -jar ./build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-fat.jar -conf ./src/main/conf/config.json"]
#CMD ["-conf", "./src/main/conf/config.json"]
