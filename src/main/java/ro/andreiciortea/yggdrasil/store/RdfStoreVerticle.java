package ro.andreiciortea.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;

public class RdfStoreVerticle extends AbstractVerticle {

  public static final String RDF_STORE_ENTITY_BUS_ADDRESS = "ro.andreiciortea.yggdrasil.eventbus.rdfstore.entity_request";
  public static final String RDF_STORE_QUERY_BUS_ADDRESS = "ro.andreiciortea.yggdrasil.eventbus.rdfstore.query";
  
  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());
  
  private RdfStore store;
  
  @Override
  public void start() {
    store = RdfStoreFactory.createStore(config().getString("store"));
    
    EventBus eventBus = vertx.eventBus();
    
    eventBus.consumer(RDF_STORE_ENTITY_BUS_ADDRESS, this::handleEntityRequest);
    eventBus.consumer(RDF_STORE_QUERY_BUS_ADDRESS, this::handleQueryRequest);
  }
  
  private void handleEntityRequest(Message<String> message) {
    try {
      EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);
      
      String requestIRIString = request.getHeader(EventBusMessage.Headers.ENTITY_IRI).get();
      IRI requestIRI = store.createIRI(requestIRIString);
      
      if (request.getMessageType() == EventBusMessage.MessageType.GET_ENTITY) {
        Optional<Graph> result = store.getEntityGraph(requestIRI);
        
        if (result.isPresent() && result.get().size() > 0) {
          replyWithPayload(message, store.graphToString(result.get(), RDFSyntax.TURTLE));
        } else {
          replyEntityNotFound(message);
        }
      } else if (request.getMessageType() == EventBusMessage.MessageType.CREATE_ENTITY) {
        Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
        String entityIRIString = generateEntityIRI(requestIRIString, slug);
        IRI entityIRI = store.createIRI(entityIRIString);
        
        Graph entityGraph = store.stringToGraph(request.getPayload(), entityIRI, RDFSyntax.TURTLE);
        store.createEntityGraph(entityIRI, entityGraph);
        
        replyWithPayload(message, store.graphToString(entityGraph, RDFSyntax.TURTLE));
      } else if (request.getMessageType() == EventBusMessage.MessageType.PATCH_ENTITY) {
        // TODO
      } else if (request.getMessageType() == EventBusMessage.MessageType.UPDATE_ENTITY) {
        // TODO
      } else if (request.getMessageType() == EventBusMessage.MessageType.DELETE_ENTITY) {
        // TODO
      }
    }
    catch (IOException e) {
      LOGGER.error(e.getMessage());
      replyFailed(message);
    }
    catch (IllegalArgumentException e) {
      LOGGER.error(e.getMessage());
      replyFailed(message);
    }
  }
  
  private void replyWithPayload(Message<String> message, String payload) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
        .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.SUCCEEDED.name())
        .setPayload(payload);
    
    message.reply(response.toJson());
  }
  
  private void replyFailed(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
        .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.FAILED.name());
    
    message.reply(response.toJson());
  }
  
  private void replyEntityNotFound(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
        .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.ENTITY_NOT_FOUND.name());
    
    message.reply(response.toJson());
  }
  
  private String generateEntityIRI(String requestIRI, Optional<String> hint) {
    if (!requestIRI.endsWith("/")) {
      requestIRI = requestIRI.concat("/");
    }
    
    String candidateIRI;
    
    // Try to generate an IRI using the hint provided in the initial request
    if (hint.isPresent() && !hint.get().isEmpty()) {
      candidateIRI = requestIRI.concat(hint.get());
      if (!store.containsEntityGraph(store.createIRI(candidateIRI))) {
        return candidateIRI;
      }
    }
    
    // Generate a new IRI
    do {
      candidateIRI = requestIRI.concat(UUID.randomUUID().toString());
    } while (store.containsEntityGraph(store.createIRI(candidateIRI)));
    
    return candidateIRI;
  }
  
  private void handleQueryRequest(Message<String> message) {
    // TODO
  }
  
}
