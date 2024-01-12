FROM eclipse-temurin:21 as builder

ADD . /opt/app/

WORKDIR /opt/app/
RUN ./gradlew

FROM eclipse-temurin:21

RUN apt update && apt install -y \
  git \
  && rm -rf /var/cache/apt/archives /var/lib/apt/lists/*

ARG YGGDRASIL_VERSION

# https://stackoverflow.com/a/793867
RUN mkdir -p /opt/app/conf
COPY --from=builder /opt/app/build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar /opt/app/libs/
COPY --from=builder /opt/app/conf/docker_disk_config.json /opt/app/conf/config.json

# The default http port
EXPOSE 8080

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar
