package org.hyperagents.yggdrasil.store;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RdfStoreVerticle.class);
  private static final String WORKSPACE_HMAS_IRI = "https://purl.org/hmas/core/Workspace";
  private static final String CONTAINS_HMAS_IRI = "https://purl.org/hmas/core/contains";

  private Messagebox<HttpNotificationDispatcherMessage> dispatcherMessagebox;
  private RdfStore store;

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @Override
  public void start(final Promise<Void> startPromise) {
    final var httpConfig = new HttpInterfaceConfigImpl(this.config());
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    final var ownMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      try {
        final var requestIri = RdfModelUtils.createIri(message.body().requestUri());
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
    this.vertx
        .<Void>executeBlocking(() -> {
          this.store = RdfStoreFactory.createStore(this.config().getJsonObject("rdf-store", null));
          final var platformIri = RdfModelUtils.createIri(httpConfig.getBaseUri() + "/");
          this.store.addEntityModel(
              platformIri,
              RdfModelUtils.stringToModel(
                new RepresentationFactoryImpl(httpConfig).createPlatformRepresentation(),
                platformIri,
                RDFFormat.TURTLE
              )
          );
          return null;
        })
        .onComplete(startPromise);
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.vertx
        .<Void>executeBlocking(() -> {
          this.store.close();
          return null;
        })
        .onComplete(stopPromise);
  }

  private void handleGetEntity(
      final IRI requestIri,
      final Message<RdfStoreMessage> message
  ) throws IOException {
    final var result = this.store.getEntityModel(requestIri);
    if (result.isPresent()) {
      this.replyWithPayload(message, RdfModelUtils.modelToString(result.get(), RDFFormat.TURTLE));
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
  ) {
    // Create IRI for new entity
    final var entityIriString =
        this.generateEntityIri(requestIri.toString(), content.artifactName());
    final var entityIri = RdfModelUtils.createIri(entityIriString);
    Optional
        .ofNullable(content.artifactRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + entityIriString + ">"))
        .ifPresentOrElse(
          s -> {
            try {
              final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);
              LOGGER.info("entity created is an artifact");
              final var artifactIri = entityIri.toString();
              final var workspaceIri =
                  RdfModelUtils.createIri(
                    artifactIri.substring(0, artifactIri.indexOf("/artifacts"))
                  );
              LOGGER.info("Found workspace IRI: " + workspaceIri);
              entityModel.add(
                  entityIri,
                  RdfModelUtils.createIri("https://purl.org/hmas/core/isContainedIn"),
                  workspaceIri
              );
              entityModel.add(
                  workspaceIri,
                  RdfModelUtils.createIri(RDF.TYPE.toString()),
                  RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
              );
              this.store
                  .getEntityModel(workspaceIri)
                  .ifPresent(workspaceModel -> {
                    try {
                      LOGGER.info("Found workspace graph: " + workspaceModel);
                      workspaceModel.add(
                          workspaceIri,
                          RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                          entityIri
                      );
                      workspaceModel.add(
                          entityIri,
                          RdfModelUtils.createIri(RDF.TYPE.toString()),
                          RdfModelUtils.createIri("https://purl.org/hmas/core/Artifact")
                      );
                      this.store.replaceEntityModel(workspaceIri, workspaceModel);
                      this.dispatcherMessagebox.sendMessage(
                        new HttpNotificationDispatcherMessage.EntityChanged(
                          workspaceIri.toString(),
                          RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE)
                        )
                      );
                    } catch (final Exception e) {
                      LOGGER.error(e);
                    }
                  });
              this.store.addEntityModel(entityIri, entityModel);
              final var stringGraphResult =
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.toString(),
                  stringGraphResult
                )
              );
              this.replyWithPayload(message, stringGraphResult);
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
        this.generateEntityIri(requestIri.toString(), content.workspaceName());
    final var entityIri = RdfModelUtils.createIri(entityIriString);
    Optional
        .ofNullable(content.workspaceRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + entityIriString + ">"))
        .ifPresentOrElse(
          s -> {
            try {
              final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);
              if (content.parentWorkspaceUri().isPresent()) {
                final var parentIri = RdfModelUtils.createIri(content.parentWorkspaceUri().get());
                entityModel.add(
                    entityIri,
                    RdfModelUtils.createIri("https://purl.org/hmas/core/isContainedIn"),
                    parentIri
                );
                entityModel.add(
                    parentIri,
                    RdfModelUtils.createIri(RDF.TYPE.toString()),
                    RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                );
                this.store
                    .getEntityModel(parentIri)
                    .ifPresent(parentModel -> {
                      try {
                        parentModel.add(
                            parentIri,
                            RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                            entityIri
                        );
                        parentModel.add(
                            entityIri,
                            RdfModelUtils.createIri(RDF.TYPE.toString()),
                            RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                        );
                        this.store.replaceEntityModel(parentIri, parentModel);
                        this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                            parentIri.toString(),
                            RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE)
                          )
                        );
                      } catch (final Exception e) {
                        LOGGER.error(e.getMessage());
                      }
                    });
              } else {
                final var workspaceIri = entityIri.toString();
                final var platformIri = RdfModelUtils.createIri(
                    workspaceIri.substring(0, workspaceIri.indexOf("workspaces"))
                );
                entityModel.add(
                    entityIri,
                    RdfModelUtils.createIri("https://purl.org/hmas/core/isHostedOn"),
                    platformIri
                );
                entityModel.add(
                    platformIri,
                    RdfModelUtils.createIri(RDF.TYPE.toString()),
                    RdfModelUtils.createIri("https://purl.org/hmas/core/HypermediaMASPlatform")
                );

                LOGGER.info("Found platform IRI: " + workspaceIri);

                this.store
                    .getEntityModel(platformIri)
                    .ifPresent(platformModel -> {
                      try {
                        LOGGER.info("Found platform graph: " + platformModel);
                        platformModel.add(
                            platformIri,
                            RdfModelUtils.createIri("https://purl.org/hmas/core/hosts"),
                            entityIri
                        );
                        platformModel.add(
                            entityIri,
                            RdfModelUtils.createIri(RDF.TYPE.toString()),
                            RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                        );
                        this.store.replaceEntityModel(platformIri, platformModel);
                        this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                            platformIri.toString(),
                            RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE)
                          )
                        );
                      } catch (final Exception e) {
                        LOGGER.error(e.getMessage());
                      }
                    });
              }
              this.store.addEntityModel(entityIri, entityModel);
              final var stringGraphResult =
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.toString(),
                  stringGraphResult
                )
              );
              this.replyWithPayload(message, stringGraphResult);
            } catch (final Exception e) {
              LOGGER.error(e.getMessage());
              this.replyFailed(message);
            }
          },
          () -> this.replyFailed(message)
        );
  }

  // TODO: add message content validation
  private void handleUpdateEntity(
      final IRI requestIri,
      final RdfStoreMessage.UpdateEntity content,
      final Message<RdfStoreMessage> message
  ) {
    this.store.getEntityModel(requestIri).ifPresentOrElse(
        m -> {
          try {
            final var replacingModel = RdfModelUtils.stringToModel(
                content.entityRepresentation(),
                requestIri,
                RDFFormat.TURTLE
            );
            this.store.replaceEntityModel(requestIri, replacingModel);

            LOGGER.info("Sending update notification for " + requestIri);

            this.dispatcherMessagebox.sendMessage(
              new HttpNotificationDispatcherMessage.EntityChanged(
                requestIri.toString(),
                content.entityRepresentation()
              )
            );
            this.replyWithPayload(message, content.entityRepresentation());
          } catch (final IOException e) {
            this.replyFailed(message);
          }
        },
        () -> this.replyEntityNotFound(message)
    );
  }

  private void handleDeleteEntity(final IRI requestIri, final Message<RdfStoreMessage> message)
      throws IllegalArgumentException {
    this.store
        .getEntityModel(requestIri)
        .ifPresentOrElse(
          entityModel -> {
            try {
              final var entityModelString =
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
              if (entityModel.contains(
                  requestIri,
                  RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                  RdfModelUtils.createIri("https://purl.org/hmas/core/Artifact")
              )) {
                final var artifactIri = requestIri.toString();
                final var workspaceIri =
                    RdfModelUtils.createIri(
                      artifactIri.substring(0, artifactIri.indexOf("/artifacts"))
                    );
                this.store
                    .getEntityModel(workspaceIri)
                    .ifPresent(workspaceModel -> {
                      try {
                        LOGGER.info("Found workspace graph: " + workspaceModel);
                        workspaceModel.remove(
                            workspaceIri,
                            RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                            requestIri
                        );
                        workspaceModel.remove(
                            requestIri,
                            RdfModelUtils.createIri(RDF.TYPE.toString()),
                            RdfModelUtils.createIri("https://purl.org/hmas/core/Artifact")
                        );
                        this.store.replaceEntityModel(workspaceIri, workspaceModel);
                        this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                            workspaceIri.toString(),
                            RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE)
                          )
                        );
                      } catch (final Exception e) {
                        LOGGER.error(e.getMessage());
                      }
                    });
                this.store.removeEntityModel(requestIri);
                this.dispatcherMessagebox.sendMessage(
                  new HttpNotificationDispatcherMessage.EntityDeleted(
                    requestIri.toString(),
                    entityModelString
                  )
                );
              } else if (entityModel.contains(
                  requestIri,
                  RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                  RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
              )) {
                final var workspaceIri = requestIri.toString();
                final var platformIri = RdfModelUtils.createIri(
                    workspaceIri.substring(0, workspaceIri.indexOf("workspaces"))
                );
                if (entityModel.contains(
                    requestIri,
                    RdfModelUtils.createIri("https://purl.org/hmas/core/isHostedOn"),
                    platformIri
                )) {
                  this.store
                      .getEntityModel(platformIri)
                      .ifPresent(platformModel -> {
                        try {
                          platformModel.remove(
                              platformIri,
                              RdfModelUtils.createIri("https://purl.org/hmas/core/hosts"),
                              requestIri
                          );
                          platformModel.remove(
                              requestIri,
                              RdfModelUtils.createIri(RDF.TYPE.toString()),
                              RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                          );
                          this.store.replaceEntityModel(platformIri, platformModel);
                          this.dispatcherMessagebox.sendMessage(
                            new HttpNotificationDispatcherMessage.EntityChanged(
                              platformIri.toString(),
                              RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE)
                            )
                          );
                        } catch (final Exception e) {
                          LOGGER.error(e);
                        }
                      });
                } else {
                  entityModel
                      .filter(
                        requestIri,
                        RdfModelUtils.createIri("https://purl.org/hmas/core/isContainedIn"),
                        null
                      )
                      .objects()
                      .stream()
                      .map(o -> o instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                      .flatMap(Optional::stream)
                      .findFirst()
                      .ifPresent(parentIri ->
                        this.store
                            .getEntityModel(parentIri)
                            .ifPresent(parentModel -> {
                              try {
                                parentModel.remove(
                                    parentIri,
                                    RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                                    requestIri
                                );
                                parentModel.remove(
                                    requestIri,
                                    RdfModelUtils.createIri(RDF.TYPE.toString()),
                                    RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                                );
                                this.store.replaceEntityModel(parentIri, parentModel);
                                this.dispatcherMessagebox.sendMessage(
                                  new HttpNotificationDispatcherMessage.EntityChanged(
                                    parentIri.toString(),
                                    RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE)
                                  )
                                );
                              } catch (final Exception e) {
                                LOGGER.error(e);
                              }
                            })
                      );
                }
                this.removeResourcesRecursively(requestIri);
              }
              this.replyWithPayload(message, entityModelString);
            } catch (final IOException e) {
              LOGGER.error(e.getMessage());
            }
          },
          () -> this.replyEntityNotFound(message)
        );
  }

  private void removeResourcesRecursively(final IRI workspaceIri) {
    final var stack = new LinkedList<>(List.of(workspaceIri));
    final var irisToDelete = new ArrayList<>(stack);
    while (!stack.isEmpty()) {
      final var iri = stack.removeLast();
      this.store.getEntityModel(iri)
                .ifPresent(model -> {
                  model
                      .filter(
                        iri,
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                        null
                      )
                      .objects()
                      .stream()
                      .map(o -> o instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                      .flatMap(Optional::stream)
                      .peek(irisToDelete::add)
                      .forEach(stack::add);
                  try {
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityDeleted(
                        iri.toString(),
                        RdfModelUtils.modelToString(model, RDFFormat.TURTLE)
                      )
                    );
                  } catch (final IOException e) {
                    LOGGER.error(e);
                  }
                });
    }
    irisToDelete.forEach(this.store::removeEntityModel);
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
      if (!this.store.containsEntityModel(RdfModelUtils.createIri(candidateIri))) {
        return candidateIri;
      }
    }
    // Generate a new IRI
    return Stream.generate(() -> UUID.randomUUID().toString())
                 .map(fullRequestIri::concat)
                 .dropWhile(i -> this.store.containsEntityModel(RdfModelUtils.createIri(i)))
                 .findFirst()
                 .orElseThrow();
  }
}
