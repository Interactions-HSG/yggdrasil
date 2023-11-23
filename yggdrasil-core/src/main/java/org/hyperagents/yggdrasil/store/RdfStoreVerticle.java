package org.hyperagents.yggdrasil.store;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;
import org.hyperagents.yggdrasil.utils.GraphUtils;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RdfStoreVerticle.class);

  private Messagebox<HttpNotificationDispatcherMessage> dispatcherMessagebox;
  private RdfStore store;

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @Override
  public void start() {
    final var httpConfig = new HttpInterfaceConfigImpl(this.config());
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    this.store = RdfStoreFactory.createStore(this.config().getJsonObject("rdf-store", null));
    try {
      final var platformIri = GraphUtils.createIri(httpConfig.getBaseUri() + "/");
      this.store.addEntityGraph(
          platformIri,
          GraphUtils.stringToGraph(
            new RepresentationFactoryImpl(httpConfig).createPlatformRepresentation(),
            platformIri,
            RDFSyntax.TURTLE
          )
      );
    } catch (final IOException e) {
      LOGGER.error(e.getMessage());
    }
    final var ownMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      try {
        final var requestIri = GraphUtils.createIri(message.body().requestUri());
        switch (message.body()) {
          case RdfStoreMessage.GetEntity ignored ->
            this.handleGetEntity(requestIri, message);
          case RdfStoreMessage.CreateArtifact content ->
            this.handleCreateArtifact(requestIri, content, message);
          case RdfStoreMessage.CreateWorkspace content ->
            this.handleCreateWorkspace(requestIri, content, message);
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
      this.replyWithPayload(message, GraphUtils.graphToString(result.get(), RDFSyntax.TURTLE));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  /**
   * Creates an artifact and adds it to the store.
   *
   * @param requestIri IRI where the request originated from
   * @param message Request
   */
  private void handleCreateArtifact(
      final IRI requestIri,
      final RdfStoreMessage.CreateArtifact content,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException {
    // Create IRI for new entity
    final var entityIriString =
        this.generateEntityIri(requestIri.getIRIString(), content.artifactName());
    final var entityIri = GraphUtils.createIri(entityIriString);
    Optional
        .ofNullable(content.artifactRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + entityIriString + ">"))
        .ifPresentOrElse(
          s -> {
            try (var entityGraph = GraphUtils.stringToGraph(s, entityIri, RDFSyntax.TURTLE)) {
              LOGGER.info("entity created is an artifact");
              final var artifactIri = entityIri.getIRIString();
              final var workspaceIri =
                  GraphUtils.createIri(artifactIri.substring(0, artifactIri.indexOf("/artifacts")));

              LOGGER.info("Found workspace IRI: " + workspaceIri);

              this.store
                  .getEntityGraph(workspaceIri)
                  .ifPresent(workspaceGraph -> {
                    try (workspaceGraph) {
                      LOGGER.info("Found workspace graph: " + workspaceGraph);
                      workspaceGraph.add(
                          workspaceIri,
                          GraphUtils.createIri("https://purl.org/hmas/core/directlyContains"),
                          entityIri
                      );
                      workspaceGraph.add(
                          entityIri,
                          GraphUtils.createIri(RDF.TYPE.toString()),
                          GraphUtils.createIri("https://purl.org/hmas/core/Artifact")
                      );
                      this.store.updateEntityGraph(workspaceIri, workspaceGraph);
                      this.dispatcherMessagebox.sendMessage(
                        new HttpNotificationDispatcherMessage.EntityChanged(
                          workspaceIri.getIRIString(),
                          GraphUtils.graphToString(workspaceGraph, RDFSyntax.TURTLE)
                        )
                      );
                    } catch (final Exception e) {
                      LOGGER.error(e);
                    }
                  });
              this.store.createEntityGraph(entityIri, entityGraph);
              final var stringGraphResult = GraphUtils.graphToString(entityGraph, RDFSyntax.TURTLE);
              this.replyWithPayload(message, stringGraphResult);
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.getIRIString(),
                  stringGraphResult
                )
              );
            } catch (final Exception e) {
              LOGGER.error(e);
              this.replyFailed(message);
            }
          },
          () -> this.replyFailed(message)
        );
  }

  /**
   * Creates an entity and adds it to the store.
   *
   * @param requestIri IRI where the request originated from
   * @param message Request
   */
  private void handleCreateWorkspace(
      final IRI requestIri,
      final RdfStoreMessage.CreateWorkspace content,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException {
    // Create IRI for new entity
    final var entityIriString =
        this.generateEntityIri(requestIri.getIRIString(), content.workspaceName());
    final var entityIri = GraphUtils.createIri(entityIriString);
    Optional
        .ofNullable(content.workspaceRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + entityIriString + ">"))
        .ifPresentOrElse(
          s -> {
            try (var entityGraph = GraphUtils.stringToGraph(s, entityIri, RDFSyntax.TURTLE)) {
              if (content.parentWorkspaceUri().isPresent()) {
                final var parentIri = GraphUtils.createIri(content.parentWorkspaceUri().get());
                entityGraph.add(
                    entityIri,
                    GraphUtils.createIri("https://purl.org/hmas/core/isContainedBy"),
                    parentIri
                );
                this.store
                    .getEntityGraph(parentIri)
                    .ifPresent(parentGraph -> {
                      try (parentGraph) {
                        parentGraph.add(
                            parentIri,
                            GraphUtils.createIri("https://purl.org/hmas/core/contains"),
                            entityIri
                        );
                        parentGraph.add(
                            entityIri,
                            GraphUtils.createIri(RDF.TYPE.toString()),
                            GraphUtils.createIri("https://purl.org/hmas/core/Workspace")
                        );
                      } catch (final Exception e) {
                        LOGGER.error(e.getMessage());
                      }
                    });
              } else {
                final var workspaceIri = entityIri.getIRIString();
                final var platformIri = GraphUtils.createIri(
                  workspaceIri.substring(0, workspaceIri.indexOf("/workspaces"))
                );

                LOGGER.info("Found platform IRI: " + workspaceIri);

                this.store
                    .getEntityGraph(platformIri)
                    .ifPresent(platformGraph -> {
                      try (platformGraph) {
                        LOGGER.info("Found platform graph: " + platformGraph);
                        platformGraph.add(
                            platformIri,
                            GraphUtils.createIri("https://purl.org/hmas/core/hosts"),
                            entityIri
                        );
                        platformGraph.add(
                            entityIri,
                            GraphUtils.createIri(RDF.TYPE.toString()),
                            GraphUtils.createIri("https://purl.org/hmas/core/Workspace")
                        );
                        this.store.updateEntityGraph(platformIri, platformGraph);
                        this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                            platformIri.getIRIString(),
                            GraphUtils.graphToString(platformGraph, RDFSyntax.TURTLE)
                          )
                        );
                      } catch (final Exception e) {
                        LOGGER.error(e.getMessage());
                      }
                    });
              }
              this.store.createEntityGraph(entityIri, entityGraph);
              final var stringGraphResult = GraphUtils.graphToString(entityGraph, RDFSyntax.TURTLE);
              this.replyWithPayload(message, stringGraphResult);
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.getIRIString(),
                  stringGraphResult
                )
              );
            } catch (final Exception e) {
              LOGGER.error(e.getMessage());
              this.replyFailed(message);
            }
          },
          () -> this.replyFailed(message)
        );
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
            GraphUtils.stringToGraph(optEntityGraphString.get(), requestIri, RDFSyntax.TURTLE)
        );
        final var result = this.store.getEntityGraph(requestIri);
        if (result.isPresent() && result.get().size() > 0) {
          final var entityGraphString = GraphUtils.graphToString(result.get(), RDFSyntax.TURTLE);
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
      throws IllegalArgumentException {
    final var optEntityGraph = this.store.getEntityGraph(requestIri);
    if (optEntityGraph.isPresent() && optEntityGraph.get().size() > 0) {
      try (var entityGraph = optEntityGraph.get()) {
        final var entityGraphString = GraphUtils.graphToString(entityGraph, RDFSyntax.TURTLE);
        if (entityGraph.contains(
            requestIri,
            GraphUtils.createIri(RDF.TYPE.stringValue()),
            GraphUtils.createIri("https://purl.org/hmas/core/Artifact")
        )) {
          final var artifactIri = requestIri.getIRIString();
          final var workspaceIri =
              GraphUtils.createIri(artifactIri.substring(0, artifactIri.indexOf("/artifacts")));
          final var optWorkspaceGraph = this.store.getEntityGraph(workspaceIri);
          if (optWorkspaceGraph.isPresent()) {
            try (var workspaceGraph = optWorkspaceGraph.get()) {
              LOGGER.info("Found workspace graph: " + workspaceGraph);
              workspaceGraph.remove(
                  workspaceIri,
                  GraphUtils.createIri("https://purl.org/hmas/core/contains"),
                  requestIri
              );
              this.store.updateEntityGraph(workspaceIri, workspaceGraph);
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityChanged(
                  workspaceIri.getIRIString(),
                  GraphUtils.graphToString(workspaceGraph, RDFSyntax.TURTLE)
                )
              );
            } catch (final Exception e) {
              LOGGER.error(e.getMessage());
            }
          }
        }
        this.store.deleteEntityGraph(requestIri);
        this.replyWithPayload(message, entityGraphString);
        this.dispatcherMessagebox.sendMessage(new HttpNotificationDispatcherMessage.EntityDeleted(
            requestIri.getIRIString(),
            entityGraphString
        ));
      } catch (final Exception e) {
        LOGGER.error(e.getMessage());
      }
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
      if (!this.store.containsEntityGraph(GraphUtils.createIri(candidateIri))) {
        return candidateIri;
      }
    }
    // Generate a new IRI
    return Stream.generate(() -> UUID.randomUUID().toString())
                 .map(fullRequestIri::concat)
                 .dropWhile(i -> this.store.containsEntityGraph(GraphUtils.createIri(i)))
                 .findFirst()
                 .orElseThrow();
  }
}
