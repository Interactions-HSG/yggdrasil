package org.hyperagents.yggdrasil.store;

import static org.eclipse.rdf4j.model.util.Values.iri;

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
import org.apache.commons.lang3.function.Failable;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.store.impl.RdfStoreFactory;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;

/**
 * Stores the RDF graphs representing the instantiated artifacts.
 */
public class RdfStoreVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RdfStoreVerticle.class);
  private static final String WORKSPACE_HMAS_IRI = "https://purl.org/hmas/Workspace";
  private static final String CONTAINS_HMAS_IRI = "https://purl.org/hmas/contains";
  private static final String CONTAINED_IN_HMAS_IRI = "https://purl.org/hmas/isContainedIn";
  private static final String PLATFORM_HMAS_IRI = "https://purl.org/hmas/HypermediaMASPlatform";
  private static final String ARTIFACT_HMAS_IRI = "https://purl.org/hmas/Artifact";
  private static final String HOSTS_HMAS_IRI = "https://purl.org/hmas/hosts";
  private static final String HOSTED_ON_HMAS_IRI = "https://purl.org/hmas/isHostedOn";
  private static final String HMAS_IRI = "https://purl.org/hmas/";
  private static final String HMAS = "hmas";
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
        .<String, WebSubConfig>getLocalMap("notification-config")
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
          case RdfStoreMessage.GetEntityIri content -> message.reply(this.handleGetEntityIri(
              content.requestUri(),
              content.slug())
          );
          case RdfStoreMessage.GetEntity content -> this.handleGetEntity(
              RdfModelUtils.createIri(content.requestUri()),
              message
          );
          case RdfStoreMessage.CreateArtifact content -> this.handleCreateArtifact(
              content,
              message
          );
          case RdfStoreMessage.CreateWorkspace content -> this.handleCreateWorkspace(
              content,
              message
          );
          case RdfStoreMessage.ReplaceEntity content -> this.handleReplaceEntity(
              RdfModelUtils.createIri(content.requestUri()),
              content,
              message
          );
          case RdfStoreMessage.UpdateEntity content -> this.handleUpdateEntity(
              RdfModelUtils.createIri(content.requestUri()),
              content,
              message
          );
          case RdfStoreMessage.DeleteEntity(String workspaceName, String artifactName) ->
              this.handleDeleteEntity(workspaceName, artifactName, message);
          case RdfStoreMessage.GetWorkspaces(String containerWorkspace) ->
              this.handleGetWorkspaces(containerWorkspace, message);
          case RdfStoreMessage.GetArtifacts(String workspaceName) ->
              this.handleGetArtifacts(workspaceName, message);
          case RdfStoreMessage.QueryKnowledgeGraph(
              String query,
              List<String> defaultGraphUris,
              List<String> namedGraphUris,
              String responseContentType
            ) -> this.handleQuery(query, defaultGraphUris, namedGraphUris, responseContentType,
              message);
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
          final var platformIri =
              RdfModelUtils.createIri(this.httpConfig.getBaseUriTrailingSlash());
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
                      httpConfig.getWorkspacesUriTrailingSlash(),
                      w.getName(),
                      w.getParentName().map(httpConfig::getWorkspaceUri),
                      Files.readString(r, StandardCharsets.UTF_8)
                  ));
                  w.getArtifacts().forEach(a -> a.getRepresentation().ifPresent(
                      Failable.asConsumer(ar ->
                          ownMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                              httpConfig.getArtifactsUriTrailingSlash(w.getName()),
                              w.getName(),
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
      this.replyWithPayload(message, RdfModelUtils.modelToString(result.get(), RDFFormat.TURTLE,
          this.httpConfig.getBaseUriTrailingSlash()));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private void handleGetWorkspaces(
      final String containerWorkspaceUri,
      final Message<RdfStoreMessage> message
  ) throws IOException {
    // either parentWorkspace or the Platform
    final var result = this.store.getEntityModel(iri(containerWorkspaceUri));
    if (result.isPresent()) {
      final var containerFragment =
          containerWorkspaceUri.equals(this.httpConfig.getBaseUriTrailingSlash())
              ? PLATFORM_FRAGMENT : WORKSPACE_FRAGMENT;

      final Model m = handleGetContainedThings(
          result.get(),
          containerWorkspaceUri + containerFragment,
          containerFragment.equals(PLATFORM_FRAGMENT) ? PLATFORM_HMAS_IRI : WORKSPACE_HMAS_IRI,
          containerFragment.equals(PLATFORM_FRAGMENT) ? HOSTS_HMAS_IRI : CONTAINS_HMAS_IRI,
          WORKSPACE_HMAS_IRI,
          WORKSPACE_FRAGMENT
      );

      this.replyWithPayload(message, RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
          this.httpConfig.getBaseUriTrailingSlash()));
    } else {
      this.replyEntityNotFound(message);
    }
  }

  private Model handleGetContainedThings(final Model container,
                                         final String containerIri,
                                         final String containerType,
                                         final String containmentAction,
                                         final String containedIri,
                                         final String containedFragment) {
    final Model m = new LinkedHashModel();

    final var containerTriple = container.filter(
        iri(containerIri), RDF.TYPE, iri(containerType)
    );

    final var relevantEntities = container.filter(
        null, RDF.TYPE, iri(containedIri)
    );

    final var allContainedEntities = container.filter(
        iri(containerIri), iri(containmentAction), null
    );

    allContainedEntities.removeIf(
        triple -> !triple.getObject().stringValue().contains(containedFragment)
    );

    m.addAll(containerTriple);
    m.addAll(allContainedEntities);
    m.addAll(relevantEntities);
    m.setNamespace(HMAS, HMAS_IRI);

    return m;
  }

  private void handleGetArtifacts(final String workspaceName,
                                  final Message<RdfStoreMessage> message) throws IOException {
    final var workspaceIri = this.httpConfig.getWorkspaceUri(workspaceName);
    final var result = this.store.getEntityModel(iri(workspaceIri));
    if (result.isPresent()) {
      final Model m = handleGetContainedThings(
          result.get(),
          workspaceIri + WORKSPACE_FRAGMENT,
          WORKSPACE_HMAS_IRI,
          CONTAINS_HMAS_IRI,
          ARTIFACT_HMAS_IRI,
          ARTIFACT_FRAGMENT
      );
      this.replyWithPayload(message, RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
          this.httpConfig.getBaseUriTrailingSlash()));
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
  ) throws IOException {
    final var bodyIri = this.httpConfig.getAgentBodyUri(
        content.workspaceName(),
        content.agentName()
    );

    final var entityIri = RdfModelUtils.createIri(bodyIri);

    if (this.store.containsEntityModel(entityIri)) {
      this.replyWithPayload(message,
          RdfModelUtils.modelToString(
              this.store.getEntityModel(entityIri).orElseThrow(),
              RDFFormat.TURTLE,
              this.httpConfig.getBaseUriTrailingSlash()));
      return;
    }

    final var entityBodyIri = RdfModelUtils.createIri(bodyIri + "#artifact");
    Optional
        .ofNullable(content.bodyRepresentation())
        .filter(s -> !s.isEmpty())
        // Replace all null relative IRIs with the IRI generated for this entity
        .map(s -> s.replaceAll("<>", "<" + bodyIri + ">"))
        .ifPresentOrElse(
            Failable.asConsumer(s -> {
              final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);
              final var workspaceActualIri = RdfModelUtils.createIri(
                  this.httpConfig.getWorkspaceUri(content.workspaceName())
              );
              this.enrichArtifactGraphWithWorkspace(
                  entityIri, entityModel, workspaceActualIri, true);
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
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,
                      this.httpConfig.getBaseUriTrailingSlash());
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
      final RdfStoreMessage.CreateArtifact content,
      final Message<RdfStoreMessage> message
  ) throws IOException {
    // Create IRI for new entity
    final var artifactIri = this.httpConfig
        .getArtifactUri(content.workspaceName(), content.artifactName());

    final var entityIri = RdfModelUtils.createIri(artifactIri);

    final var workspaceIri = RdfModelUtils.createIri(
        artifactIri.substring(0, artifactIri.indexOf("/artifacts/"))
    );

    final var t = this.store.containsEntityModel(workspaceIri);
    if (!t) {
      this.replyFailed(message);
    } else {
      Optional
          .ofNullable(content.artifactRepresentation())
          .filter(s -> !s.isEmpty())
          // Replace all null relative IRIs with the IRI generated for this entity
          .map(s -> s.replaceAll("<>", "<" + artifactIri + ">"))
          .ifPresentOrElse(
              Failable.asConsumer(s -> {
                final var entityModel = RdfModelUtils.stringToModel(s, entityIri, RDFFormat.TURTLE);
                this.enrichArtifactGraphWithWorkspace(entityIri, entityModel, workspaceIri, false);
                this.store.addEntityModel(entityIri, entityModel);
                final var stringGraphResult =
                    RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,
                        this.httpConfig.getBaseUriTrailingSlash());
                this.replyWithPayload(message, stringGraphResult);
              }),
              () -> this.replyFailed(message)
          );
    }
  }

  private void enrichArtifactGraphWithWorkspace(
      final IRI entityIri,
      final Model entityModel,
      final IRI workspaceIri,
      final boolean isBody
  ) throws IOException {
    final var artifactIRI =
        RdfModelUtils.createIri(entityIri + ARTIFACT_FRAGMENT);

    final var workspaceActualIRI =
        RdfModelUtils.createIri(workspaceIri + WORKSPACE_FRAGMENT);

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
              RdfModelUtils.createIri(ARTIFACT_HMAS_IRI)
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
                  RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE,
                      this.httpConfig.getBaseUriTrailingSlash())
              )
          );

          final Model m = new LinkedHashModel();

          final var artifactsContained = workspaceModel
              .filter(null, RDF.TYPE, iri(ARTIFACT_HMAS_IRI));

          final var workspaceDefTriple = workspaceModel
              .filter(workspaceActualIRI, RDF.TYPE, iri(WORKSPACE_HMAS_IRI));


          final var containedThings = workspaceModel
              .filter(null, iri(CONTAINS_HMAS_IRI), null);

          containedThings.removeIf(
              triple -> !triple.getObject().stringValue().contains(ARTIFACT_FRAGMENT)
          );

          m.addAll(containedThings);
          m.addAll(workspaceDefTriple);
          m.addAll(artifactsContained);
          m.setNamespace(HMAS, HMAS_IRI);


          this.dispatcherMessagebox.sendMessage(
              new HttpNotificationDispatcherMessage.EntityChanged(
                  workspaceIri + "/artifacts/",
                  RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
                      this.httpConfig.getBaseUriTrailingSlash())
              )
          );
        }));
  }

  /**
   * Creates an entity and adds it to the store.
   *
   * @param message    Request
   */
  private void handleCreateWorkspace(
      final RdfStoreMessage.CreateWorkspace content,
      final Message<RdfStoreMessage> message
  ) throws IllegalArgumentException, IOException {
    // Create IRI for new entity
    final var workspaceIri = this.httpConfig.getWorkspaceUri(content.workspaceName());
    final var resourceIRI = RdfModelUtils.createIri(workspaceIri);

    final var workspaceIRI = RdfModelUtils.createIri(workspaceIri + WORKSPACE_FRAGMENT);
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
                    workspaceIRI,
                    RdfModelUtils.createIri(CONTAINED_IN_HMAS_IRI),
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
                              RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE,
                                  this.httpConfig.getBaseUriTrailingSlash())
                          )
                      );

                      final var parentWorkspaceName = parentIri.toString()
                          .substring(parentIri.toString().lastIndexOf('/') + 1);

                      final Model m = new LinkedHashModel();

                      final var workspaceDefTriple = parentModel
                          .filter(
                              RdfModelUtils.createIri(parentIri + WORKSPACE_FRAGMENT),
                              RDF.TYPE, iri(WORKSPACE_HMAS_IRI));

                      final Model containedThings = parentModel
                          .filter(null, iri(CONTAINS_HMAS_IRI), null);


                      containedThings.removeIf(
                          triple -> !triple.getObject().stringValue().contains(WORKSPACE_FRAGMENT)
                      );

                      final Model containedDefinitions = parentModel
                          .filter(null, null, iri(WORKSPACE_HMAS_IRI));

                      m.addAll(containedThings);
                      m.addAll(workspaceDefTriple);
                      m.addAll(containedDefinitions);

                      m.setNamespace(HMAS, HMAS_IRI);


                      this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                              this.httpConfig.getWorkspacesUri()
                                  + "?parent=" + parentWorkspaceName,
                              RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
                                  this.httpConfig.getBaseUriTrailingSlash())
                          )
                      );
                    }));
              } else {
                final var platformResourceProfileIri =
                    iri(this.httpConfig.getBaseUriTrailingSlash());

                final var platformIRI =
                    RdfModelUtils.createIri(platformResourceProfileIri + PLATFORM_FRAGMENT);
                entityModel.add(
                    workspaceIRI,
                    RdfModelUtils.createIri(HOSTED_ON_HMAS_IRI),
                    platformIRI
                );
                entityModel.add(
                    platformIRI,
                    RDF.TYPE,
                    RdfModelUtils.createIri(PLATFORM_HMAS_IRI)
                );
                this.store
                    .getEntityModel(platformResourceProfileIri)
                    .ifPresent(Failable.asConsumer(platformModel -> {
                      platformModel.add(
                          platformIRI,
                          RdfModelUtils.createIri(HOSTS_HMAS_IRI),
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
                              RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE,
                                  this.httpConfig.getBaseUriTrailingSlash())
                          )
                      );

                      final Model m = new LinkedHashModel();

                      final var workspaceDefTriple = platformModel
                          .filter(RdfModelUtils.createIri(
                                  platformResourceProfileIri + PLATFORM_FRAGMENT),
                              RDF.TYPE, iri(PLATFORM_HMAS_IRI));

                      final Model containedThings = platformModel
                          .filter(null, iri(HOSTS_HMAS_IRI), null);

                      final Model workspaceDef = platformModel
                          .filter(null, null, iri(WORKSPACE_HMAS_IRI));

                      m.addAll(workspaceDefTriple);
                      m.addAll(containedThings);
                      m.addAll(workspaceDef);

                      m.setNamespace(HMAS, HMAS_IRI);


                      this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                              this.httpConfig.getWorkspacesUriTrailingSlash(),
                              RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
                                  this.httpConfig.getBaseUriTrailingSlash())
                          )
                      );
                    }));
              }
              this.store.addEntityModel(resourceIRI, entityModel);
              final var stringGraphResult =
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,
                      this.httpConfig.getBaseUriTrailingSlash());
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
          final var updatedModel =
              RdfModelUtils.modelToString(this.store.getEntityModel(requestIri).orElseThrow(),
                  RDFFormat.TURTLE,
                  this.httpConfig.getBaseUriTrailingSlash());
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

  private void handleDeleteEntity(final String workspaceName, final String artifactName,
                                  final Message<RdfStoreMessage> message)
      throws IllegalArgumentException, IOException {

    final var workspaceIri = iri(this.httpConfig.getWorkspaceUri(workspaceName));
    final var requestIri = artifactName == null ? workspaceIri :
        iri(this.httpConfig.getArtifactUri(workspaceName, artifactName));

    this.store
        .getEntityModel(requestIri)
        .ifPresentOrElse(
            Failable.asConsumer(entityModel -> {
              final var entityModelString =
                  RdfModelUtils.modelToString(entityModel, RDFFormat.TURTLE,
                      this.httpConfig.getBaseUriTrailingSlash());
              if (entityModel.contains(
                  RdfModelUtils.createIri(requestIri + ARTIFACT_FRAGMENT),
                  RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                  RdfModelUtils.createIri("https://purl.org/hmas/Artifact")
              )) {
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
                      final var workspaceIriWIthoutTrailingSlash = workspaceIri.toString()
                          .endsWith("/")
                          ?
                          workspaceIri.toString().substring(0, workspaceIri.toString().length() - 1)
                          : workspaceIri.toString();
                      this.dispatcherMessagebox.sendMessage(
                          new HttpNotificationDispatcherMessage.EntityChanged(
                              workspaceIriWIthoutTrailingSlash,
                              RdfModelUtils.modelToString(workspaceModel, RDFFormat.TURTLE,
                                  this.httpConfig.getBaseUriTrailingSlash())
                          )
                      );
                    }));
                this.store.removeEntityModel(requestIri);
                final var requestIriWithoutTrailingSlash = requestIri.toString().endsWith("/")
                    ? requestIri.toString().substring(0, requestIri.toString().length() - 1)
                    : requestIri.toString();
                this.dispatcherMessagebox.sendMessage(
                    new HttpNotificationDispatcherMessage.EntityDeleted(
                        requestIriWithoutTrailingSlash,
                        entityModelString
                    )
                );
              } else if (entityModel.contains(
                  RdfModelUtils.createIri(requestIri + WORKSPACE_FRAGMENT),
                  RdfModelUtils.createIri(RDF.TYPE.stringValue()),
                  RdfModelUtils.createIri(WORKSPACE_HMAS_IRI)
              )) {
                final var workspaceIriResource = RdfModelUtils.createIri(
                    requestIri + WORKSPACE_FRAGMENT
                );
                final var platformIri = iri(this.httpConfig.getBaseUriTrailingSlash());
                final var platformIriResource = iri(
                    this.httpConfig.getBaseUriTrailingSlash() + PLATFORM_FRAGMENT
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
                            RdfModelUtils.createIri(HOSTS_HMAS_IRI),
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
                                RdfModelUtils.modelToString(platformModel, RDFFormat.TURTLE,
                                    this.httpConfig.getBaseUriTrailingSlash())
                            )
                        );

                        final Model m = new LinkedHashModel();

                        final var workspaceDefTriple = platformModel
                            .filter(platformIriResource, RDF.TYPE,
                                iri(PLATFORM_HMAS_IRI));

                        final Model containedThings = platformModel
                            .filter(null, iri(HOSTS_HMAS_IRI), null);

                        final Model workspaceDef = platformModel
                            .filter(null, null, iri(WORKSPACE_HMAS_IRI));

                        m.addAll(workspaceDefTriple);
                        m.addAll(containedThings);
                        m.addAll(workspaceDef);

                        m.setNamespace(HMAS, HMAS_IRI);

                        this.dispatcherMessagebox.sendMessage(
                            new HttpNotificationDispatcherMessage.EntityChanged(
                                this.httpConfig.getWorkspacesUriTrailingSlash(),
                                RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
                                    this.httpConfig.getBaseUriTrailingSlash())
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
                      .ifPresent(Failable.asConsumer(parentIri -> {
                        // strip fragments
                        final var parentIriDefragmented =
                            iri(parentIri.getNamespace().replace("#", ""));
                        this.store
                            .getEntityModel(parentIriDefragmented)
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
                              this.store.replaceEntityModel(
                                  parentIriDefragmented, parentModel);
                              this.dispatcherMessagebox.sendMessage(
                                  new HttpNotificationDispatcherMessage.EntityChanged(
                                      parentIriDefragmented.toString(),
                                      RdfModelUtils.modelToString(parentModel, RDFFormat.TURTLE,
                                          this.httpConfig.getBaseUriTrailingSlash())
                                  )
                              );

                              final Model m = new LinkedHashModel();

                              final var workspaceDefTriple = parentModel
                                  .filter(parentIriDefragmented, RDF.TYPE, iri(WORKSPACE_HMAS_IRI));


                              final Model containedThings = parentModel
                                  .filter(null, iri("https://purl.org/hmas/contains"), null);

                              containedThings.removeIf(
                                  triple -> !triple.getObject().stringValue().contains("#workspace")
                              );

                              final Model containedDefinitions = parentModel
                                  .filter(null, null, iri(WORKSPACE_HMAS_IRI));

                              m.addAll(containedThings);
                              m.addAll(workspaceDefTriple);
                              m.addAll(containedDefinitions);

                              m.setNamespace(HMAS, HMAS_IRI);

                              final var parentWorkspaceSplit =
                                  parentIriDefragmented.toString().split("/");
                              final var parentWorkspaceName =
                                  parentWorkspaceSplit[parentWorkspaceSplit.length - 1];

                              this.dispatcherMessagebox.sendMessage(
                                  new HttpNotificationDispatcherMessage.EntityChanged(
                                      this.httpConfig.getWorkspacesUri()
                                          + "?parent=" + parentWorkspaceName,
                                      RdfModelUtils.modelToString(m, RDFFormat.TURTLE,
                                          this.httpConfig.getBaseUriTrailingSlash())
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
            final var iriResource = iri(iri + WORKSPACE_FRAGMENT);
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
                .map(fragmentedIri -> RdfModelUtils.createIri(
                    fragmentedIri.getNamespace().replace("#", "")))
                .peek(irisToDelete::add)
                .forEach(stack::add);
            this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.EntityDeleted(
                    iri.toString(),
                    RdfModelUtils.modelToString(model, RDFFormat.TURTLE,
                        this.httpConfig.getBaseUriTrailingSlash())
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
}
