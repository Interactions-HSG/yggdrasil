FROM eclipse-temurin:21

ARG YGGDRASIL_VERSION

# https://stackoverflow.com/a/793867
RUN mkdir -p /opt/
ADD opt/app/ /opt/app/
WORKDIR /opt/app/

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar
