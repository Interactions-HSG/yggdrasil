FROM openjdk:11

EXPOSE 8080

COPY .  /yggdrasil
COPY src/main/conf/config3.json /usr/verticles/

RUN cd /yggdrasil && ./gradlew build -x test

RUN cp /yggdrasil/build/libs/yggdrasil-0.0-SNAPSHOT-fat.jar  /usr/verticles/

# Launch the verticle
WORKDIR /usr/verticles


ENTRYPOINT ["sh", "-c"]
#CMD ["exec java -jar yggdrasil-0.0-SNAPSHOT-fat.jar -conf config.json"]
CMD ["exec java -jar yggdrasil-0.0-SNAPSHOT-fat.jar -conf /usr/verticles/config3.json"]
