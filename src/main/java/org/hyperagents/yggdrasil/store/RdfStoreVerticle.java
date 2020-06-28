package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

/*
 * Stores the RDF graphs representing the instantiated artifacts
 *
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());

  private RdfStore store;
  private RDF4J rdf = new RDF4J();
  private WebClient client;

  @Override
  public void start() {
    store = RdfStoreFactory.createStore(config().getString("store"));
    client = WebClient.create(vertx);

    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(RdfStore.BUS_ADDRESS, this::handleEntityRequest);
  }

  private void handleEntityRequest(Message<String> message) {
    try {
      String requestIRIString = message.headers().get(HttpEntityHandler.REQUEST_URI);
      IRI requestIRI = store.createIRI(requestIRIString);

      String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
      switch (requestMethod) {
        case RdfStore.GET_ENTITY:
          handleGetEntity(requestIRI, message);
          break;
        case RdfStore.CREATE_ENTITY:
          handleCreateEntity(requestIRI, message);
          break;
        case RdfStore.PATCH_ENTITY:
          handlePatchEntity(requestIRI, message);
          break;
        case RdfStore.UPDATE_ENTITY:
          handleUpdateEntity(requestIRI, message);
          break;
        case RdfStore.DELETE_ENTITY:
          handleDeleteEntity(requestIRI, message);
          break;
        default:
        	break;
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

  private void handleGetEntity(IRI requestIRI, Message<String> message) 
      throws IllegalArgumentException, IOException {
    
    RDFSyntax syntax = RDFSyntax.TURTLE;
    String contentType = message.headers().get(HttpEntityHandler.CONTENT_TYPE);
    
    if (contentType != null && contentType.equals("application/ld+json")) {
      syntax = RDFSyntax.JSONLD;
    }
    
    Optional<Graph> result = store.getEntityGraph(requestIRI);
    
    if (result.isPresent() && result.get().size() > 0) {
      replyWithPayload(message, store.graphToString(result.get(), syntax));
    } else {
      replyEntityNotFound(message);
    }
  }

  /**
   * Creates an entity and adds it to the store
   * @param requestIRI	IRI where the request originated from
   * @param request Eventbus message describing the request
   * @param message Request
   * @throws IllegalArgumentException
   * @throws IOException
   */
  private void handleCreateEntity(IRI requestIRI, Message<String> message) 
      throws IllegalArgumentException, IOException {
	// Create IRI for new entity
    Graph entityGraph;
    
    String slug = message.headers().get(HttpEntityHandler.ENTITY_URI_HINT);
    String contentType = message.headers().get(HttpEntityHandler.CONTENT_TYPE);
    String entityIRIString = generateEntityIRI(requestIRI.getIRIString(), slug);
    
    IRI entityIRI = store.createIRI(entityIRIString);

    if (message.body() == null || message.body().isEmpty()) {
      replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      String entityGraphStr = message.body();

      if (contentType != null && contentType.equals("application/ld+json")) {
        entityGraph = store.stringToGraph(entityGraphStr, entityIRI, RDFSyntax.JSONLD);
      } else {
        entityGraphStr = entityGraphStr.replaceAll("<>", "<" + entityIRIString + ">");
        entityGraph = store.stringToGraph(entityGraphStr, entityIRI, RDFSyntax.TURTLE);
      }
      
      // TODO: seems like legacy integration from Simon Bienz, to be reviewed
      IRI subscribesIri = rdf.createIRI("http://w3id.org/eve#subscribes");
      if (entityGraph.contains(null, subscribesIri, null)) {
        System.out.println("Crawler subscription link found!");
        subscribeCrawler(entityGraph);
      }
      
      store.createEntityGraph(entityIRI, entityGraph);
      replyWithPayload(message, entityGraphStr);
      
      DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
              .ENTITY_CREATED)
          .addHeader(HttpEntityHandler.REQUEST_URI, entityIRIString);
      
      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr, options);
    }
  }

  private void handlePatchEntity(IRI requestIRI, Message<String> message) 
      throws IllegalArgumentException, IOException {
    // TODO
  }
  
  private void handleUpdateEntity(IRI requestIRI, Message<String> message) 
      throws IllegalArgumentException, IOException {
    if (store.containsEntityGraph(requestIRI)) {
      if (message.body() == null || message.body().isEmpty()) {
        replyFailed(message);
      } else {
        Graph entityGraph = store.stringToGraph(message.body(), requestIRI, RDFSyntax.TURTLE);
        store.updateEntityGraph(requestIRI, entityGraph);
        
        Optional<Graph> result = store.getEntityGraph(requestIRI);

        if (result.isPresent() && result.get().size() > 0) {
          String entityGraphStr = store.graphToString(result.get(), RDFSyntax.TURTLE);
          replyWithPayload(message, entityGraphStr);

          LOGGER.info("Sending update notification for " + requestIRI.getIRIString());

          DeliveryOptions options = new DeliveryOptions()
              .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
                  .ENTITY_CHANGED)
              .addHeader(HttpEntityHandler.REQUEST_URI, requestIRI.getIRIString());
          
          vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr, 
              options);
        } else {
          replyFailed(message);
        }
      }
    } else {
      replyEntityNotFound(message);
    }
  }

  private void handleDeleteEntity(IRI requestIRI, Message<String> message) 
      throws IllegalArgumentException, IOException {
    Optional<Graph> result = store.getEntityGraph(requestIRI);

    if (result.isPresent() && result.get().size() > 0) {
      String entityGraphStr = store.graphToString(result.get(), RDFSyntax.TURTLE);
      store.deleteEntityGraph(requestIRI);
      replyWithPayload(message, entityGraphStr);

      DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
              .ENTITY_DELETED)
          .addHeader(HttpEntityHandler.REQUEST_URI, requestIRI.getIRIString());
      
      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr, options);
    } else {
      replyEntityNotFound(message);
    }
  }

  private void replyWithPayload(Message<String> message, String payload) {
    message.reply(payload);
  }

  private void replyFailed(Message<String> message) {
    message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Store request failed.");
  }
  
  private void replyEntityNotFound(Message<String> message) {
    message.fail(HttpStatus.SC_NOT_FOUND, "Entity not found.");
  }

  private String generateEntityIRI(String requestIRI, String hint) {
    if (!requestIRI.endsWith("/")) {
      requestIRI = requestIRI.concat("/");
    }

    String candidateIRI;

    // Try to generate an IRI using the hint provided in the initial request
    if (hint != null && !hint.isEmpty()) {
      candidateIRI = requestIRI.concat(hint);
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

  private void subscribeCrawler(Graph entityGraph) {
    IRI subscribesIri = rdf.createIRI("http://w3id.org/eve#subscribes");
    for (Triple t : entityGraph.iterate(null, subscribesIri, null)) {
      String crawlerUrl = t.getObject().toString();
      LOGGER.info(crawlerUrl);
      
      String id = t.getSubject().toString();
      client.postAbs(crawlerUrl).sendBuffer(Buffer.buffer(id), response -> {
        LOGGER.info("Registered at crawler: " + crawlerUrl);
      });
    }
  }
}
