package org.hyperagents.yggdrasil.store;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdfStoreVerticle.class.getName());

  private final RDF4J rdf = new RDF4J();
  private Messagebox<HttpNotificationDispatcherMessage> dispatcherMessagebox;
  private RdfStore store;
  private WebClient client;

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @Override
  public void start() {
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    this.store = RdfStoreFactory.createStore(config().getJsonObject("rdf-store", null));
    this.client = WebClient.create(this.vertx);
    final var ownMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      try {
        final var requestIri = this.store.createIri(message.body().requestUri());
        switch (message.body()) {
          case RdfStoreMessage.GetEntity ignored ->
            this.handleGetEntity(requestIri, message);
          case RdfStoreMessage.CreateEntity content ->
            this.handleCreateEntity(requestIri, content, message);
          case RdfStoreMessage.UpdateEntity content ->
            this.handleUpdateEntity(requestIri, content, message);
          case RdfStoreMessage.DeleteEntity ignored ->
            this.handleDeleteEntity(requestIri, message);
        }
      } catch (final IOException | IllegalArgumentException e) {
        LOGGER.error(e.getMessage());
        this.replyFailed(message);
      }
    });
  }

  private void handleGetEntity(
      final IRI requestIri,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException, IOException {
    final var result = this.store.getEntityGraph(requestIri);
    if (result.isPresent() && result.get().size() > 0) {
      this.replyWithPayload(message, this.store.graphToString(result.get(), RDFSyntax.TURTLE));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  /**
   * Creates an entity and adds it to the store.
   *
   * @param requestIri IRI where the request originated from
   * @param message Request
   */
  private void handleCreateEntity(
      final IRI requestIri,
      final RdfStoreMessage.CreateEntity content,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException, IOException {
    // Create IRI for new entity
    final var entityIriString =
        this.generateEntityIri(requestIri.getIRIString(), content.entityName());
    final var entityIri = this.store.createIri(entityIriString);
    final var optEntityGraphString = Optional.ofNullable(content.entityRepresentation());

    if (optEntityGraphString.filter(s -> !s.isEmpty()).isEmpty()) {
      this.replyFailed(message);
    } else {
      // Replace all null relative IRIs with the IRI generated for this entity
      final var entityGraphStr =
          optEntityGraphString.get().replaceAll("<>", "<" + entityIriString + ">");
      try (
          var entityGraph = this.store.stringToGraph(entityGraphStr, entityIri, RDFSyntax.TURTLE)
      ) {
        // TODO: seems like legacy integration from Simon Bienz, to be reviewed
        final var subscribesIri = this.rdf.createIRI("http://w3id.org/eve#subscribes");
        if (entityGraph.contains(null, subscribesIri, null)) {
          LOGGER.info("Crawler subscription link found!");
          this.subscribeCrawler(entityGraph);
        }
        this.store.createEntityGraph(entityIri, this.addContainmentTriples(entityIri, entityGraph));
        this.replyWithPayload(message, entityGraphStr);
        this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityCreated(
            requestIri.getIRIString(),
            entityGraphStr
        ));
      } catch (final Exception e) {
        LOGGER.error(e.getMessage());
      }
    }
  }

  private Graph addContainmentTriples(final IRI entityIri, final Graph entityGraph)
      throws IllegalArgumentException, IOException {
    LOGGER.info("Looking for containment triples for: " + entityIri.getIRIString());
    if (entityGraph.contains(
        entityIri,
        this.store.createIri(RDF.TYPE.stringValue()),
        this.store.createIri(("http://w3id.org/eve#Artifact"))
    )) {
      final var artifactIri = entityIri.getIRIString();
      final var workspaceIri =
          this.store.createIri(artifactIri.substring(0, artifactIri.indexOf("/artifacts")));

      LOGGER.info("Found workspace IRI: " + workspaceIri);

      final var optWorkspaceGraph = this.store.getEntityGraph(workspaceIri);
      if (optWorkspaceGraph.isPresent()) {
        try (var workspaceGraph = optWorkspaceGraph.get()) {
          LOGGER.info("Found workspace graph: " + workspaceGraph);
          workspaceGraph.add(workspaceIri, this.store.createIri("http://w3id.org/eve#contains"), entityIri);
          // TODO: updateEntityGraph would yield 404, to be investigated
          this.store.createEntityGraph(workspaceIri, workspaceGraph);
          this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityChanged(
              workspaceIri.getIRIString(),
              this.store.graphToString(workspaceGraph, RDFSyntax.TURTLE)
          ));
        } catch (final Exception e) {
          LOGGER.error(e.getMessage());
        }
      }
    } else if (entityGraph.contains(
        entityIri,
        this.store.createIri(RDF.TYPE.stringValue()),
        this.store.createIri(("http://w3id.org/eve#WorkspaceArtifact"))
    )) {
      final var workspaceIri = entityIri.getIRIString();
      final var environmentIri =
          this.store.createIri(workspaceIri.substring(0, workspaceIri.indexOf("/workspaces")));

      LOGGER.info("Found env IRI: " + workspaceIri);

      final var optEnvironmentGraph = this.store.getEntityGraph(environmentIri);
      if (optEnvironmentGraph.isPresent()) {
        try (var environmentGraph = optEnvironmentGraph.get()) {
          LOGGER.info("Found env graph: " + environmentGraph);
          environmentGraph.add(
              environmentIri,
              this.store.createIri("http://w3id.org/eve#contains"),
              entityIri
          );
          // TODO: updateEntityGraph would yield 404, to be investigated
          this.store.createEntityGraph(environmentIri, environmentGraph);
          this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityChanged(
              environmentIri.getIRIString(),
              this.store.graphToString(environmentGraph, RDFSyntax.TURTLE)
          ));
        } catch (final Exception e) {
          LOGGER.error(e.getMessage());
        }
      }
    }
    return entityGraph;
  }

  private void handleUpdateEntity(
      final IRI requestIri,
      final RdfStoreMessage.UpdateEntity content,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException, IOException {
    if (this.store.containsEntityGraph(requestIri)) {
      final var optEntityGraphString = Optional.ofNullable(content.entityRepresentation());
      if (optEntityGraphString.filter(s -> !s.isEmpty()).isEmpty()) {
        this.replyFailed(message);
      } else {
        this.store.updateEntityGraph(
            requestIri,
            this.store.stringToGraph(optEntityGraphString.get(), requestIri, RDFSyntax.TURTLE)
        );
        final var result = this.store.getEntityGraph(requestIri);
        if (result.isPresent() && result.get().size() > 0) {
          final var entityGraphString = this.store.graphToString(result.get(), RDFSyntax.TURTLE);
          this.replyWithPayload(message, entityGraphString);

          LOGGER.info("Sending update notification for " + requestIri.getIRIString());

          this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityChanged(
              requestIri.getIRIString(),
              entityGraphString
          ));
        } else {
          this.replyFailed(message);
        }
      }
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private void handleDeleteEntity(final IRI requestIri, final Message<RdfStoreMessage> message)
      throws IllegalArgumentException, IOException {
    final var result = this.store.getEntityGraph(requestIri);
    if (result.isPresent() && result.get().size() > 0) {
      final var entityGraphString = this.store.graphToString(result.get(), RDFSyntax.TURTLE);
      this.store.deleteEntityGraph(requestIri);
      this.replyWithPayload(message, entityGraphString);
      this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityDeleted(
          requestIri.getIRIString(),
          entityGraphString
      ));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private void replyWithPayload(final Message<RdfStoreMessage> message, final String payload) {
    message.reply(payload);
  }

  private void replyFailed(final Message<RdfStoreMessage> message) {
    message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Store request failed.");
  }

  private void replyEntityNotFound(final Message<RdfStoreMessage> message) {
    message.fail(HttpStatus.SC_NOT_FOUND, "Entity not found.");
  }

  private String generateEntityIri(final String requestIri, final String hint) {
    final var fullRequestIri = !requestIri.endsWith("/") ? requestIri.concat("/") : requestIri;
    final var optHint = Optional.ofNullable(hint).filter(s -> !s.isEmpty());
    // Try to generate an IRI using the hint provided in the initial request
    if (optHint.isPresent()) {
      final var candidateIri = fullRequestIri.concat(optHint.get());
      if (!this.store.containsEntityGraph(this.store.createIri(candidateIri))) {
        return candidateIri;
      }
    }
    // Generate a new IRI
    return Stream.generate(() -> UUID.randomUUID().toString())
                 .map(fullRequestIri::concat)
                 .dropWhile(i -> this.store.containsEntityGraph(this.store.createIri(i)))
                 .findFirst()
                 .orElseThrow();
  }

  private void subscribeCrawler(final Graph entityGraph) {
    entityGraph.iterate(null, this.rdf.createIRI("http://w3id.org/eve#subscribes"), null).forEach(t -> {
      final var crawlerUrl = t.getObject().toString();
      LOGGER.info(crawlerUrl);
      this.client
          .postAbs(crawlerUrl)
          .sendBuffer(
            Buffer.buffer(t.getSubject().toString()),
            response -> LOGGER.info("Registered at crawler: " + crawlerUrl)
          );
    });
  }
}
