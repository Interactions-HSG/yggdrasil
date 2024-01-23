FROM eclipse-temurin:21

ARG YGGDRASIL_VERSION

# https://stackoverflow.com/a/793867
RUN mkdir -p /opt/app/conf
ADD . /opt/app/
WORKDIR /opt/app/

RUN ls -alF


# COPY --from=builder /opt/app/build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar /opt/app/libs/
# COPY --from=builder /opt/app/conf/docker_disk_config.json /opt/app/conf/config.json

# The default http port
EXPOSE 8080

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar
