package ro.andreiciortea.yggdrasil.store;

import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.rdf4j.RDF4J;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
/*
 * Stores the RDF graphs representing the instantiated artifacts
 *
 */
public class RdfStoreVerticle extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());

  private RdfStore store;
  private RDF4J rdf = new RDF4J();
  private HttpClient httpClient;

  @Override
  public void start() {
    store = RdfStoreFactory.createStore(config().getString("store"));

    httpClient = vertx.createHttpClient();

    EventBus eventBus = vertx.eventBus();

    eventBus.consumer(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, this::handleEntityRequest);
    eventBus.consumer(EventBusRegistry.RDF_STORE_QUERY_BUS_ADDRESS, this::handleQueryRequest);
  }

  private void handleEntityRequest(Message<String> message) {
    try {
      EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

      String requestIRIString = request.getHeader(EventBusMessage.Headers.REQUEST_IRI).get();
      IRI requestIRI = store.createIRI(requestIRIString);

      switch (request.getMessageType()) {
        case GET_ENTITY:
          handleGetEntity(requestIRI, request, message);
          break;
        case CREATE_ENTITY:
          handleCreateEntity(requestIRI, request, message);
          break;
        case PATCH_ENTITY:
          handlePatchEntity(requestIRI, request, message);
          break;
        case UPDATE_ENTITY:
          handleUpdateEntity(requestIRI, request, message);
          break;
        case DELETE_ENTITY:
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

  private void handleGetEntity(IRI requestIRI, EventBusMessage request, Message<String> message) throws IllegalArgumentException, IOException {
    Optional<Graph> result = store.getEntityGraph(requestIRI);
    RDFSyntax syntax = RDFSyntax.TURTLE;
    Optional<String> contentType = request.getHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE);

    if (contentType.isPresent() && contentType.get().equals("application/ld+json")) {
      syntax = RDFSyntax.JSONLD;
    }

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
  private void handleCreateEntity(IRI requestIRI, EventBusMessage request, Message<String> message) throws IllegalArgumentException, IOException {
	// Create IRI for new entity
    Graph entityGraph;
    Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
    Optional<String> contentType = request.getHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE);
    String entityIRIString = generateEntityIRI(requestIRI.getIRIString(), slug);
    IRI entityIRI = store.createIRI(entityIRIString);

    if (!request.getPayload().isPresent()) {
      replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      String entityGraphStr = request.getPayload().get();

      if (contentType.isPresent() && contentType.get().equals("application/ld+json")) {
        entityGraph = store.stringToGraph(request.getPayload().get(), entityIRI, RDFSyntax.JSONLD);
      } else {
        entityGraphStr = entityGraphStr.replaceAll("<>", "<" + entityIRIString + ">");
        entityGraph = store.stringToGraph(request.getPayload().get(), entityIRI, RDFSyntax.TURTLE);
      }
      IRI subscribesIri = rdf.createIRI("http://w3id.org/eve#subscribes");

      if (entityGraph.contains(null, subscribesIri, null)) {
        System.out.println("Crawler subscription link found!");
        subscribeCrawler(entityGraph);
      }

      store.createEntityGraph(entityIRI, entityGraph);
      // TODO: reply with original payload? or representation of created entity graph? (In the ideal case they are the same)
      replyWithPayload(message, entityGraphStr);

      vertx.eventBus().publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS,
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

          vertx.eventBus().publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS,
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

      vertx.eventBus().publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS,
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

  private void subscribeCrawler(Graph entityGraph) {
    IRI subscribesIri = rdf.createIRI("http://w3id.org/eve#subscribes");
    for (Triple t : entityGraph.iterate(null, subscribesIri, null)) {
      String crawlerUrl = t.getObject().toString();
      System.out.println(crawlerUrl);
      String id = t.getSubject().toString();
      httpClient.postAbs(crawlerUrl, new Handler<HttpClientResponse>() {

        @Override
        public void handle(HttpClientResponse httpClientResponse) {
          System.out.println("Registered at crawler: " + crawlerUrl);
        }
      }).end(id);
    }
  }
}
