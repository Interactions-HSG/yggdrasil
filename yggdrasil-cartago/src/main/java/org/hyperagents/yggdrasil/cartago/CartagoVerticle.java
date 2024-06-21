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
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.function.Failable;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;

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

  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx
                          .sharedData()
                          .<String, HttpInterfaceConfig>getLocalMap("http-config")
                          .get(DEFAULT_CONFIG_VALUE);
    this.workspaceRegistry = new WorkspaceRegistryImpl();
    this.representationFactory = new RepresentationFactoryImpl(this.httpConfig);
    this.agentCredentials = new HashMap<>();

    final var eventBus = this.vertx.eventBus();
    final var ownMessagebox = new CartagoMessagebox(
        eventBus,
        this.vertx
            .sharedData()
            .<String, EnvironmentConfig>getLocalMap("environment-config")
            .get(DEFAULT_CONFIG_VALUE)
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
    final var registry = HypermediaArtifactRegistry.getInstance();
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
                  this.httpConfig.getWorkspacesUri() + "/",
                  w.getName(),
                  Optional.of(this.httpConfig.getWorkspaceUri(p)),
                  this.instantiateSubWorkspace(p, w.getName())
                )
              )),
              Failable.asRunnable(() -> this.storeMessagebox.sendMessage(
                new RdfStoreMessage.CreateWorkspace(
                  this.httpConfig.getWorkspacesUri() + "/",
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
                          .map(List::toArray)
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
        case CartagoMessage.JoinWorkspace(String agentId, String workspaceName) ->
          message.reply(this.joinWorkspace(agentId, workspaceName));
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
                             .flatMap(HypermediaArtifactRegistry.getInstance()::getArtifactTemplate)
                             .orElseThrow(),
              artifactName,
              JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER::error)
                             .map(i -> i.getList().toArray())
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

  private String joinWorkspace(final String agentUri, final String workspaceName)
      throws CartagoException {
    this.workspaceRegistry
        .getWorkspace(workspaceName)
        .orElseThrow()
        .joinWorkspace(this.getAgentCredential(agentUri), e -> {});
    return this.representationFactory.createBodyRepresentation(
      workspaceName,
      this.getAgentNameFromAgentUri(agentUri),
      new NoSecurityScheme(),
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
          new NotificationCallback(this.httpConfig, this.dispatcherMessagebox),
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
    return HypermediaArtifactRegistry.getInstance().getArtifactDescription(artifactName);
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
          .orElseGet(() -> registry.hasFeedbackParam(artifactName, action)
                           ? new Op(action, feedbackParameter)
                           : new Op(action));
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

  private JsonObject getActionNotificationContent(final String artifactName, final String action) {
    return JsonObject.of(
      "artifactName",
      artifactName,
      "actionName",
      action
    );
  }

  private String getAgentNameFromAgentUri(final String agentUri) {
    return Pattern.compile("^https?://.*?:[0-9]+/agents/(.*?)$")
                  .matcher(agentUri)
                  .results()
                  .findFirst()
                  .orElseThrow()
                  .group(1);
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
