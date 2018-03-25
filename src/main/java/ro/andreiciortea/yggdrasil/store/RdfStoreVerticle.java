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
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;

public class RdfStoreVerticle extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());
  
  private RdfStore store;
  
  @Override
  public void start() {
    store = RdfStoreFactory.createStore(config().getString("store"));
    
    EventBus eventBus = vertx.eventBus();
    
    eventBus.consumer(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, this::handleEntityRequest);
    eventBus.consumer(EventBusRegistry.RDF_STORE_QUERY_BUS_ADDRESS, this::handleQueryRequest);
  }
  
  private void handleEntityRequest(Message<String> message) {
    try {
      EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);
      
      String requestIRIString = request.getHeader(EventBusMessage.Headers.REQUEST_IRI).get();
      IRI requestIRI = store.createIRI(requestIRIString);
      
      if (request.getMessageType() == EventBusMessage.MessageType.GET_ENTITY) {
        handleGetEntity(requestIRI, message);
      }
      else if (request.getMessageType() == EventBusMessage.MessageType.CREATE_ENTITY) {
        handleCreateEntity(requestIRI, request, message);
      }
      else if (request.getMessageType() == EventBusMessage.MessageType.PATCH_ENTITY) {
        handlePatchEntity(requestIRI, request, message);
      }
      else if (request.getMessageType() == EventBusMessage.MessageType.UPDATE_ENTITY) {
        handleUpdateEntity(requestIRI, request, message);
      }
      else if (request.getMessageType() == EventBusMessage.MessageType.DELETE_ENTITY) {
        handleDeleteEntity(requestIRI, message);
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
  
  private void handleGetEntity(IRI requestIRI, Message<String> message) throws IllegalArgumentException, IOException {
    Optional<Graph> result = store.getEntityGraph(requestIRI);
    
    if (result.isPresent() && result.get().size() > 0) {
      replyWithPayload(message, store.graphToString(result.get(), RDFSyntax.TURTLE));
    } else {
      replyEntityNotFound(message);
    }
  }
  
  private void handleCreateEntity(IRI requestIRI, EventBusMessage request, Message<String> message) throws IllegalArgumentException, IOException {
    Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
    String entityIRIString = generateEntityIRI(requestIRI.getIRIString(), slug);
    IRI entityIRI = store.createIRI(entityIRIString);
    
    if (!request.getPayload().isPresent()) {
      replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      String entityGraphStr = request.getPayload().get();
      entityGraphStr = entityGraphStr.replaceAll("<>", "<" + entityIRIString + ">");
      
      Graph entityGraph = store.stringToGraph(request.getPayload().get(), entityIRI, RDFSyntax.TURTLE);
      store.createEntityGraph(entityIRI, entityGraph);
      replyWithPayload(message, entityGraphStr);
      
      vertx.eventBus().send(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, 
          new EventBusMessage(EventBusMessage.MessageType.ENTITY_CREATED_NOTIFICATION)
            .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIRIString)
            .setPayload(entityGraphStr)
            .toJson()
        );
    }
  }
  
  private void handlePatchEntity(IRI requestIRI, EventBusMessage request, Message<String> message) throws IllegalArgumentException, IOException {
    // TODO
  }
  
  private void handleUpdateEntity(IRI requestIRI, EventBusMessage request, Message<String> message) throws IllegalArgumentException, IOException {
    if (store.containsEntityGraph(requestIRI)) {
      if (!request.getPayload().isPresent()) {
        replyFailed(message);
      } else {
        Graph entityGraph = store.stringToGraph(request.getPayload().get(), requestIRI, RDFSyntax.TURTLE);
        store.updateEntityGraph(requestIRI, entityGraph);
        
        Optional<Graph> result = store.getEntityGraph(requestIRI);
        
        if (result.isPresent() && result.get().size() > 0) {
          String entityGraphStr = store.graphToString(result.get(), RDFSyntax.TURTLE);
          replyWithPayload(message, entityGraphStr);
          
          LOGGER.info("Sending update notification for " + requestIRI.getIRIString());
          
          vertx.eventBus().send(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, 
              new EventBusMessage(EventBusMessage.MessageType.ENTITY_CHANGED_NOTIFICATION)
                .setHeader(EventBusMessage.Headers.REQUEST_IRI, requestIRI.getIRIString())
                .setPayload(entityGraphStr)
                .toJson()
            );
        } else {
          replyFailed(message);
        }
      }
    } else {
      replyEntityNotFound(message);
    }
  }
  
  private void handleDeleteEntity(IRI requestIRI, Message<String> message) throws IllegalArgumentException, IOException {
    Optional<Graph> result = store.getEntityGraph(requestIRI);
    
    if (result.isPresent() && result.get().size() > 0) {
      String entityGraphStr = store.graphToString(result.get(), RDFSyntax.TURTLE);
      store.deleteEntityGraph(requestIRI);
      replyWithPayload(message, entityGraphStr);
      
      vertx.eventBus().send(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, 
          new EventBusMessage(EventBusMessage.MessageType.ENTITY_DELETED_NOTIFICATION)
            .setHeader(EventBusMessage.Headers.REQUEST_IRI, requestIRI.getIRIString())
            .setPayload(entityGraphStr)
            .toJson()
        );
    } else {
      replyEntityNotFound(message);
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
