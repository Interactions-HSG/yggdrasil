FROM openjdk:11

EXPOSE 8080

COPY gradlew /yggdrasil/
COPY gradle/  /yggdrasil/gradle
RUN  cd yggdrasil && /yggdrasil/gradlew wrapper --gradle-version 6.5
COPY log4j.properties /yggdrasil/
COPY logging.properties /yggdrasil/
COPY build.gradle /yggdrasil/
COPY settings.gradle /yggdrasil/
COPY cartago/  /yggdrasil/cartago
COPY moise/  /yggdrasil/moise
COPY wot-td-java/  /yggdrasil/wot-td-java
COPY src/main/conf/config4.json /usr/verticles/
COPY src/  /yggdrasil/src

RUN cd /yggdrasil && ./gradlew build -x test
RUN cp /yggdrasil/build/libs/yggdrasil-0.0-SNAPSHOT-fat.jar  /usr/verticles/

ADD initialize.sh /yggdrasil
ADD initialize-demo-lab.sh /yggdrasil
ADD initialize-demo-lab.sh /yggdrasil
ADD create-agent-full-system2-demo-lab.sh /yggdrasil
ADD create-agent-full-system2.sh /yggdrasil

# Launch the verticle
WORKDIR /usr/verticles


ENTRYPOINT ["sh", "-c"]
#CMD ["exec java -jar yggdrasil-0.0-SNAPSHOT-fat.jar -conf config.json"]
CMD ["exec java -jar yggdrasil-0.0-SNAPSHOT-fat.jar -conf /usr/verticles/config4.json"]
