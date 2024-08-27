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
import java.util.regex.Pattern;
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
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RdfStoreVerticle.class);
  private static final String WORKSPACE_HMAS_IRI = "https://purl.org/hmas/Workspace";
  private static final String CONTAINS_HMAS_IRI = "https://purl.org/hmas/contains";
  private static final String ARTIFACT_FRAGMENT = "#artifact";
  private static final String WORKSPACE_FRAGMENT = "#workspace";
  private static final String PLATFORM_FRAGMENT = "#platform";
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private Messagebox<HttpNotificationDispatcherMessage> dispatcherMessagebox;
  private HttpInterfaceConfig httpConfig;
  private RdfStore store;

  private RepresentationFactory representationFactory;

  @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx.sharedData()
                                .<String, HttpInterfaceConfig>getLocalMap("http-config")
                                .get(DEFAULT_CONFIG_VALUE);

    final WebSubConfig notificationConfig = this.vertx.sharedData()
      .<String,WebSubConfig>getLocalMap("notification-config")
      .get(DEFAULT_CONFIG_VALUE);

    final EnvironmentConfig environmentConfig = this.vertx.sharedData()
                                .<String, EnvironmentConfig>getLocalMap("environment-config")
                                .get(DEFAULT_CONFIG_VALUE);
    this.representationFactory = RepresentationFactoryFactory.getRepresentationFactory(
      environmentConfig.getOntology(),
      notificationConfig,
      this.httpConfig
    );

    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(
      this.vertx.eventBus(),
      notificationConfig
    );
    final var ownMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    ownMessagebox.init();
    ownMessagebox.receiveMessages(message -> {
      try {
        switch (message.body()) {
          case RdfStoreMessage.GetEntityIri content ->
            message.reply(this.handleGetEntityIri(
              content.requestUri(),
              content.slug())
            );
          case RdfStoreMessage.GetEntity content ->
            this.handleGetEntity(
              RdfModelUtils.createIri(content.requestUri()),
              message
            );
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
          case RdfStoreMessage.ReplaceEntity content ->
            this.handleReplaceEntity(
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
      this.replyWithPayload(message, RdfModelUtils.modelToString(result.get(), RDFFormat.TURTLE,this.httpConfig.getBaseUri()));
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
            this.enrichArtifactGraphWithWorkspace(entityIri, entityModel, workspaceActualIri, true);
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
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri());
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
            this.enrichArtifactGraphWithWorkspace(entityIri, entityModel, workspaceIri, false);
            this.store.addEntityModel(entityIri, entityModel);
            final var stringGraphResult =
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri());
            this.dispatcherMessagebox.sendMessage(
              new HttpNotificationDispatcherMessage.EntityCreated(
                removeDuplicateSlashes(requestIri.toString()),
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
      final IRI workspaceIri,
      final boolean isBody
  ) throws IOException {
    final var artifactIRI = entityIri.stringValue().endsWith("/") ? RdfModelUtils.createIri(entityIri + ARTIFACT_FRAGMENT) : RdfModelUtils.createIri(entityIri + "/" + ARTIFACT_FRAGMENT);
    final var workspaceActualIRI = workspaceIri.stringValue().endsWith("/") ? RdfModelUtils.createIri(workspaceIri + WORKSPACE_FRAGMENT) : RdfModelUtils.createIri(workspaceIri + "/" + WORKSPACE_FRAGMENT);
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
          if (isBody) {
            workspaceModel.add(
              artifactIRI,
              RDF.TYPE,
              RdfModelUtils.createIri("https://purl.org/hmas/jacamo/Body")
            );
            workspaceModel.setNamespace("jacamo", "https://purl.org/hmas/jacamo/");
          }
          this.store.replaceEntityModel(workspaceIri, workspaceModel);
          this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.EntityChanged(
              workspaceIri.toString(),
              RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
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
    final var resourceIRI = RdfModelUtils.createIri(workspaceIri.endsWith("/") ? workspaceIri : workspaceIri + "/");

    final IRI workspaceIRI;
    if (workspaceIri.endsWith("/")) {
      workspaceIRI = RdfModelUtils.createIri(workspaceIri + WORKSPACE_FRAGMENT);
    } else {
      workspaceIRI = RdfModelUtils.createIri(workspaceIri + "/" + WORKSPACE_FRAGMENT);
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
              final var parentIri = RdfModelUtils.createIri(content.parentWorkspaceUri().get().endsWith("/") ? content.parentWorkspaceUri().get() : content.parentWorkspaceUri().get() + "/");
              entityModel.add(
                workspaceIRI,
                  RdfModelUtils.createIri("https://purl.org/hmas/isContainedIn"),
                  RdfModelUtils.createIri(parentIri + WORKSPACE_FRAGMENT)
              );
              entityModel.add(
                  RdfModelUtils.createIri(parentIri + WORKSPACE_FRAGMENT),
                  RDF.TYPE,
                  RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
              );
              this.store
                  .getEntityModel(parentIri)
                  .ifPresent(Failable.asConsumer(parentModel -> {
                    parentModel.add(
                        RdfModelUtils.createIri(parentIri + WORKSPACE_FRAGMENT),
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                      workspaceIRI
                    );
                    parentModel.add(
                      workspaceIRI,
                        RDF.TYPE,
                        RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                    );
                    this.store.replaceEntityModel(parentIri, parentModel);
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityChanged(
                        parentIri.toString(),
                        RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
                      )
                    );
                  }));
            } else {
              final var platformResourceProfileIri = RdfModelUtils.createIri(
                  workspaceIri.substring(0, workspaceIri.indexOf("workspaces"))
              );
              final var platformIRI = RdfModelUtils.createIri(platformResourceProfileIri + PLATFORM_FRAGMENT);
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
                        RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
                      )
                    );
                  }));
            }
            this.store.addEntityModel(resourceIRI, entityModel);
            final var stringGraphResult =
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri());
            if (requestIri.toString().endsWith("//")) {
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.toString().substring(0, requestIri.toString().length() - 1),
                  stringGraphResult
                )
              );
            } else {
              this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityCreated(
                  requestIri.toString(),
                  stringGraphResult
                )
              );
            }
            this.replyWithPayload(message, stringGraphResult);
          }),
          () -> this.replyFailed(message)
        );
  }

  // TODO: add message content validation
  private void handleReplaceEntity(
      final IRI requestIri,
      final RdfStoreMessage.ReplaceEntity content,
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

  private void handleUpdateEntity(
    final IRI requestIri,
    final RdfStoreMessage.UpdateEntity content,
    final Message<RdfStoreMessage> message
  ) throws IOException, IllegalArgumentException {
    this.store.getEntityModel(requestIri).ifPresentOrElse(
      Failable.asConsumer(m -> {
        final var additionalTriples = RdfModelUtils.stringToModel(
          content.entityRepresentation(),
          requestIri,
          RDFFormat.TURTLE
        );
        this.store.addEntityModel(requestIri, additionalTriples);
        final var updatedModel = RdfModelUtils.modelToString(this.store.getEntityModel(requestIri).orElseThrow(),
          RDFFormat.TURTLE,
          this.httpConfig.getBaseUri());
        this.dispatcherMessagebox.sendMessage(
          new HttpNotificationDispatcherMessage.EntityChanged(
            requestIri.toString(),
            updatedModel
          )
        );
        this.replyWithPayload(message, updatedModel);
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
                RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri());
            if (entityModel.contains(
                RdfModelUtils.createIri(requestIri + ARTIFACT_FRAGMENT),
                RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
            )) {
              final var artifactIri = requestIri.toString();
              final var workspaceIri =
                  RdfModelUtils.createIri(
                      Pattern
                        .compile(
                          "^(https?://.*?:[0-9]+/workspaces/.*?/)(?:artifacts|agents)/.*?$"
                        )
                        .matcher(artifactIri)
                        .results()
                        .map(r -> r.group(1))
                        .findFirst()
                        .orElseThrow()
                  );
              this.store
                  .getEntityModel(workspaceIri)
                  .ifPresent(Failable.asConsumer(workspaceModel -> {
                    workspaceModel.remove(
                        RdfModelUtils.createIri(workspaceIri + WORKSPACE_FRAGMENT),
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                        RdfModelUtils.createIri(requestIri + ARTIFACT_FRAGMENT)
                    );
                    workspaceModel.remove(
                      RdfModelUtils.createIri(requestIri + ARTIFACT_FRAGMENT),
                        RDF.TYPE,
                        RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
                    );
                    workspaceModel.remove(
                      RdfModelUtils.createIri(requestIri + ARTIFACT_FRAGMENT),
                      RDF.TYPE,
                      RdfModelUtils.createIri("https://purl.org/hmas/jacamo/Body")
                    );
                    this.store.replaceEntityModel(workspaceIri, workspaceModel);
                    this.dispatcherMessagebox.sendMessage(
                      new HttpNotificationDispatcherMessage.EntityChanged(
                        workspaceIri.toString(),
                        RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
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
                RdfModelUtils.createIri(requestIri + WORKSPACE_FRAGMENT),
                RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
            )) {
              final var workspaceIri = requestIri.toString();
              final var workspaceIriResource = RdfModelUtils.createIri(
                requestIri + WORKSPACE_FRAGMENT
              );
              final var platformIri = RdfModelUtils.createIri(
                  workspaceIri.substring(0, workspaceIri.indexOf("workspaces"))
              );
              final var platformIriResource = RdfModelUtils.createIri(
                this.httpConfig.getBaseUri() + PLATFORM_FRAGMENT
              );
              if (entityModel.contains(
                workspaceIriResource,
                  RdfModelUtils.createIri("https://purl.org/hmas/isHostedOn"),
                  platformIriResource
              )) {
                this.store
                    .getEntityModel(platformIri)
                    .ifPresent(Failable.asConsumer(platformModel -> {
                      platformModel.remove(
                          platformIriResource,
                          RdfModelUtils.createIri("https://purl.org/hmas/hosts"),
                        workspaceIriResource
                      );
                      platformModel.remove(
                        workspaceIriResource,
                          RDF.TYPE,
                          RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                      );
                      this.store.replaceEntityModel(platformIri, platformModel);
                      this.dispatcherMessagebox.sendMessage(
                        new HttpNotificationDispatcherMessage.EntityChanged(
                          platformIri.toString(),
                          RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
                        )
                      );
                    }));
              } else {
                entityModel
                    .filter(
                      workspaceIriResource,
                      RdfModelUtils.createIri("https://purl.org/hmas/isContainedIn"),
                      null
                    )
                    .objects()
                    .stream()
                    .map(o -> o instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(Failable.asConsumer(parentIri ->{
                      // strip fragments
                      final var parentIriDefragmented = parentIri.getNamespace().replace("#","");
                      this.store
                          .getEntityModel(RdfModelUtils.createIri(parentIriDefragmented))
                          .ifPresent(Failable.asConsumer(parentModel -> {
                            parentModel.remove(
                                parentIri,
                                RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                              workspaceIriResource
                            );
                            parentModel.remove(
                              workspaceIriResource,
                                RDF.TYPE,
                                RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
                            );
                            this.store.replaceEntityModel(RdfModelUtils.createIri(parentIriDefragmented), parentModel);
                            this.dispatcherMessagebox.sendMessage(
                              new HttpNotificationDispatcherMessage.EntityChanged(
                                parentIriDefragmented,
                                RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
                              )
                            );
                          }));
              }));
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
                  final var iriResource = RdfModelUtils.createIri(iri.toString().endsWith("/") ? iri + WORKSPACE_FRAGMENT : iri + "/" + WORKSPACE_FRAGMENT);
                  model
                      .filter(
                        iriResource,
                        RdfModelUtils.createIri(CONTAINS_HMAS_IRI),
                        null
                      )
                      .objects()
                      .stream()
                      .map(o -> o instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                      .flatMap(Optional::stream)
                      .map(fragmentedIri -> RdfModelUtils.createIri(fragmentedIri.getNamespace().replace("#","")))
                      .peek(irisToDelete::add)
                      .forEach(stack::add);
                  this.dispatcherMessagebox.sendMessage(
                    new HttpNotificationDispatcherMessage.EntityDeleted(
                      iri.toString(),
                      RdfModelUtils.modelToString(model, RDFFormat.TURTLE,this.httpConfig.getBaseUri())
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
    final String regexPattern = "(?<!:)//";

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
    final String regexPattern = "(?<!:)//";

    // Try to generate an IRI using the hint provided in the initial request
    if (optHint.isPresent()) {
      final var candidateIri = fullRequestIri.concat(optHint.get()).replaceAll(regexPattern, "/");

      if (!this.store.containsEntityModel(RdfModelUtils.createIri(candidateIri))) {
        return candidateIri;
      }
    }
    // Generate a new IRI
    final var IRI = Stream.generate(() -> UUID.randomUUID().toString()).map(fullRequestIri::concat)
      .dropWhile(Failable.asPredicate(
        i -> this.store.containsEntityModel(RdfModelUtils.createIri(i))
      )).findFirst();
    if (IRI.isPresent()) {
      return IRI.get().replaceAll(regexPattern, "/");
    }
    throw new IOException("Failed to generate a new IRI");

  }

  private String removeDuplicateSlashes(final String requestIri) {
    return requestIri.replaceAll("(?<!:)//", "/");
  }
}
