package org.hyperagents.yggdrasil.store;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.function.Failable;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.model.Environment;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;
import org.hyperagents.yggdrasil.utils.*;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryHMASImpl;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticleHMAS extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RdfStoreVerticleHMAS.class);
  private static final String WORKSPACE_HMAS_IRI = "https://purl.org/hmas/Workspace";
  private static final String CONTAINS_HMAS_IRI = "https://purl.org/hmas/contains";
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private Messagebox<HttpNotificationDispatcherMessage> dispatcherMessagebox;
  private HttpInterfaceConfig httpConfig;
  private RdfStore store;

  private RepresentationFactoryHMASImpl representationFactory;

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx.sharedData()
                                .<String, HttpInterfaceConfig>getLocalMap("http-config")
                                .get("default");
    this.representationFactory = new RepresentationFactoryHMASImpl(this.httpConfig);
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(
      this.vertx.eventBus(),
      this.vertx.sharedData()
                .<String, WebSubConfig>getLocalMap("notification-config")
                .get(DEFAULT_CONFIG_VALUE)
    );
    final var ownMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      try {
        switch (message.body()) {
          case RdfStoreMessage.GetEntityIri(String requestUri, String slug) ->
            message.reply(this.handleGetEntityIri(requestUri, slug));
          case RdfStoreMessage.GetEntity(String requestUri) ->
            this.handleGetEntity(RdfModelUtils.createIri(requestUri), message);
          case RdfStoreMessage.CreateArtifact content ->
            this.handleCreateArtifact(
              RdfModelUtils.createIri(content.requestUri()),
              content,
              message
            );
          case RdfStoreMessage.CreateWorkspace content ->
            this.handleCreateWorkspace(
              RdfModelUtils.createIri(content.requestUri()),
              content,
              message
            );
          case RdfStoreMessage.UpdateEntity content ->
            this.handleUpdateEntity(
              RdfModelUtils.createIri(content.requestUri()),
              content,
              message
            );
          case RdfStoreMessage.DeleteEntity(String requestUri) ->
            this.handleDeleteEntity(RdfModelUtils.createIri(requestUri), message);
          case RdfStoreMessage.QueryKnowledgeGraph(
              String query,
              List<String> defaultGraphUris,
              List<String> namedGraphUris,
              String responseContentType
            ) ->
            this.handleQuery(query, defaultGraphUris, namedGraphUris, responseContentType, message);
          case RdfStoreMessage.CreateBody content -> this.handleCreateBody(content, message);
        }
      } catch (final IllegalArgumentException e) {
        LOGGER.error(e);
        this.replyBadRequest(message);
      } catch (final IOException | UncheckedIOException e) {
        LOGGER.error(e);
        this.replyFailed(message);
      }
    });
    this.vertx
        .<Void>executeBlocking(() -> {
          this.store =
            Optional.ofNullable(this.config())
                    .flatMap(c -> JsonObjectUtils.getBoolean(c, "in-memory", LOGGER::error))
                    .map(Failable.asFunction(inMemory -> {
                      if (inMemory) {
                        return RdfStoreFactory.createInMemoryStore();
                      } else {
                        return RdfStoreFactory.createFilesystemStore(
                          JsonObjectUtils
                              .getJsonObject(this.config(), "rdf-store", LOGGER::error)
                              .flatMap(c -> JsonObjectUtils.getString(
                                c,
                                "store-path",
                                LOGGER::error
                              ))
                              .orElse("data/")
                        );
                      }
                    }))
                    .orElse(RdfStoreFactory.createInMemoryStore());
          final var platformIri = RdfModelUtils.createIri(this.httpConfig.getBaseUri());
          this.store.addEntityModel(
              platformIri,
              RdfModelUtils.stringToModel(
                this.representationFactory.createPlatformRepresentation(),
                platformIri,
                RDFFormat.TURTLE
              )
          );
          if (
              !this.vertx
                   .sharedData()
                   .<String, EnvironmentConfig>getLocalMap("environment-config")
                   .get(DEFAULT_CONFIG_VALUE)
                   .isEnabled()
          ) {
            final var environment =
                this.vertx.sharedData()
                          .<String, Environment>getLocalMap("environment")
                          .get(DEFAULT_CONFIG_VALUE);
            environment.getWorkspaces()
                       .forEach(w -> w.getRepresentation().ifPresent(Failable.asConsumer(r -> {
                         ownMessagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
                             httpConfig.getWorkspacesUri(),
                             w.getName(),
                             w.getParentName().map(httpConfig::getWorkspaceUri),
                             Files.readString(r, StandardCharsets.UTF_8)
                         ));
                         w.getArtifacts().forEach(a -> a.getRepresentation().ifPresent(
                             Failable.asConsumer(ar ->
                               ownMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                                 httpConfig.getArtifactsUri(w.getName()),
                                 a.getName(),
                                 Files.readString(ar, StandardCharsets.UTF_8)
                               ))
                             )
                         ));
                       })));
          }
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
   * Creates a body artifact and adds it to the store.
   */
  private void handleCreateBody(
      final RdfStoreMessage.CreateBody content,
      final Message<RdfStoreMessage> message
  ) {
    final var bodyIri = this.httpConfig.getAgentBodyUri(
        content.workspaceName(),
        content.agentName()
    );
    final var entityIri = RdfModelUtils.createIri(bodyIri);
    final var entityBodyIri = RdfModelUtils.createIri(bodyIri + "#artifact");
    Optional
        .ofNullable(content.bodyRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + bodyIri + ">"))
        .ifPresentOrElse(
          Failable.asConsumer(s -> {
            final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);
            final var workspaceIri = this.httpConfig.getWorkspaceUri(content.workspaceName());
            final var workspaceActualIri = workspaceIri.endsWith("/") ? RdfModelUtils.createIri(workspaceIri.substring(0, workspaceIri.length() - 1)) : RdfModelUtils.createIri(workspaceIri);
            this.enrichArtifactGraphWithWorkspace(entityIri, entityModel, workspaceActualIri);
            final var agentIri =
                RdfModelUtils.createIri(content.agentID());
            entityModel.add(
                entityBodyIri,
                RdfModelUtils.createIri("https://purl.org/hmas/jacamo/isBodyOf"),
                agentIri
            );
            entityModel.add(
                agentIri,
                RDF.TYPE,
                RdfModelUtils.createIri("https://purl.org/hmas/Agent")
            );
            this.store.addEntityModel(entityIri, entityModel);
            final var stringGraphResult =
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
            this.dispatcherMessagebox.sendMessage(
              new HttpNotificationDispatcherMessage.EntityCreated(
                this.httpConfig.getAgentBodiesUri(content.workspaceName()),
                stringGraphResult
              )
            );
            this.replyWithPayload(message, stringGraphResult);
          }),
          () -> this.replyFailed(message)
        );
  }

  /**
   * Creates an artifact and adds it to the store.
   */
  private void handleCreateArtifact(
      final IRI requestIri,
      final RdfStoreMessage.CreateArtifact content,
      final Message<RdfStoreMessage> message
  ) throws IOException {
    // Create IRI for new entity
    final var artifactIri =
        this.generateEntityIri(requestIri.toString(), content.artifactName());
    final var entityIri = RdfModelUtils.createIri(artifactIri);
    Optional
        .ofNullable(content.artifactRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + artifactIri + ">"))
        .ifPresentOrElse(
          Failable.asConsumer(s -> {
            final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);

            final var workspaceIri = RdfModelUtils.createIri(
                artifactIri.substring(0, artifactIri.indexOf("/artifacts/"))
            );
            this.enrichArtifactGraphWithWorkspace(entityIri, entityModel, workspaceIri);
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
          }),
          () -> this.replyFailed(message)
        );
  }

  private void enrichArtifactGraphWithWorkspace(
      final IRI entityIri,
      final Model entityModel,
      final IRI workspaceIri
  ) throws IOException {
    final var artifactIRI = entityIri.stringValue().endsWith("/") ? RdfModelUtils.createIri(entityIri + "#artifact") : RdfModelUtils.createIri(entityIri + "/#artifact");
    final var workspaceActualIRI = workspaceIri.stringValue().endsWith("/") ? RdfModelUtils.createIri(workspaceIri + "#workspace") : RdfModelUtils.createIri(workspaceIri + "/#workspace");
    entityModel.add(
        artifactIRI,
        RdfModelUtils.createIri("https://purl.org/hmas/isContainedIn"),
      workspaceActualIRI
    );
    entityModel.add(
      workspaceActualIRI,
        RDF.TYPE,
        RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
    );
    this.store
        .getEntityModel(workspaceIri)
        .ifPresent(Failable.asConsumer(workspaceModel -> {
          workspaceModel.add(
              workspaceActualIRI,
              RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
              artifactIRI
          );
          workspaceModel.add(
              artifactIRI,
              RDF.TYPE,
              RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
          );
          this.store.replaceEntityModel(workspaceIri, workspaceModel);
          this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.EntityChanged(
              workspaceIri.toString(),
              RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE)
            )
          );
        }));
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
  ) throws IllegalArgumentException, IOException {
    // Create IRI for new entity
    final var workspaceIri =
        this.generateEntityIri(requestIri.toString(), content.workspaceName());
    final var resourceIRI = RdfModelUtils.createIri(workspaceIri);

    IRI workspaceIRI;
    if (workspaceIri.endsWith("/")) {
      workspaceIRI = RdfModelUtils.createIri(workspaceIri + "#workspace");
    } else {
      workspaceIRI = RdfModelUtils.createIri(workspaceIri + "/#workspace");
    }

    Optional
        .ofNullable(content.workspaceRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + workspaceIri + ">"))
        .ifPresentOrElse(
          Failable.asConsumer(s -> {
            final var entityModel = RdfModelUtils.stringToModel(s, resourceIRI, RDFFormat.TURTLE);
            if (content.parentWorkspaceUri().isPresent()) {
              final var parentIri = RdfModelUtils.createIri(content.parentWorkspaceUri().get());
              entityModel.add(
                resourceIRI,
                  RdfModelUtils.createIri("https://purl.org/hmas/isContainedIn"),
                  parentIri
              );
              entityModel.add(
                  parentIri,
                  RDF.TYPE,
                  RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
              );
              this.store
                  .getEntityModel(parentIri)
                  .ifPresent(Failable.asConsumer(parentModel -> {
                    parentModel.add(
                        parentIri,
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                      resourceIRI
                    );
                    parentModel.add(
                      resourceIRI,
                        RDF.TYPE,
                        RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                    );
                    this.store.replaceEntityModel(parentIri, parentModel);
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityChanged(
                        parentIri.toString(),
                        RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE)
                      )
                    );
                  }));
            } else {
              final var platformResourceProfileIri = RdfModelUtils.createIri(
                  workspaceIri.substring(0, workspaceIri.indexOf("workspaces"))
              );
              final var platformIRI = RdfModelUtils.createIri(platformResourceProfileIri + "#platform");
              entityModel.add(
                workspaceIRI,
                  RdfModelUtils.createIri("https://purl.org/hmas/isHostedOn"),
                platformIRI
              );
              entityModel.add(
                platformIRI,
                  RDF.TYPE,
                  RdfModelUtils.createIri("https://purl.org/hmas/HypermediaMASPlatform")
              );
              this.store
                  .getEntityModel(platformResourceProfileIri)
                  .ifPresent(Failable.asConsumer(platformModel -> {
                    platformModel.add(
                      platformIRI,
                        RdfModelUtils.createIri("https://purl.org/hmas/hosts"),
                      workspaceIRI
                    );
                    platformModel.add(
                      workspaceIRI,
                        RDF.TYPE,
                        RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                    );
                    this.store.replaceEntityModel(platformResourceProfileIri, platformModel);
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityChanged(
                        platformResourceProfileIri.toString(),
                        RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE)
                      )
                    );
                  }));
            }
            this.store.addEntityModel(resourceIRI, entityModel);
            final var stringGraphResult =
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
            this.dispatcherMessagebox.sendMessage(
              new HttpNotificationDispatcherMessage.EntityCreated(
                requestIri.toString(),
                stringGraphResult
              )
            );
            this.replyWithPayload(message, stringGraphResult);
          }),
          () -> this.replyFailed(message)
        );
  }

  // TODO: add message content validation
  private void handleUpdateEntity(
      final IRI requestIri,
      final RdfStoreMessage.UpdateEntity content,
      final Message<RdfStoreMessage> message
  ) throws IOException {
    this.store.getEntityModel(requestIri).ifPresentOrElse(
        Failable.asConsumer(m -> {
          final var replacingModel = RdfModelUtils.stringToModel(
              content.entityRepresentation(),
              requestIri,
              RDFFormat.TURTLE
          );
          this.store.replaceEntityModel(requestIri, replacingModel);
          this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.EntityChanged(
              requestIri.toString(),
              content.entityRepresentation()
            )
          );
          this.replyWithPayload(message, content.entityRepresentation());
        }),
        () -> this.replyEntityNotFound(message)
    );
  }

  private void handleDeleteEntity(final IRI requestIri, final Message<RdfStoreMessage> message)
      throws IllegalArgumentException, IOException {
    this.store
        .getEntityModel(requestIri)
        .ifPresentOrElse(
          Failable.asConsumer(entityModel -> {
            final var entityModelString =
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE);
            if (entityModel.contains(
                requestIri,
                RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
            )) {
              final var artifactIri = requestIri.toString();
              final var workspaceIri =
                  RdfModelUtils.createIri(
                    artifactIri.substring(0, artifactIri.indexOf("/artifacts"))
                  );
              this.store
                  .getEntityModel(workspaceIri)
                  .ifPresent(Failable.asConsumer(workspaceModel -> {
                    workspaceModel.remove(
                        workspaceIri,
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                        requestIri
                    );
                    workspaceModel.remove(
                        requestIri,
                        RDF.TYPE,
                        RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
                    );
                    this.store.replaceEntityModel(workspaceIri, workspaceModel);
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityChanged(
                        workspaceIri.toString(),
                        RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE)
                      )
                    );
                  }));
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
                  RdfModelUtils.createIri("https://purl.org/hmas/isHostedOn"),
                  platformIri
              )) {
                this.store
                    .getEntityModel(platformIri)
                    .ifPresent(Failable.asConsumer(platformModel -> {
                      platformModel.remove(
                          platformIri,
                          RdfModelUtils.createIri("https://purl.org/hmas/hosts"),
                          requestIri
                      );
                      platformModel.remove(
                          requestIri,
                          RDF.TYPE,
                          RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                      );
                      this.store.replaceEntityModel(platformIri, platformModel);
                      this.dispatcherMessagebox.sendMessage(
                        new HttpNotificationDispatcherMessage.EntityChanged(
                          platformIri.toString(),
                          RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE)
                        )
                      );
                    }));
              } else {
                entityModel
                    .filter(
                      requestIri,
                      RdfModelUtils.createIri("https://purl.org/hmas/isContainedIn"),
                      null
                    )
                    .objects()
                    .stream()
                    .map(o -> o instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(Failable.asConsumer(parentIri ->
                      this.store
                          .getEntityModel(parentIri)
                          .ifPresent(Failable.asConsumer(parentModel -> {
                            parentModel.remove(
                                parentIri,
                                RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                                requestIri
                            );
                            parentModel.remove(
                                requestIri,
                                RDF.TYPE,
                                RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                            );
                            this.store.replaceEntityModel(parentIri, parentModel);
                            this.dispatcherMessagebox.sendMessage(
                              new HttpNotificationDispatcherMessage.EntityChanged(
                                parentIri.toString(),
                                RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE)
                              )
                            );
                          }))
                    ));
              }
              this.removeResourcesRecursively(requestIri);
            }
            this.replyWithPayload(message, entityModelString);
          }),
          () -> this.replyEntityNotFound(message)
        );
  }

  private void removeResourcesRecursively(final IRI workspaceIri) throws IOException {
    final var stack = new LinkedList<>(List.of(workspaceIri));
    final var irisToDelete = new ArrayList<>(stack);
    while (!stack.isEmpty()) {
      final var iri = stack.removeLast();
      this.store.getEntityModel(iri)
                .ifPresent(Failable.asConsumer(model -> {
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
                  this.dispatcherMessagebox.sendMessage(
                    new HttpNotificationDispatcherMessage.EntityDeleted(
                      iri.toString(),
                      RdfModelUtils.modelToString(model, RDFFormat.TURTLE)
                    )
                  );
                }));
    }
    irisToDelete.forEach(Failable.asConsumer(this.store::removeEntityModel));
  }

  private void handleQuery(
      final String query,
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris,
      final String responseContentType,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException, IOException {
    this.replyWithPayload(
        message,
        this.store.queryGraph(query, defaultGraphUris, namedGraphUris, responseContentType)
    );
  }

  private void replyWithPayload(final Message<RdfStoreMessage> message, final String payload) {
    message.reply(payload);
  }

  private void replyFailed(final Message<RdfStoreMessage> message) {
    message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Store request failed.");
  }

  private void replyBadRequest(final Message<RdfStoreMessage> message) {
    message.fail(HttpStatus.SC_BAD_REQUEST, "Arguments badly formatted.");
  }

  private void replyEntityNotFound(final Message<RdfStoreMessage> message) {
    message.fail(HttpStatus.SC_NOT_FOUND, "Entity not found.");
  }


  private String handleGetEntityIri(final String requestIri, final String hint) throws IOException {
    final var fullRequestIri = !requestIri.endsWith("/") ? requestIri.concat("/") : requestIri;
    final var optHint = Optional.ofNullable(hint).filter(s -> !s.isEmpty());
    String regexPattern = "(?<!:)//";

    // Try to generate an IRI using the hint provided in the initial request
    if (optHint.isPresent()) {
      final var candidateIri = fullRequestIri.concat(optHint.get()).replaceAll(regexPattern, "/");

      if (!this.store.containsEntityModel(RdfModelUtils.createIri(candidateIri))) {
        return hint;
      }
    }
    return UUID.randomUUID().toString();
  }

  private String generateEntityIri(final String requestIri, final String hint) throws IOException {
    final var fullRequestIri = !requestIri.endsWith("/") ? requestIri.concat("/") : requestIri;
    final var optHint = Optional.ofNullable(hint).filter(s -> !s.isEmpty());
    String regexPattern = "(?<!:)//";

    // Try to generate an IRI using the hint provided in the initial request
    if (optHint.isPresent()) {
      final var candidateIri = fullRequestIri.concat(optHint.get()).replaceAll(regexPattern, "/");

      if (!this.store.containsEntityModel(RdfModelUtils.createIri(candidateIri))) {
        return candidateIri;
      }
    }
    // Generate a new IRI
    var IRI = Stream.generate(() -> UUID.randomUUID().toString()).map(fullRequestIri::concat)
      .dropWhile(Failable.asPredicate(
        i -> this.store.containsEntityModel(RdfModelUtils.createIri(i))
      )).findFirst();
    if (IRI.isPresent()) {
      return IRI.get().replaceAll(regexPattern, "/");
    }
    throw new IOException("Failed to generate a new IRI");

  }
}
