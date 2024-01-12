FROM eclipse-temurin:21 as builder

# Yggdrasil build configuration
ENV YGGDRASIL_VERSION 0.0.0
# Enable building specific branches
ARG YGGDRASIL_BRANCH="main"

# Build environment preparation
ENV LANG C.UTF-8

RUN apt update && apt install -y \
  git \
  && rm -rf /var/cache/apt/archives /var/lib/apt/lists/*

RUN echo ${YGGDRASIL_BRANCH}
RUN git clone https://github.com/Interactions-HSG/yggdrasil.git \
  --branch $YGGDRASIL_BRANCH \
  # only the specified branch
  --depth=1 \
  /opt/app

WORKDIR /opt/app/
RUN ./gradlew

FROM eclipse-temurin:21

# Yggdrasil build configuration
ENV YGGDRASIL_VERSION 0.0.0

# Build environment preparation
ENV LANG C.UTF-8

# Copy the jar
RUN mkdir /opt/app
COPY --from=builder  /opt/app/build/libs/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar /opt/app
# Copy the configuration
RUN mkdir /opt/app/conf
COPY --from=builder /opt/app/conf/docker_disk_config.json /opt/app/conf/config.json

# The default http port
EXPOSE 8080

ENTRYPOINT java -jar /opt/app/yggdrasil-${YGGDRASIL_VERSION}-SNAPSHOT-all.jar
