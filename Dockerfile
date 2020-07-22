FROM openjdk:latest

ENV VERTICLE_FILE yggdrasil-0.0-SNAPSHOT-fat.jar

ENV CONFIG_FILE config.json

ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

COPY build/libs/$VERTICLE_FILE $VERTICLE_HOME/

COPY src/main/conf/$CONFIG_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE -conf $CONFIG_FILE"]