package org.hyperagents.yggdrasil.cartago;

import cartago.AgentCredential;
import cartago.AgentId;
import cartago.AgentIdCredential;
import cartago.ArtifactConfig;
import cartago.CartagoEnvironment;
import cartago.CartagoException;
import cartago.Op;
import cartago.OpFeedbackParam;
import cartago.Workspace;
import cartago.WorkspaceId;
import cartago.events.ActionFailedEvent;
import cartago.events.ActionSucceededEvent;
import cartago.utils.BasicLogger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.function.Failable;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.artifacts.HypermediaArtifact;
import org.hyperagents.yggdrasil.cartago.entities.NotificationCallback;
import org.hyperagents.yggdrasil.cartago.entities.WorkspaceRegistry;
import org.hyperagents.yggdrasil.cartago.entities.errors.AgentNotFoundException;
import org.hyperagents.yggdrasil.cartago.entities.errors.ArtifactNotFoundException;
import org.hyperagents.yggdrasil.cartago.entities.errors.WorkspaceNotFoundException;
import org.hyperagents.yggdrasil.cartago.entities.impl.WorkspaceRegistryImpl;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;


/**
 * The Vertx Verticle that is responsible for enabling Cartago functionality.
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CartagoVerticle.class);
  private static final String DEFAULT_CONFIG_VALUE = "default";
  private static final String YGGDRASIL = "yggdrasil";

  private HttpInterfaceConfig httpConfig;
  private WorkspaceRegistry workspaceRegistry;
  private RepresentationFactory representationFactory;

  // AgentUri -> Workspace -> AgentBodyName
  private Map<String, Map<String, AgentCredential>> agentCredentials;
  private RdfStoreMessagebox storeMessagebox;
  private HttpNotificationDispatcherMessagebox dispatcherMessagebox;
  private HypermediaArtifactRegistry registry;

  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx
        .sharedData()
        .<String, HttpInterfaceConfig>getLocalMap("http-config")
        .get(DEFAULT_CONFIG_VALUE);
    this.workspaceRegistry = new WorkspaceRegistryImpl();
    this.registry = new HypermediaArtifactRegistry();


    final EnvironmentConfig environmentConfig = this.vertx.sharedData()
        .<String, EnvironmentConfig>getLocalMap("environment-config")
        .get(DEFAULT_CONFIG_VALUE);

    final WebSubConfig notificationConfig = this.vertx.sharedData()
        .<String, WebSubConfig>getLocalMap("notification-config")
        .get(DEFAULT_CONFIG_VALUE);

    this.representationFactory = RepresentationFactoryFactory.getRepresentationFactory(
        environmentConfig.getOntology(),
        notificationConfig,
        this.httpConfig
    );
    this.agentCredentials = new HashMap<>();

    final var eventBus = this.vertx.eventBus();
    final var ownMessagebox = new CartagoMessagebox(
        eventBus,
        environmentConfig
    );
    ownMessagebox.init();
    ownMessagebox.receiveMessages(this::handleCartagoRequest);
    this.storeMessagebox = new RdfStoreMessagebox(eventBus);
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(
        eventBus,
        this.vertx
            .sharedData()
            .<String, WebSubConfig>getLocalMap("notification-config")
            .get(DEFAULT_CONFIG_VALUE)
    );
    this.vertx
        .<Void>executeBlocking(() -> {
          CartagoEnvironment.getInstance().init(new BasicLogger());
          this.initializeFromConfiguration();
          return null;
        })
        .onComplete(startPromise);
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.vertx
        .<Void>executeBlocking(() -> {
          CartagoEnvironment.getInstance().shutdown();
          // Resetting CArtAgO root workspace before shutting down to ensure system is fully reset
          final var rootWorkspace = CartagoEnvironment.getInstance().getRootWSP();
          rootWorkspace.setWorkspace(new Workspace(
              rootWorkspace.getId(),
              rootWorkspace,
              new BasicLogger()
          ));
          return null;
        })
        .onComplete(stopPromise);
  }

  private void initializeFromConfiguration() {
    final var environment = this.vertx
        .sharedData()
        .<String, Environment>getLocalMap("environment")
        .get(DEFAULT_CONFIG_VALUE);
    environment.getKnownArtifacts()
        .forEach(a -> registry.addArtifactTemplate(a.getClazz(), a.getTemplate()));
    environment
        .getWorkspaces()
        .forEach(w -> w.getRepresentation().ifPresentOrElse(
            // Not creating a cartago workspace since we're using representation from file
            Failable.asConsumer(r -> {
              this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
                  httpConfig.getWorkspacesUri(),
                  w.getName(),
                  w.getParentName().map(httpConfig::getWorkspaceUriTrailingSlash),
                  Files.readString(r, StandardCharsets.UTF_8)
              ));
              // Since the workspace cannot hold cartago artifacts we only create ones using file
              // representation
              w.getArtifacts().forEach(a -> a.getRepresentation().ifPresent(
                  Failable.asConsumer(ar ->
                      this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                          httpConfig.getArtifactsUri(w.getName()),
                          a.getName(),
                          Files.readString(ar, StandardCharsets.UTF_8)
                      ))
                  )
              ));
            }),
            // if no representation is present we might create cartago objects
            () -> {
              // subworkspace
              w.getParentName().ifPresentOrElse(
                  Failable.asConsumer(p -> this.storeMessagebox.sendMessage(
                      new RdfStoreMessage.CreateWorkspace(
                          this.httpConfig.getWorkspacesUri(),
                          w.getName(),
                          Optional.of(this.httpConfig.getWorkspaceUriTrailingSlash(p)),
                          this.instantiateSubWorkspace(p, w.getName())
                      )
                  )),
                  // workspace
                  Failable.asRunnable(() -> this.storeMessagebox.sendMessage(
                      new RdfStoreMessage.CreateWorkspace(
                          this.httpConfig.getWorkspacesUri(),
                          w.getName(),
                          Optional.empty(),
                          this.instantiateWorkspace(w.getName())
                      )
                  ))
              );

              // add metaData onto Workspace
              w.getMetaData().ifPresent(Failable.asConsumer(metaData ->
                  this.storeMessagebox.sendMessage(
                      new RdfStoreMessage.UpdateEntity(
                          this.httpConfig.getWorkspaceUri(w.getName()),
                          Files.readString(metaData, StandardCharsets.UTF_8)
                      ))
              ));

              // creating bodies and joining workspaces
              w.getAgents().forEach(
                  Failable.asConsumer(a -> {

                    final var body = a.getBodyConfig().stream().filter(b ->
                        b.getJoinedWorkspaces().contains(w.getName())
                    ).findFirst().orElse(a.getBodyConfig().stream().filter(b ->
                        b.getJoinedWorkspaces().isEmpty()).findFirst().orElse(null));



                    this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateBody(
                        w.getName(),
                        a.getAgentUri(),
                        a.getName(),
                        this.joinWorkspace(a.getAgentUri(), a.getName(), w.getName())
                    ));


                    if (body != null && body.getMetadata() != null) {
                      this.storeMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
                            this.httpConfig.getAgentBodyUri(w.getName(), a.getName()),
                            Files.readString(body.getMetadata(), StandardCharsets.UTF_8)
                          ));
                    }
                  })
              );

              // Yggdrasil Agent must join to create the artifacts
              try {
                this.joinWorkspace(
                    this.httpConfig.getAgentUri(YGGDRASIL), YGGDRASIL, w.getName());
              } catch (CartagoException e) {
                throw new RuntimeException(e);
              }

              // creating artifacts
              w.getArtifacts().forEach(a -> a.getClazz().ifPresentOrElse(Failable.asConsumer(c -> {
                this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                    this.httpConfig.getArtifactsUri(w.getName()),
                    a.getName(),
                    this.instantiateArtifact(
                      this.httpConfig.getAgentUri(YGGDRASIL),
                      w.getName(),
                      registry.getArtifactTemplate(c).orElseThrow(),
                      a.getName(),
                      Optional.of(a.getInitializationParameters())
                        .filter(p -> !p.isEmpty())
                        .map(List::toArray).orElse(null)
                    )
                  ));
                a.getMetaData().ifPresent(Failable.asConsumer(metadata ->
                    this.storeMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
                      this.httpConfig.getArtifactUri(w.getName(), a.getName()),
                      Files.readString(metadata, StandardCharsets.UTF_8)
                    ))));
                a.getFocusedBy().forEach(Failable.asConsumer(
                    focusingAgent -> this.focus(w.getAgents()
                      .stream()
                      .filter(ag -> ag.getName().equals(focusingAgent))
                      .findFirst().orElseThrow()
                      .getAgentUri(), w.getName(), a.getName()
                    )
                  ));
              }),
                  () -> a.getRepresentation().ifPresent(Failable.asConsumer(ar ->
                  this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                    httpConfig.getArtifactsUri(w.getName()),
                    a.getName(),
                    Files.readString(ar, StandardCharsets.UTF_8)
                  )))
                )));

              try {
                this.leaveWorkspace(this.httpConfig.getAgentUri(YGGDRASIL), w.getName());
              } catch (CartagoException e) {
                throw new RuntimeException(e);
              }
            }
        ));
  }

  @SuppressWarnings({
      "checkstyle:MissingSwitchDefault",
      "PMD.SwitchStmtsShouldHaveDefault",
      "PMD.SwitchDensity"
  })
  private void handleCartagoRequest(final Message<CartagoMessage> message) {
    try {
      switch (message.body()) {
        case CartagoMessage.CreateWorkspace(String workspaceName) ->
            message.reply(this.instantiateWorkspace(workspaceName));
        case CartagoMessage.CreateSubWorkspace(String workspaceName, String subWorkspaceName) ->
            message.reply(this.instantiateSubWorkspace(workspaceName, subWorkspaceName));
        case CartagoMessage.JoinWorkspace(String agentId, String hint, String workspaceName) ->
            message.reply(this.joinWorkspace(agentId, hint, workspaceName));
        case CartagoMessage.LeaveWorkspace(String agentId, String workspaceName) -> {
          this.leaveWorkspace(agentId, workspaceName);
          message.reply(String.valueOf(HttpStatus.SC_OK));
        }
        case CartagoMessage.CreateArtifact(
            String agentId,
            String workspaceName,
            String artifactName,
            String representation
          ) -> {
          final var artifactInit = new JsonObject(representation);

          message.reply(this.instantiateArtifact(
              agentId,
              workspaceName,
              JsonObjectUtils.getString(artifactInit, "artifactClass", LOGGER::error)
                  .flatMap(registry::getArtifactTemplate)
                  .orElseThrow(),
              artifactName,
              JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER::error)
                  .map(i -> i.getList().toArray()).orElse(null)
          ));
        }
        case CartagoMessage.Focus(
            String agentId,
            String workspaceName,
            String artifactName
          ) -> {
          this.focus(agentId, workspaceName, artifactName);
          message.reply(String.valueOf(HttpStatus.SC_OK));
        }
        case CartagoMessage.DoAction(
            String agentId,
            String workspaceName,
            String artifactName,
            String actionName,
            Optional<String> apiKey,
            String storeResponse,
            String requestContext
          ) -> this.doAction(agentId, workspaceName, artifactName, actionName, apiKey.orElse(null),
                storeResponse,
                requestContext)
            .onSuccess(o -> message.reply(o.orElse(null)))
            .onFailure(e -> message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
        case CartagoMessage.DeleteEntity(
            String workspaceName,
            String entityUri
          ) -> this.deleteEntity(workspaceName, entityUri);
      }
    } catch (final DecodeException | NoSuchElementException | CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (AgentNotFoundException e) {
      message.fail(HttpStatus.SC_METHOD_NOT_ALLOWED, e.getMessage());
    } catch (WorkspaceNotFoundException | ArtifactNotFoundException e) {
      message.fail(HttpStatus.SC_NOT_FOUND, e.getMessage());
    }
  }

  private String instantiateWorkspace(final String workspaceName) throws CartagoException {
    this.workspaceRegistry
        .registerWorkspace(CartagoEnvironment.getInstance()
                .getRootWSP()
                .getWorkspace()
                .createWorkspace(workspaceName),
            this.httpConfig.getWorkspaceUriTrailingSlash(workspaceName));
    return this.representationFactory.createWorkspaceRepresentation(
        workspaceName,
        registry.getArtifactTemplates(),
        true
    );
  }

  private String instantiateSubWorkspace(final String workspaceName, final String subWorkspaceName)
      throws CartagoException {
    this.workspaceRegistry
        .registerWorkspace(this.workspaceRegistry
                .getWorkspace(workspaceName)
                .orElseThrow()
                .createWorkspace(subWorkspaceName),
            this.httpConfig.getWorkspaceUriTrailingSlash(subWorkspaceName));
    return this.representationFactory.createWorkspaceRepresentation(
        subWorkspaceName,
        registry.getArtifactTemplates(),
        true
    );
  }

  private String joinWorkspace(final String agentUri, final String agentBodyName,
                               final String workspaceName)
      throws CartagoException {
    this.workspaceRegistry
        .getWorkspace(workspaceName)
        .orElseThrow()
        .joinWorkspace(this.getAgentCredential(agentUri, agentBodyName, workspaceName), e -> {
        });
    return this.representationFactory.createBodyRepresentation(
        workspaceName,
        agentBodyName,
        new LinkedHashModel()
    );
  }

  private void focus(
      final String agentUri,
      final String workspaceName,
      final String artifactName
  ) throws CartagoException, AgentNotFoundException, ArtifactNotFoundException,
      WorkspaceNotFoundException {
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName)
        .orElseThrow(() -> new WorkspaceNotFoundException(workspaceName));
    workspace
        .focus(
            this.getAgentId(this.getAgentCredential(agentUri, workspaceName)
                    .orElseThrow(() -> new AgentNotFoundException(agentUri)),
                workspace.getId()),
            p -> true,
            new NotificationCallback(this.httpConfig, this.dispatcherMessagebox, workspaceName,
                artifactName),
            Optional.ofNullable(workspace.getArtifact(artifactName))
                .orElseThrow(() -> new ArtifactNotFoundException(artifactName))
        )
        .forEach(p -> this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
                this.httpConfig.getArtifactUriTrailingSlash(workspaceName, artifactName),
                p.toString()
            )
        ));
  }

  private void leaveWorkspace(final String agentUri, final String workspaceName)
      throws CartagoException {
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace.quitAgent(
        this.getAgentId(this.getAgentCredential(agentUri, workspaceName).orElseThrow(),
            workspace.getId()));
  }

  private String instantiateArtifact(
      final String agentUri,
      final String workspaceName,
      final String artifactClass,
      final String artifactName,
      final Object... params
  ) throws CartagoException, WorkspaceNotFoundException, AgentNotFoundException {
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName)
        .orElseThrow(() -> new WorkspaceNotFoundException(workspaceName));

    final var artifactId = workspace.makeArtifact(
        this.getAgentId(this.getAgentCredential(agentUri, workspaceName)
                .orElseThrow(() -> new AgentNotFoundException(agentUri)),
            workspace.getId()),
        artifactName,
        artifactClass,
        new ArtifactConfig(params != null ? params : new Object[0])
    );

    final var artifact =
        (HypermediaArtifact) workspace.getArtifactDescriptor(artifactId.getName()).getArtifact();
    registry.register(artifact);


    return registry.getArtifactDescription(artifactName);
  }

  private Future<Optional<String>> doAction(
      final String agentUri,
      final String workspaceName,
      final String artifactName,
      final String actionUri,
      final String apiKey,
      final String storeResponse,
      final String context
  ) throws CartagoException, AgentNotFoundException {

    final var hypermediaArtifact = registry.getArtifact(artifactName);
    if (apiKey != null) {
      hypermediaArtifact.setApiKey(apiKey);
    }


    final var action = registry.getActionName(actionUri);
    if (action == null) {
      return Future.failedFuture("No action");
    }

    final Optional<String> payload;
    try {
      payload = hypermediaArtifact.handleInput(storeResponse, action, context);
    } catch (Exception e) {
      return Future.failedFuture(e);
    }


    final var listOfParams = new ArrayList<OpFeedbackParam<Object>>();
    final var numberOfFeedbackParams = hypermediaArtifact.handleOutputParams(storeResponse, action);
    for (int i = 0; i < numberOfFeedbackParams; i++) {
      listOfParams.add(new OpFeedbackParam<>());
    }

    final Optional<String> finalPayload = payload;
    final var operation =
        payload
            .map(p -> {
              final var params = CartagoDataBundle.fromJson(finalPayload.get());

              return new Op(
                  action,
                  numberOfFeedbackParams > 0
                      ? Stream.concat(Arrays.stream(params), listOfParams.stream())
                      .toArray()
                      : params
              );
            })
            .orElseGet(() -> {
              if (numberOfFeedbackParams > 0) {
                return new Op(action, listOfParams.toArray());
              }
              return new Op(action);
            });
    final var promise = Promise.<Void>promise();
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    final var agentName = this.getAgentNameFromAgentUri(agentUri, workspaceName);
    this.dispatcherMessagebox.sendMessage(
        new HttpNotificationDispatcherMessage.ActionRequested(
            this.httpConfig.getAgentBodyUriTrailingSlash(workspaceName, agentName),
            this.getActionNotificationContent(artifactName, action).encode()
        )
    );
    workspace.execOp(0L,
        this.getAgentId(this.getAgentCredential(agentUri, workspaceName).orElseThrow(),
            workspace.getId()),
        e -> {
          if (e instanceof ActionSucceededEvent) {
            this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.ActionSucceeded(
                    this.httpConfig.getAgentBodyUriTrailingSlash(workspaceName, agentName),
                    this.getActionNotificationContent(artifactName, action).encode()
                )
            );
            promise.complete();
          } else if (e instanceof ActionFailedEvent f) {
            this.dispatcherMessagebox.sendMessage(
                new HttpNotificationDispatcherMessage.ActionFailed(
                    this.httpConfig.getAgentBodyUriTrailingSlash(workspaceName, agentName),
                    this.getActionNotificationContent(artifactName, action)
                        .put("cause", f.getFailureMsg())
                        .encode()
                )
            );
            promise.fail(f.getFailureMsg());
          }
        },
        artifactName,
        operation,
        -1,
        null);
    return promise.future()
        .map(ignored -> {
          // TODO: add feedbackConversion into the logik as well
          if (!listOfParams.isEmpty()) {
            return listOfParams.stream()
                .map(OpFeedbackParam::get)
                .map(Object::toString)
                .collect(Collectors.joining(", "))
                .describeConstable();
          }
          return Optional.empty();
        });
  }

  private void deleteEntity(final String workspaceName, final String requestUri)
      throws CartagoException {
    final var credentials = getAgentCredential(this.httpConfig.getAgentUri(YGGDRASIL), "root");


    if (workspaceName.equals(requestUri)) {
      final var workspaceDescriptor = this.workspaceRegistry.getWorkspaceDescriptor(workspaceName);
      if (workspaceDescriptor.isEmpty()) {
        return;
      }
      final var parent = workspaceDescriptor.get().getParentInfo();
      final var parentWorkspace = parent.getWorkspace();
      parentWorkspace.removeWorkspace(workspaceName);
      this.workspaceRegistry.deleteWorkspace(workspaceName);

    } else {
      final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
      final var agentId = getAgentId(credentials.orElseThrow(), workspace.getId());
      final var artifact = workspace.getArtifact(requestUri);
      workspace.disposeArtifact(agentId, artifact);
    }
  }

  private JsonObject getActionNotificationContent(final String artifactName, final String action) {
    return JsonObject.of(
        "artifactName",
        artifactName,
        "actionName",
        action
    );
  }

  private String getAgentNameFromAgentUri(final String agentUri, final String workspaceName)
      throws AgentNotFoundException {

    if (this.agentCredentials.get(agentUri) == null
        || this.agentCredentials.get(agentUri).get(workspaceName) == null) {
      throw new AgentNotFoundException(agentUri);
    }
    return this.agentCredentials.get(agentUri).get(workspaceName).getId();
  }

  private AgentId getAgentId(final AgentCredential credential, final WorkspaceId workspaceId) {
    return new AgentId(
        credential.getId(),
        credential.getGlobalId(),
        0,
        credential.getRoleName(),
        workspaceId
    );
  }

  // should give error if no body name since that will indicate that agent has not yet joined
  private Optional<AgentCredential> getAgentCredential(
      final String agentUri, final String workspaceName) {
    if (this.agentCredentials.get(agentUri) == null
        || this.agentCredentials.get(agentUri).get(workspaceName) == null) {
      return Optional.empty();
    }
    return Optional.of(this.agentCredentials.get(agentUri).get(workspaceName));
  }

  private AgentCredential getAgentCredential(final String agentUri, final String agentBodyName,
                                             final String workspaceName) {
    this.agentCredentials.putIfAbsent(agentUri, new HashMap<>());
    this.agentCredentials.get(agentUri).put(workspaceName, new AgentIdCredential(agentBodyName));
    return this.agentCredentials.get(agentUri).get(workspaceName);
  }
}
