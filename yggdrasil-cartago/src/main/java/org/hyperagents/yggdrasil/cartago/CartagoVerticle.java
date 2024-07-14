package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import cartago.events.ActionFailedEvent;
import cartago.events.ActionSucceededEvent;
import cartago.utils.BasicLogger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.regex.Pattern;
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
import org.hyperagents.yggdrasil.cartago.entities.impl.WorkspaceRegistryImpl;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.model.Environment;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;


@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CartagoVerticle.class);
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private HttpInterfaceConfig httpConfig;
  private WorkspaceRegistry workspaceRegistry;
  private RepresentationFactory representationFactory;
  private Map<String, AgentCredential> agentCredentials;
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

    this.representationFactory = RepresentationFactoryFactory.getRepresentationFactory(
      environmentConfig.getOntology(),
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
      .forEach(w -> {
        w.getParentName().ifPresentOrElse(
          Failable.asConsumer(p -> this.storeMessagebox.sendMessage(
            new RdfStoreMessage.CreateWorkspace(
              this.httpConfig.getWorkspacesUri(),
              w.getName(),
              Optional.of(this.httpConfig.getWorkspaceUri(p)),
              this.instantiateSubWorkspace(p, w.getName())
            )
          )),
          Failable.asRunnable(() -> this.storeMessagebox.sendMessage(
            new RdfStoreMessage.CreateWorkspace(
              this.httpConfig.getWorkspacesUri(),
              w.getName(),
              Optional.empty(),
              this.instantiateWorkspace(w.getName())
            )
          ))
        );
        w.getAgents().forEach(
          Failable.asConsumer(a -> this.joinWorkspace(a.getName(), w.getName()))
        );
        w.getArtifacts().forEach(a -> a.getClazz().ifPresent(Failable.asConsumer(c -> {
          this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
            this.httpConfig.getArtifactsUri(w.getName()) + "/",
            a.getName(),
            this.instantiateArtifact(
              this.httpConfig.getAgentUri("yggdrasil"),
              w.getName(),
              registry.getArtifactTemplate(c).orElseThrow(),
              a.getName(),
              Optional.of(a.getInitializationParameters())
                .filter(p -> !p.isEmpty())
                .map(List::toArray).orElse(null)
            )
          ));
          a.getFocusingAgents().forEach(Failable.asConsumer(ag ->
            this.focus(ag.getName(), w.getName(), a.getName())
          ));
        })));
      });
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
        ) -> this.doAction(agentId, workspaceName, artifactName, actionName, apiKey.orElse(null), storeResponse, requestContext)
          .onSuccess(o -> message.reply(o.orElse(null)))
          .onFailure(e -> message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
        case CartagoMessage.DeleteEntity(
          String workspaceName,
          String entityUri
        ) -> this.deleteEntity(workspaceName, entityUri);
      }
    } catch (final DecodeException | NoSuchElementException | CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private String instantiateWorkspace(final String workspaceName) throws CartagoException {
    this.workspaceRegistry
      .registerWorkspace(CartagoEnvironment.getInstance()
          .getRootWSP()
          .getWorkspace()
          .createWorkspace(workspaceName),
        this.httpConfig.getWorkspaceUri(workspaceName));
    return this.representationFactory.createWorkspaceRepresentation(
      workspaceName,
      registry.getArtifactTemplates()
    );
  }

  private String instantiateSubWorkspace(final String workspaceName, final String subWorkspaceName)
    throws CartagoException {
    this.workspaceRegistry
      .registerWorkspace(this.workspaceRegistry
          .getWorkspace(workspaceName)
          .orElseThrow()
          .createWorkspace(subWorkspaceName),
        this.httpConfig.getWorkspaceUri(subWorkspaceName));
    return this.representationFactory.createWorkspaceRepresentation(
      subWorkspaceName,
      registry.getArtifactTemplates()
    );
  }

  private String joinWorkspace(final String agentUri, final String hint, final String workspaceName)
    throws CartagoException {
    if (hint == null || hint.isEmpty()) {
      return this.joinWorkspace(agentUri, workspaceName);
    }
    this.workspaceRegistry
      .getWorkspace(workspaceName)
      .orElseThrow()
      .joinWorkspace(this.getAgentCredential(agentUri), e -> {
      });
    return this.representationFactory.createBodyRepresentation(
      workspaceName,
      hint,
      new LinkedHashModel()
    );
  }

  private String joinWorkspace(final String agentUri, final String workspaceName)
    throws CartagoException {
    this.workspaceRegistry
      .getWorkspace(workspaceName)
      .orElseThrow()
      .joinWorkspace(this.getAgentCredential(agentUri), e -> {
      });
    return this.representationFactory.createBodyRepresentation(
      workspaceName,
      this.getAgentNameFromAgentUri(agentUri),
      new LinkedHashModel()
    );
  }

  private void focus(
    final String agentUri,
    final String workspaceName,
    final String artifactName
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace
      .focus(
        this.getAgentId(new AgentIdCredential(agentUri), workspace.getId()),
        p -> true,
        new NotificationCallback(this.httpConfig, this.dispatcherMessagebox, workspaceName, artifactName),
        Optional.ofNullable(workspace.getArtifact(artifactName)).orElseThrow()
      )
      .forEach(p -> this.dispatcherMessagebox.sendMessage(
        new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
          this.httpConfig.getArtifactUri(workspaceName, artifactName),
          p.toString()
        )
      ));
  }

  private void leaveWorkspace(final String agentUri, final String workspaceName)
    throws CartagoException {
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace.quitAgent(this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()));
  }

  private String instantiateArtifact(
    final String agentUri,
    final String workspaceName,
    final String artifactClass,
    final String artifactName,
    final Object... params
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();

    final var artifactId = workspace.makeArtifact(
      this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()),
      artifactName,
      artifactClass,
      new ArtifactConfig(params != null ? params : new Object[0])
    );

    final var artifact = (HypermediaArtifact) workspace.getArtifactDescriptor(artifactId.getName()).getArtifact();
    registry.register(artifact);


    return registry.getArtifactDescription(artifactName);
  }

  // for some reason pmd thinks this method is not used
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private Future<Optional<String>> doAction(
    final String agentUri,
    final String workspaceName,
    final String artifactName,
    final String actionUri,
    final String apiKey,
    final String storeResponse,
    final String context
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);

    final var hypermediaArtifact = registry.getArtifact(artifactName);
    if(apiKey != null) {
      hypermediaArtifact.setApiKey(apiKey);
    }


    final var action = registry.getActionName(actionUri);
    if (action == null) return Future.failedFuture("No action");

    Optional<String> payload;
    try {
      payload = hypermediaArtifact.handleInput(storeResponse, action, context);
    } catch (Exception e) {
      return Future.failedFuture(e);
    }


    final var listOfParams = new ArrayList<OpFeedbackParam<Object>>();
    final var numberOfFeedbackParams = hypermediaArtifact.handleOutputParams(storeResponse, action, context);
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
    final var agentName = this.getAgentNameFromAgentUri(agentUri);
    this.dispatcherMessagebox.sendMessage(
      new HttpNotificationDispatcherMessage.ActionRequested(
        this.httpConfig.getAgentBodyUri(workspaceName, agentName),
        this.getActionNotificationContent(artifactName, action).encode()
      )
    );
    workspace.execOp(0L,
      this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()),
      e -> {
        if (e instanceof ActionSucceededEvent) {
          this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.ActionSucceeded(
              this.httpConfig.getAgentBodyUri(workspaceName, agentName),
              this.getActionNotificationContent(artifactName, action).encode()
            )
          );
          promise.complete();
        } else if (e instanceof ActionFailedEvent f) {
          this.dispatcherMessagebox.sendMessage(
            new HttpNotificationDispatcherMessage.ActionFailed(
              this.httpConfig.getAgentBodyUri(workspaceName, agentName),
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

  private void deleteEntity(final String workspaceName, final String requestUri) throws CartagoException {
    final var credentials = getAgentCredential(this.httpConfig.getAgentUri("yggdrasil"));


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
      final var agentId = getAgentId(credentials, workspace.getId());
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

  private String getAgentNameFromAgentUri(final String agentUri) {
    final var returnVal = Pattern.compile("^https?://.*?:[0-9]+/agents/(.*?)$")
      .matcher(agentUri)
      .results()
      .findFirst();
    if (returnVal.isPresent()) {
      return returnVal.get().group(1);
    }
    return "anonymous-agent";
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

  private AgentCredential getAgentCredential(final String agentUri) {
    this.agentCredentials.putIfAbsent(agentUri, new AgentIdCredential(agentUri));
    return this.agentCredentials.get(agentUri);
  }
}
