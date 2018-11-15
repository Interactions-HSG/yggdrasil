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
import ro.andreiciortea.yggdrasil.environment.Action;
import ro.andreiciortea.yggdrasil.environment.Artifact;
import ro.andreiciortea.yggdrasil.environment.ArtifactDeserializer;
import ro.andreiciortea.yggdrasil.store.RdfStoreVerticle;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;


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
          break;
        case ACTIONS_ENTITY:
          handleArtifactActions(requestIRIString, message);
          break;
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
    String id;

    Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
    if (slug.isPresent()) {
      id = requestIRI.concat("/").concat(slug.get());
    } else {
      id = UUID.randomUUID().toString();
    }

    if (request.getPayload().isPresent()) {
      Gson gson =
        new GsonBuilder()
          .registerTypeAdapter(Artifact.class, new ArtifactDeserializer())
          .create();
      Artifact artifact = gson.fromJson(request.getPayload().get(), Artifact.class);
      store.put(id, artifact);

      Gson responseBuidler = new Gson();
      EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY_JSON)
        .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.SUCCEEDED.name())
        .setPayload(responseBuidler.toJson(artifact));

      message.reply(response.toJson());
    } else {
      replyFailed(message);
    }
  }

  private void handleArtifactActions(String artifactIRI, Message<String> message) {
    System.out.println("Get artifact actions");
    Artifact target = store.get(artifactIRI);
    if (target == null) {
      replyFailed(message);
    }
    Action[] actions = target.getActions();
    Gson gson = new Gson();
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY_JSON)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.SUCCEEDED.name())
      .setPayload(gson.toJson(actions));

    message.reply(response.toJson());
  }

  private void replyFailed(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.FAILED.name());

    message.reply(response.toJson());
  }
}
