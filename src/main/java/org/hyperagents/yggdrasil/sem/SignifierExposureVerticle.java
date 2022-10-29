package org.hyperagents.yggdrasil.sem;

import ch.unisg.ics.interactions.hmas.core.hostables.ResourceProfile;
import ch.unisg.ics.interactions.hmas.core.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.core.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.core.vocabularies.HMAS;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.vocabularies.INTERACTION;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.rdf.api.IRI;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.sem.impl.SemFactory;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;

import java.io.IOException;

public class SignifierExposureVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(SignifierExposureVerticle.class.getName());

  private SignifierExposureMechanism sem;
  private WebClient client;

  @Override
  public void start() {
    client = WebClient.create(vertx);
    sem = SemFactory.createSem(config().getString("sem"));
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(SignifierExposureMechanism.BUS_ADDRESS, this::handleEntityRequest);
  }

  private void handleEntityRequest(Message<String> message) {
/*
    try {

     // IRI requestIRI = store.createIRI(requestIRIString);
*/

      String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);

      if (SignifierExposureMechanism.ADJUST_ENTITY.equals(requestMethod)) {
        String requestIRIString = message.headers().get(HttpEntityHandler.REQUEST_URI);
        String agentIRIString = message.headers().get(HttpEntityHandler.AGENT_WEB_ID);
        DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_URI, requestIRIString)
          .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.GET_ENTITY_FOR_AGENT)
          .addHeader(HttpEntityHandler.AGENT_WEB_ID, agentIRIString);

        vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options,
            handleStoreReply(message));
      }
      /*
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
      replyFailed(message);
    }*/

  }

  private void handleAdjustEntity(IRI requestIRI, Message<String> message)
    throws IllegalArgumentException, IOException {


  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(Message message) {

    return reply -> {
      if (reply.succeeded()) {
        LOGGER.info("Response from RDF store");

        String artifactProfileStr = reply.result().body();
        String agentProfileStr = reply.result().headers().get(HttpEntityHandler.AGENT_ENTITY);
        if (artifactProfileStr != null && !artifactProfileStr.isEmpty()) {

          LOGGER.info("Profile of entity found");
          ResourceProfile artifactProfile = ResourceProfileGraphReader.readFromString(artifactProfileStr);
          LOGGER.info(artifactProfile.getResource().getTypeAsString());

          if (agentProfileStr != null && !agentProfileStr.isEmpty()) {
            LOGGER.info("Profile of requesting agent found");
            ResourceProfile agentProfile = ResourceProfileGraphReader.readFromString(agentProfileStr);
            LOGGER.info(agentProfile.getResource().getTypeAsString());

            ResourceProfile adjustedProfile = sem.getComplementaryProfile(artifactProfile, agentProfile);
            String adjustedProfileStr = new ResourceProfileGraphWriter(adjustedProfile)
              .setNamespace("hmas", HMAS.PREFIX.toString())
              .setNamespace("hmas-int", INTERACTION.PREFIX.toString())
              .write();
            replyWithPayload(message, adjustedProfileStr);
          } else {
            replyWithPayload(message, artifactProfileStr);
          }
        }
      }
    };
  }

  private void replyWithPayload(Message<String> message, String payload) {
    message.reply(payload);
  }
}
