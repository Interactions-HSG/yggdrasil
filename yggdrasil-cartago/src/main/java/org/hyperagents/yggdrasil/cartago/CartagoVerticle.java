package org.hyperagents.yggdrasil.cartago;

import cartago.AgentBodyArtifact;
import cartago.AgentCredential;
import cartago.AgentId;
import cartago.AgentIdCredential;
import cartago.ArtifactConfig;
import cartago.CartagoEnvironment;
import cartago.CartagoException;
import cartago.Op;
import cartago.OpFeedbackParam;
import cartago.WorkspaceId;
import cartago.events.ActionFailedEvent;
import cartago.events.ActionSucceededEvent;
import cartago.tools.Console;
import cartago.utils.BasicLogger;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.cartago.entities.HypermediaInterfaceFactory;
import org.hyperagents.yggdrasil.cartago.entities.NotificationCallback;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

@SuppressWarnings("PMD.ReplaceHashtableWithMap")
public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CartagoVerticle.class);
  private static final String ARTIFACT_NAME_PARAM = "artifactName";
  private static final String ARTIFACTS_URI_SUFFIX = "/artifacts/";

  private Map<String, AgentCredential> agentCredentials;
  private HttpNotificationDispatcherMessagebox dispatcherMessagebox;
  private RdfStoreMessagebox rdfStoreMessagebox;

  @Override
  public void start() {
    final var registry = HypermediaArtifactRegistry.getInstance();
    registry.addArtifactTemplate("http://example.org/Body", AgentBodyArtifact.class.getCanonicalName());
    registry.addArtifactTemplate("http://example.org/Console", Console.class.getCanonicalName());
    JsonObjectUtils
        .getJsonObject(this.config(), "known-artifacts", LOGGER)
        .ifPresent(registry::addArtifactTemplates);

    registry.setHttpPrefix(new HttpInterfaceConfigImpl(this.context.config()).getBaseUri());

    this.agentCredentials = new Hashtable<>();

    final var eventBus = this.vertx.eventBus();
    final var ownMessagebox = new CartagoMessagebox(eventBus);
    ownMessagebox.init();
    ownMessagebox.receiveMessages(this::handleCartagoRequest);
    this.dispatcherMessagebox = new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    this.rdfStoreMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());

    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoEnvironment.getInstance().init(new BasicLogger());
    } catch (final CartagoException e) {
      LOGGER.error(e.getMessage());
    }
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
          message.reply(HypermediaArtifactRegistry.getInstance().getArtifactDescription(
            this.joinWorkspace(agentId, workspaceName)
          ));
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
              JsonObjectUtils.getString(artifactInit, "artifactClass", LOGGER)
                             .flatMap(registry::getArtifactTemplate)
                             .orElseThrow(),
              artifactName,
              JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER)
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
    LOGGER.info("Creating workspace " + workspaceName);
    final var workspaceId =
        HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix() + workspaceName;
    WorkspaceRegistry.getInstance()
                     .registerWorkspace(CartagoEnvironment.getInstance()
                                                          .getRootWSP()
                                                          .getWorkspace()
                                                          .createWorkspace(workspaceName),
                                        workspaceId);
    return this.createWorkspaceRepresentation(workspaceId, workspaceName);
  }

  private String instantiateSubWorkspace(final String workspaceName, final String subWorkspaceName)
      throws CartagoException {
    LOGGER.info("Creating workspace " + subWorkspaceName + " under " + workspaceName);
    final var subWorkspaceId =
        HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix() + subWorkspaceName;
    WorkspaceRegistry.getInstance()
                     .registerWorkspace(WorkspaceRegistry.getInstance()
                                                         .getWorkspace(workspaceName)
                                                         .orElseThrow()
                                                         .createWorkspace(subWorkspaceName),
                                        subWorkspaceId);
    return this.createWorkspaceRepresentation(subWorkspaceId, subWorkspaceName);
  }

  private String createWorkspaceRepresentation(
      final String workspaceId,
      final String workspaceName
  ) {
    return new TDGraphWriter(
        new ThingDescription
          .Builder(workspaceName)
          .addThingURI(workspaceId)
          .addSemanticType("https://purl.org/hmas/core/Workspace")
          .addAction(
            new ActionAffordance.Builder(
                "makeArtifact",
                new Form.Builder(workspaceId + ARTIFACTS_URI_SUFFIX).build()
            )
            .addInputSchema(
              new ObjectSchema
                .Builder()
                .addProperty(
                  "artifactClass",
                  new StringSchema
                    .Builder()
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build()
                )
                .addProperty(
                  ARTIFACT_NAME_PARAM,
                  new StringSchema
                    .Builder()
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build()
                )
                .addProperty("initParams", new ArraySchema.Builder().build())
                .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                .build()
            )
            .build()
          )
          .addAction(
            new ActionAffordance.Builder(
                "joinWorkspace",
                new Form.Builder(workspaceId + "/join").setMethodName("PUT").build()
            )
            .build()
          )
          .addAction(
            new ActionAffordance.Builder(
                "leaveWorkspace",
                new Form.Builder(workspaceId + "/leave").setMethodName("DELETE").build()
            )
            .build()
          )
          .addAction(
            new ActionAffordance.Builder(
                "focus",
                new Form.Builder(workspaceId + "/focus").setMethodName("POST").build()
            )
            .addInputSchema(
              new ObjectSchema
                .Builder()
                .addProperty(ARTIFACT_NAME_PARAM, new StringSchema.Builder().build())
                .addProperty("artifactIri", new StringSchema.Builder().build())
                .addProperty("callbackIri", new StringSchema.Builder().build())
                .addRequiredProperties(ARTIFACT_NAME_PARAM, "artifactIri", "callbackIri")
                .build()
            )
            .build()
          )
          .build()
    )
    .setNamespace("td", "https://www.w3.org/2019/wot/td#")
    .setNamespace("htv", "http://www.w3.org/2011/http#")
    .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
    .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
    .setNamespace("dct", "http://purl.org/dc/terms/")
    .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
    .setNamespace("hmas", "https://purl.org/hmas/core/")
    .write();
  }

  private String joinWorkspace(final String agentUri, final String workspaceName)
      throws CartagoException {
    LOGGER.info(agentUri + " joins workspace " + workspaceName);
    final var workspace = WorkspaceRegistry.getInstance()
                                           .getWorkspace(workspaceName)
                                           .orElseThrow();
    final var agentCredential = this.getAgentCredential(agentUri);
    workspace.joinWorkspace(agentCredential, e -> {});
    final var workspaceId = workspace.getId();
    final var agentId = this.getAgentId(agentCredential, workspaceId);
    final var registry = HypermediaAgentBodyArtifactRegistry.getInstance();
    if (registry.hasArtifact(agentId, workspaceId)) {
      return registry.getHypermediaName(registry.getArtifact(agentId, workspaceId));
    } else {
      LOGGER.info("create body for agent: " + agentUri);
      final var bodyName = "body_" + agentUri;
      final var hypermediaInterface = HypermediaInterfaceFactory.createBodyInterface(
          workspace,
          workspace.getArtifactDescriptor(bodyName),
          workspace.getArtifact(bodyName),
          agentUri
      );
      HypermediaArtifactRegistry.getInstance().register(hypermediaInterface);
      this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.CreateEntity(
          WorkspaceRegistry.getInstance().getUri(workspaceName) + ARTIFACTS_URI_SUFFIX,
          hypermediaInterface.getActualArtifactName(),
          hypermediaInterface.getHypermediaDescription()
      ));
      final var hypermediaBodyName = registry.getHypermediaName(bodyName);
      registry.setArtifact(agentId, workspaceId, bodyName);
      this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.CreateEntity(
          HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceId.getName()),
          hypermediaBodyName,
          HypermediaArtifactRegistry.getInstance().getArtifactDescription(hypermediaBodyName)
      ));
      return hypermediaBodyName;
    }
  }

  private void focus(
      final String agentUri,
      final String workspaceName,
      final String artifactName,
      final String callbackIri
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    final var workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName).orElseThrow();
    final var artifactIri =
        HypermediaArtifactRegistry.getInstance().getArtifactUri(workspaceName, artifactName);
    LOGGER.info("artifact IRI: " + artifactIri);
    LOGGER.info("callback IRI: " + callbackIri);
    NotificationSubscriberRegistry.getInstance().addCallbackIri(artifactIri, callbackIri);
    workspace
        .focus(
          this.getAgentId(new AgentIdCredential(agentUri), workspace.getId()),
          p -> true,
          new NotificationCallback(this.dispatcherMessagebox),
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
    final var workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName).orElseThrow();
    final var workspaceId = workspace.getId();
    final var agentId = this.getAgentId(this.getAgentCredential(agentUri), workspaceId);
    final var bodyName = HypermediaAgentBodyArtifactRegistry.getInstance()
                                                            .getArtifact(agentId, workspaceId);
    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
        HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName)
        + HypermediaAgentBodyArtifactRegistry.getInstance().getHypermediaName(bodyName)
    ));
    workspace.quitAgent(agentId);
  }

  private void instantiateArtifact(
      final String agentUri,
      final String workspaceName,
      final String artifactClass,
      final String artifactName,
      final Optional<Object[]> params
  ) throws CartagoException {
    this.joinWorkspace(agentUri, workspaceName);
    LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
    final var workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName).orElseThrow();
    final var artifactId = workspace.makeArtifact(
        this.getAgentId(this.getAgentCredential(agentUri), workspace.getId()),
        artifactName,
        artifactClass,
        params.map(ArtifactConfig::new).orElse(new ArtifactConfig())
    );
    final var artifactDescriptor = workspace.getArtifactDescriptor(artifactName);
    LOGGER.info("Done!");
    final var registry = HypermediaArtifactRegistry.getInstance();
    if (registry.hasInterfaceConstructor(artifactClass)) {
      final var hypermediaInterface =
          registry.getInterfaceConstructor(artifactClass)
                  .createHypermediaInterface(workspace, artifactDescriptor, artifactId);
      registry.register(hypermediaInterface);
      this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.CreateEntity(
          WorkspaceRegistry.getInstance().getUri(workspaceName) + ARTIFACTS_URI_SUFFIX,
          hypermediaInterface.getActualArtifactName(),
          hypermediaInterface.getHypermediaDescription()
      ));
    } else {
      this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.CreateEntity(
          WorkspaceRegistry.getInstance().getUri(workspaceName) + ARTIFACTS_URI_SUFFIX,
          artifactName,
          registry.getArtifactDescription(artifactName)
      ));
    }
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
            final var convertedParams =
                registry.hasHypermediaInterface(artifactName)
                ? registry.getHypermediaInterface(artifactName).convert(action, params)
                : params;
            final var completeParams =
                registry.hasFeedbackParam(artifactName, action)
                ? Stream.concat(Arrays.stream(convertedParams), Stream.of(feedbackParameter))
                        .toArray()
                : convertedParams;
            return new Op(action, completeParams);
          })
          .orElseGet(() -> new Op(action));
    LOGGER.info(
        "Performing action "
        + action
        + " on artifact "
        + artifactName
        + " with params: "
        + Arrays.asList(operation.getParamValues())
    );
    final var promise = Promise.<Void>promise();
    WorkspaceRegistry.getInstance()
                     .getWorkspace(workspaceName)
                     .orElseThrow()
                     .execOp(0L,
                             this.getAgentId(
                               this.getAgentCredential(agentUri),
                               WorkspaceRegistry.getInstance()
                                                .getWorkspace(workspaceName)
                                                .orElseThrow()
                                                .getId()
                             ),
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
