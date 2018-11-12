package ro.andreiciortea.yggdrasil.store.td;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.mas.Artifact;
import ro.andreiciortea.yggdrasil.mas.ArtifactDeserializer;
import ro.andreiciortea.yggdrasil.store.RdfStoreVerticle;

import java.util.HashMap;


public class TdStoreVerticle extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());
  private final static HashMap<String, Artifact> store = new HashMap();


  @Override
  public void start() {
    // get some store

    EventBus eventBus = vertx.eventBus();

    eventBus.consumer(EventBusRegistry.TD_STORE_ENTITY_BUS_ADDRESS, this::handleEntityRequest);
  }

  private void handleEntityRequest(Message<String> message) {
    try {
      EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

      String requestIRIString = request.getHeader(EventBusMessage.Headers.REQUEST_IRI).get();
      // TODO: add IRI generator
      // IRI requestIRI = store.createIRI(requestIRIString);


      switch (request.getMessageType()) {
        case CREATE_ARTIFACT_ENTITY:
          handleCreateEntity(requestIRIString, request, message);
      }
    }
    catch (IllegalArgumentException e) {
      LOGGER.error(e.getMessage());
      replyFailed(message);
    }
  }

  /**
   * @param requestIRI
   * @param request
   * @param message
   */
  private void handleCreateEntity(String requestIRI, EventBusMessage request, Message<String> message) {
    System.out.println("create artifact from json");

    if (request.getPayload().isPresent()) {
      Gson gson =
        new GsonBuilder()
          .registerTypeAdapter(Artifact.class, new ArtifactDeserializer())
          .create();
      Artifact artifact = gson.fromJson(request.getPayload().get(), Artifact.class);
      store.put(requestIRI, artifact);
    } else {
      replyFailed(message);
    }
  }

  private void replyFailed(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.FAILED.name());

    message.reply(response.toJson());
  }
}
