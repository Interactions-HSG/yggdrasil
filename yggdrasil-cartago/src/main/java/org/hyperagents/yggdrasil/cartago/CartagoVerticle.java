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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.cartago.entities.NotificationCallback;
import org.hyperagents.yggdrasil.cartago.entities.WorkspaceRegistry;
import org.hyperagents.yggdrasil.cartago.entities.impl.WorkspaceRegistryImpl;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CartagoVerticle.class);

  private HttpInterfaceConfig httpConfig;
  private WorkspaceRegistry workspaceRegistry;
  private RepresentationFactory representationFactory;
  private Map<String, AgentCredential> agentCredentials;
  private HttpNotificationDispatcherMessagebox dispatcherMessagebox;

  @Override
  public void start(final Promise<Void> startPromise) {
    final var registry = HypermediaArtifactRegistry.getInstance();
    JsonObjectUtils
        .getJsonObject(this.config(), "known-artifacts", LOGGER::error)
        .ifPresent(registry::addArtifactTemplates);

    this.httpConfig = new HttpInterfaceConfigImpl(this.context.config());
    this.workspaceRegistry = new WorkspaceRegistryImpl();
    this.representationFactory = new RepresentationFactoryImpl(this.httpConfig);
    this.agentCredentials = new HashMap<>();

    final var eventBus = this.vertx.eventBus();
    final var ownMessagebox = new CartagoMessagebox(eventBus);
    ownMessagebox.init();
    ownMessagebox.receiveMessages(this::handleCartagoRequest);
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());

    this.vertx
        .<Void>executeBlocking(() -> {
          CartagoEnvironment.getInstance().init(new BasicLogger());
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
        case CartagoMessage.JoinWorkspace(String agentId, String workspaceName) -> {
          this.joinWorkspace(agentId, workspaceName);
          message.reply(String.valueOf(HttpStatus.SC_OK));
        }
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
          final var registry = HypermediaArtifactRegistry.getInstance();
          this.instantiateArtifact(
              agentId,
              workspaceName,
              JsonObjectUtils.getString(artifactInit, "artifactClass", LOGGER::error)
                             .flatMap(registry::getArtifactTemplate)
                             .orElseThrow(),
              artifactName,
              JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER::error)
                             .map(i -> i.getList().toArray())
          );
          message.reply(registry.getArtifactDescription(artifactName));
        }
        case CartagoMessage.Focus(
          String agentId,
          String workspaceName,
          String artifactName,
          String callbackIri
          ) -> {
          this.focus(agentId, workspaceName, artifactName, callbackIri);
          message.reply(String.valueOf(HttpStatus.SC_OK));
        }
        case CartagoMessage.DoAction(
          String agentId,
          String workspaceName,
          String artifactName,
          String actionName,
          Optional<String> content
          ) ->
          this.doAction(agentId, workspaceName, artifactName, actionName, content)
              .onSuccess(o -> message.reply(o.orElse(String.valueOf(HttpStatus.SC_OK))))
              .onFailure(e -> message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage()));
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
      HypermediaArtifactRegistry.getInstance().getArtifactTemplates()
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
      HypermediaArtifactRegistry.getInstance().getArtifactTemplates()
    );
  }

  private void joinWorkspace(final String agentUri, final String workspaceName)
      throws CartagoException {
    this.workspaceRegistry
        .getWorkspace(workspaceName)
        .orElseThrow()
        .joinWorkspace(this.getAgentCredential(agentUri), e -> {});
  }

  private void focus(
      final String agentUri,
      final String workspaceName,
      final String artifactName,
      final String callbackIri
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    final var artifactIri = this.httpConfig.getArtifactUri(workspaceName, artifactName);
    NotificationSubscriberRegistry.getInstance().addCallbackIri(artifactIri, callbackIri);
    workspace
        .focus(
          this.getAgentId(new AgentIdCredential(agentUri), workspace.getId()),
          p -> true,
          new NotificationCallback(this.httpConfig, this.dispatcherMessagebox),
          Optional.ofNullable(workspace.getArtifact(artifactName)).orElseThrow()
        )
        .forEach(p -> this.dispatcherMessagebox.sendMessage(
          new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
            artifactIri,
            p.toString()
          )
        ));
  }

  private void leaveWorkspace(final String agentUri, final String workspaceName)
      throws CartagoException {
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace.quitAgent(this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()));
  }

  private void instantiateArtifact(
      final String agentUri,
      final String workspaceName,
      final String artifactClass,
      final String artifactName,
      final Optional<Object[]> params
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace.makeArtifact(
        this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()),
        artifactName,
        artifactClass,
        params.map(ArtifactConfig::new).orElse(new ArtifactConfig())
    );
  }

  private Future<Optional<String>> doAction(
      final String agentUri,
      final String workspaceName,
      final String artifactName,
      final String action,
      final Optional<String> payload
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var registry = HypermediaArtifactRegistry.getInstance();
    final var feedbackParameter = new OpFeedbackParam<>();
    final var operation =
        payload
          .map(p -> {
            final var params = CartagoDataBundle.fromJson(payload.get());
            return new Op(
              action,
              registry.hasFeedbackParam(artifactName, action)
              ? Stream.concat(Arrays.stream(params), Stream.of(feedbackParameter))
                      .toArray()
              : params
            );
          })
          .orElseGet(() -> new Op(action));
    final var promise = Promise.<Void>promise();
    final var workspace = this.workspaceRegistry.getWorkspace(workspaceName).orElseThrow();
    workspace.execOp(0L,
                     this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()),
                     e -> {
                       if (e instanceof ActionSucceededEvent) {
                         promise.complete();
                       } else if (e instanceof ActionFailedEvent f) {
                         promise.fail(f.getFailureMsg());
                       }
                     },
                     artifactName,
                     operation,
                     -1,
                     null);
    return promise.future()
                  .map(ignored ->
                    Optional.ofNullable(feedbackParameter.get())
                            .map(r ->
                              registry.hasFeedbackResponseConverter(artifactName, action)
                              ? registry.getFeedbackResponseConverter(artifactName, action)
                                        .apply(r)
                              : r
                            )
                            .map(Object::toString)
                  );
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
