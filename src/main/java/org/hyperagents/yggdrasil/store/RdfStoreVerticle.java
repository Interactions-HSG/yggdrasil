package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import ch.unisg.ics.interactions.hmas.core.vocabularies.HMAS;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.vocabularies.INTERACTION;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
        case RdfStore.GET_ENTITY_FOR_AGENT:
          handleGetEntityForAgent(requestIRI, message);
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
    LOGGER.info("GET ENTITY");
    Optional<Graph> result = store.getEntityGraph(requestIRI);
    if (result.isPresent() && result.get().size() > 0) {
      replyWithPayload(message, store.graphToString(result.get(), RDFSyntax.TURTLE));
    } else {
      replyEntityNotFound(message);
    }
  }

  private void handleGetEntityForAgent(IRI requestIRI, Message<String> message)
    throws IllegalArgumentException, IOException {

    LOGGER.info("GET ENTITY FOR AGENT");
    String agentIRIString = message.headers().get(HttpEntityHandler.AGENT_WEB_ID);
    IRI agentIRI = store.createIRI(agentIRIString);

    Optional<Graph> entityResult = store.getEntityGraph(requestIRI);
    Optional<Graph> agentResult = store.getEntityGraph(agentIRI);
    LOGGER.info(requestIRI);
    LOGGER.info(agentIRI);

    if (entityResult.isPresent() && entityResult.get().size() > 0) {
      String entityStr = store.graphToString(entityResult.get(), RDFSyntax.TURTLE);
      LOGGER.info(entityStr);
      if (agentResult.isPresent()  && entityResult.get().size() > 0) {
        String agentStr = store.graphToString(agentResult.get(), RDFSyntax.TURTLE);
        LOGGER.info("AGENT FOUND");
        LOGGER.info(agentStr);
        DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.AGENT_ENTITY, agentStr);

        replyWithPayloadAndOptions(message, entityStr, options);
      } else {
        LOGGER.info("AGENT NOT FOUND");
        replyWithPayload(message, entityStr);
      }
    } else {
      replyEntityNotFound(message);
    }
  }

  /**
   * Creates an entity and adds it to the store
   * @param requestIRI	IRI where the request originated from
   * @param message Request
   * @throws IllegalArgumentException
   * @throws IOException
   */
  private void handleCreateEntity(IRI requestIRI, Message<String> message)
    throws IllegalArgumentException, IOException {
    // Create IRI for new entity
    Graph entityGraph;

    String slug = message.headers().get(HttpEntityHandler.ENTITY_URI_HINT);
//    String contentType = message.headers().get(HttpEntityHandler.CONTENT_TYPE);
    String entityIRIString = generateEntityIRI(requestIRI.getIRIString(), slug);

    IRI entityIRI = store.createIRI(entityIRIString);

    if (message.body() == null || message.body().isEmpty()) {
      replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      String entityGraphStr = message.body();

//      if (contentType != null && contentType.equals("application/ld+json")) {
//        entityGraph = store.stringToGraph(entityGraphStr, entityIRI, RDFSyntax.JSONLD);
//      } else {
      entityGraphStr = entityGraphStr.replaceAll("<>", "<" + entityIRIString + ">");
      entityGraph = store.stringToGraph(entityGraphStr, entityIRI, RDFSyntax.TURTLE);
//      }

      // TODO: seems like legacy integration from Simon Bienz, to be reviewed
      IRI subscribesIri = rdf.createIRI("http://w3id.org/eve#subscribes");
      if (entityGraph.contains(null, subscribesIri, null)) {
        LOGGER.info("Crawler subscription link found!");
        subscribeCrawler(entityGraph);
      }

      entityGraph = addContainmentTriples(entityIRI, entityGraph);

      store.createEntityGraph(entityIRI, entityGraph);
      replyWithPayload(message, entityGraphStr);

//      DeliveryOptions options = new DeliveryOptions()
//          .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
//              .ENTITY_CREATED)
//          .addHeader(HttpEntityHandler.REQUEST_URI, entityIRIString);
//
//      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr, options);

      pushNotification(HttpNotificationVerticle.ENTITY_CREATED, requestIRI, entityGraphStr);
    }
  }

  private Graph addContainmentTriples(IRI entityIRI, Graph entityGraph)
    throws IllegalArgumentException, IOException {
    LOGGER.info("Looking for containment triples for: " + entityIRI.getIRIString());

    Optional<IRI> containedEntityIRI = getContainedEntity(entityIRI, entityGraph);
    if (containedEntityIRI.isPresent()) {
      String artifactIRI = containedEntityIRI.get().getIRIString();
      IRI workspaceIRI = store.createIRI(artifactIRI.substring(0, artifactIRI.indexOf("/artifacts")));

      LOGGER.info("Found workspace IRI: " + workspaceIRI);

      Optional<Graph> workspaceGraph = store.getEntityGraph(workspaceIRI);
      if (workspaceGraph.isPresent()) {
        Graph wkspGraph = workspaceGraph.get();
        LOGGER.info("Found workspace graph: " + wkspGraph);
        wkspGraph.add(workspaceIRI, store.createIRI("http://w3id.org/eve#contains"), containedEntityIRI.get());
        // TODO: updateEntityGraph would yield 404, to be investigated
        store.createEntityGraph(workspaceIRI, wkspGraph);

        String entityGraphStr = store.graphToString(wkspGraph, RDFSyntax.TURTLE);
        pushNotification(HttpNotificationVerticle.ENTITY_CHANGED, workspaceIRI, entityGraphStr);
      }
    } else if (entityGraph.contains(entityIRI, store.createIRI(RDF.TYPE.stringValue()),
      store.createIRI(("http://w3id.org/eve#WorkspaceArtifact")))) {
      String workspaceIRI = entityIRI.getIRIString();
      IRI envIRI = store.createIRI(workspaceIRI.substring(0, workspaceIRI.indexOf("/workspaces")));

      LOGGER.info("Found env IRI: " + workspaceIRI);

      Optional<Graph> envGraph = store.getEntityGraph(envIRI);
      if (envGraph.isPresent()) {
        Graph graph = envGraph.get();
        LOGGER.info("Found env graph: " + graph);
        graph.add(envIRI, store.createIRI("http://w3id.org/eve#contains"), entityIRI);
        // TODO: updateEntityGraph would yield 404, to be investigated
        store.createEntityGraph(envIRI, graph);

        String entityGraphStr = store.graphToString(graph, RDFSyntax.TURTLE);
        pushNotification(HttpNotificationVerticle.ENTITY_CHANGED, envIRI, entityGraphStr);
      }
    }

    return entityGraph;
  }

  private Optional<IRI> getContainedEntity(IRI entityIRI, Graph entityGraph) {
    if (entityGraph.contains(entityIRI, store.createIRI(RDF.TYPE.stringValue()),
      store.createIRI(("http://w3id.org/eve#Artifact")))) {
      return Optional.of(entityIRI);
    }
    Triple tr = entityGraph.stream()
      .filter(t -> HMAS.IS_PROFILE_OF.toString().equals(t.getPredicate().toString())
        && entityIRI.getIRIString().equals(t.getSubject().toString()))
      .findFirst()
      .orElse(null);

    if (tr != null && tr.getObject() instanceof IRI) {
      IRI objectIRI = store.createIRI(tr.getObject().toString());
      // If this the profile of an artifact, then the artifact is contained
      if (entityGraph.contains(objectIRI, store.createIRI(RDF.TYPE.stringValue()),
        store.createIRI(HMAS.ARTIFACT.toString()))) {
        return Optional.of(objectIRI);
      }

      // If this the profile of an agent, then the body of the agent is contained
      Triple tr2 = entityGraph.stream()
        .filter(t -> INTERACTION.HAS_AGENT_BODY.toString().equals(t.getPredicate().toString())
          && tr.getObject().equals(t.getSubject()))
        .findFirst()
        .orElse(null);

      if (tr2 != null && tr2.getObject() instanceof IRI) {
        IRI agentBodyIRI = store.createIRI(tr2.getObject().toString());
        return Optional.of(agentBodyIRI);
      }
    }
    return Optional.empty();
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

//          DeliveryOptions options = new DeliveryOptions()
//              .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
//                  .ENTITY_CHANGED)
//              .addHeader(HttpEntityHandler.REQUEST_URI, requestIRI.getIRIString());
//
//          vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr,
//              options);

          pushNotification(HttpNotificationVerticle.ENTITY_CHANGED, requestIRI, entityGraphStr);
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

//      DeliveryOptions options = new DeliveryOptions()
//          .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
//              .ENTITY_DELETED)
//          .addHeader(HttpEntityHandler.REQUEST_URI, requestIRI.getIRIString());
//
//      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraphStr, options);

      pushNotification(HttpNotificationVerticle.ENTITY_DELETED, requestIRI, entityGraphStr);
    } else {
      replyEntityNotFound(message);
    }
  }

  private void pushNotification(String notificationType, IRI requestIRI, String entityGraph) {
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, notificationType)
      .addHeader(HttpEntityHandler.REQUEST_URI, requestIRI.getIRIString());

    vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, entityGraph, options);
  }

  private void replyWithPayloadAndOptions(Message<String> message, String payload, DeliveryOptions options) {
    message.reply(payload, options);
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
