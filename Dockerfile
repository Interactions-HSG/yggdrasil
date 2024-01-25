FROM eclipse-temurin:21

ARG YGGDRASIL_VERSION

# Here as precaution but actually not used when naming the jar
ENV YGGDRASIL_VERSION ${YGGDRASIL_VERSION}

# https://stackoverflow.com/a/793867
RUN mkdir -p /opt/
ADD opt/app/ /opt/app/
WORKDIR /opt/app/

ENTRYPOINT java -jar /opt/app/yggdrasil-0.0.0-SNAPSHOT-all.jar
