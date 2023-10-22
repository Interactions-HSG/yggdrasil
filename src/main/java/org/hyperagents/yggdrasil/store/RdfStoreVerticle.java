package org.hyperagents.yggdrasil.store;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.messages.*;
import org.hyperagents.yggdrasil.messages.impl.HttpNotificationDispatcherMessageboxImpl;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());

  private final RDF4J rdf = new RDF4J();
  private HttpNotificationDispatcherMessagebox dispatcherMessagebox;
  private RdfStore store;
  private WebClient client;

  @Override
  public void start() {
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessageboxImpl(this.vertx.eventBus());
    this.store = RdfStoreFactory.createStore(config().getJsonObject("rdf-store", null));
    this.client = WebClient.create(this.vertx);
    this.vertx.eventBus().<String>consumer(MessageAddresses.RDF_STORE.getName(), message -> {
      try {
        final var requestIRI = this.store.createIRI(message.headers().get(MessageHeaders.REQUEST_URI.getName()));
        switch (MessageRequestMethods.valueOf(message.headers().get(MessageHeaders.REQUEST_METHOD.getName()))) {
          case GET_ENTITY:
            this.handleGetEntity(requestIRI, message);
            break;
          case CREATE_ENTITY:
            this.handleCreateEntity(requestIRI, message);
            break;
          case PATCH_ENTITY:
            this.handlePatchEntity(requestIRI, message);
            break;
          case UPDATE_ENTITY:
            this.handleUpdateEntity(requestIRI, message);
            break;
          case DELETE_ENTITY:
            this.handleDeleteEntity(requestIRI, message);
            break;
          default:
            break;
        }
      } catch (final IOException | IllegalArgumentException e) {
        LOGGER.error(e.getMessage());
        this.replyFailed(message);
      }
    });
  }

  private void handleGetEntity(final IRI requestIRI, final Message<String> message) throws IllegalArgumentException, IOException {
    final var result = this.store.getEntityGraph(requestIRI);
    if (result.isPresent() && result.get().size() > 0) {
      this.replyWithPayload(message, this.store.graphToString(result.get(), RDFSyntax.TURTLE));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  /**
   * Creates an entity and adds it to the store
   * @param requestIRI IRI where the request originated from
   * @param message Request
   * @throws IllegalArgumentException
   * @throws IOException
   */
  private void handleCreateEntity(final IRI requestIRI, final Message<String> message)
    throws IllegalArgumentException, IOException {
	  // Create IRI for new entity
    final var entityIRIString =
      this.generateEntityIRI(requestIRI.getIRIString(), message.headers().get(MessageHeaders.ENTITY_URI_HINT.getName()));
    final var entityIRI = this.store.createIRI(entityIRIString);
    final var optEntityGraphString = Optional.ofNullable(message.body());

    if (optEntityGraphString.filter(s -> !s.isEmpty()).isEmpty()) {
      this.replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      final var entityGraphStr = optEntityGraphString.get().replaceAll("<>", "<" + entityIRIString + ">");
      final Graph entityGraph = this.store.stringToGraph(entityGraphStr, entityIRI, RDFSyntax.TURTLE);
      // TODO: seems like legacy integration from Simon Bienz, to be reviewed
      final var subscribesIri = this.rdf.createIRI("http://w3id.org/eve#subscribes");
      if (entityGraph.contains(null, subscribesIri, null)) {
        LOGGER.info("Crawler subscription link found!");
        this.subscribeCrawler(entityGraph);
      }
      this.store.createEntityGraph(entityIRI, this.addContainmentTriples(entityIRI, entityGraph));
      this.replyWithPayload(message, entityGraphStr);
      this.dispatcherMessagebox.pushNotification(MessageNotifications.ENTITY_CREATED, requestIRI, entityGraphStr);
    }
  }

  private Graph addContainmentTriples(final IRI entityIRI, final Graph entityGraph) throws IllegalArgumentException, IOException {
    LOGGER.info("Looking for containment triples for: " + entityIRI.getIRIString());
    if (entityGraph.contains(
      entityIRI,
      this.store.createIRI(RDF.TYPE.stringValue()),
      this.store.createIRI(("http://w3id.org/eve#Artifact"))
    )) {
      final var artifactIRI = entityIRI.getIRIString();
      final var workspaceIRI = this.store.createIRI(artifactIRI.substring(0, artifactIRI.indexOf("/artifacts")));

      LOGGER.info("Found workspace IRI: " + workspaceIRI);

      final var optWorkspaceGraph = this.store.getEntityGraph(workspaceIRI);
      if (optWorkspaceGraph.isPresent()) {
        final var workspaceGraph = optWorkspaceGraph.get();
        LOGGER.info("Found workspace graph: " + workspaceGraph);
        workspaceGraph.add(workspaceIRI, this.store.createIRI("http://w3id.org/eve#contains"), entityIRI);
        // TODO: updateEntityGraph would yield 404, to be investigated
        this.store.createEntityGraph(workspaceIRI, workspaceGraph);
        this.dispatcherMessagebox.pushNotification(
          MessageNotifications.ENTITY_CHANGED,
          workspaceIRI,
          this.store.graphToString(workspaceGraph, RDFSyntax.TURTLE)
        );
      }
    } else if (entityGraph.contains(
      entityIRI,
      this.store.createIRI(RDF.TYPE.stringValue()),
      this.store.createIRI(("http://w3id.org/eve#WorkspaceArtifact"))
    )) {
      final var workspaceIRI = entityIRI.getIRIString();
      final var environmentIRI = this.store.createIRI(workspaceIRI.substring(0, workspaceIRI.indexOf("/workspaces")));

      LOGGER.info("Found env IRI: " + workspaceIRI);

      final var optEnvironmentGraph = this.store.getEntityGraph(environmentIRI);
      if (optEnvironmentGraph.isPresent()) {
        final var environmentGraph = optEnvironmentGraph.get();
        LOGGER.info("Found env graph: " + environmentGraph);
        environmentGraph.add(environmentIRI, this.store.createIRI("http://w3id.org/eve#contains"), entityIRI);
        // TODO: updateEntityGraph would yield 404, to be investigated
        this.store.createEntityGraph(environmentIRI, environmentGraph);
        this.dispatcherMessagebox.pushNotification(
          MessageNotifications.ENTITY_CHANGED,
          environmentIRI,
          this.store.graphToString(environmentGraph, RDFSyntax.TURTLE)
        );
      }
    }
    return entityGraph;
  }

  private void handlePatchEntity(final IRI requestIRI, final Message<String> message)
    throws IllegalArgumentException, IOException {
    throw new NotImplementedException("It is not possible to patch an entity right now");
  }

  private void handleUpdateEntity(final IRI requestIRI, final Message<String> message)
      throws IllegalArgumentException, IOException {
    if (this.store.containsEntityGraph(requestIRI)) {
      final var optEntityGraphString = Optional.ofNullable(message.body());
      if (optEntityGraphString.filter(s -> !s.isEmpty()).isEmpty()) {
        this.replyFailed(message);
      } else {
        this.store.updateEntityGraph(
          requestIRI,
          this.store.stringToGraph(optEntityGraphString.get(), requestIRI, RDFSyntax.TURTLE)
        );
        final var result = this.store.getEntityGraph(requestIRI);
        if (result.isPresent() && result.get().size() > 0) {
          final var entityGraphString = this.store.graphToString(result.get(), RDFSyntax.TURTLE);
          this.replyWithPayload(message, entityGraphString);

          LOGGER.info("Sending update notification for " + requestIRI.getIRIString());

          this.dispatcherMessagebox.pushNotification(MessageNotifications.ENTITY_CHANGED, requestIRI, entityGraphString);
        } else {
          this.replyFailed(message);
        }
      }
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private void handleDeleteEntity(final IRI requestIRI, final Message<String> message)
    throws IllegalArgumentException, IOException {
    final var result = this.store.getEntityGraph(requestIRI);
    if (result.isPresent() && result.get().size() > 0) {
      final var entityGraphString = this.store.graphToString(result.get(), RDFSyntax.TURTLE);
      this.store.deleteEntityGraph(requestIRI);
      this.replyWithPayload(message, entityGraphString);
      this.dispatcherMessagebox.pushNotification(MessageNotifications.ENTITY_DELETED, requestIRI, entityGraphString);
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private void replyWithPayload(final Message<String> message, final String payload) {
    message.reply(payload);
  }

  private void replyFailed(final Message<String> message) {
    message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Store request failed.");
  }

  private void replyEntityNotFound(final Message<String> message) {
    message.fail(HttpStatus.SC_NOT_FOUND, "Entity not found.");
  }

  private String generateEntityIRI(final String requestIRI, final String hint) {
    final var fullRequestIRI = !requestIRI.endsWith("/") ? requestIRI.concat("/") : requestIRI;
    final var optHint = Optional.ofNullable(hint).filter(s -> !s.isEmpty());
    // Try to generate an IRI using the hint provided in the initial request
    if (optHint.isPresent()) {
      final var candidateIRI = fullRequestIRI.concat(optHint.get());
      if (!this.store.containsEntityGraph(this.store.createIRI(candidateIRI))) {
        return candidateIRI;
      }
    }
    // Generate a new IRI
    return Stream.generate(() -> UUID.randomUUID().toString())
                 .map(fullRequestIRI::concat)
                 .dropWhile(i -> this.store.containsEntityGraph(this.store.createIRI(i)))
                 .findFirst()
                 .orElseThrow();
  }

  private void subscribeCrawler(final Graph entityGraph) {
    entityGraph.iterate(null, this.rdf.createIRI("http://w3id.org/eve#subscribes"), null).forEach(t -> {
      final var crawlerUrl = t.getObject().toString();
      LOGGER.info(crawlerUrl);
      this.client
          .postAbs(crawlerUrl)
          .sendBuffer(Buffer.buffer(t.getSubject().toString()), response -> LOGGER.info("Registered at crawler: " + crawlerUrl));
    });
  }
}
