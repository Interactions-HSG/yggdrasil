package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.messages.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.messages.MessageAddresses;
import org.hyperagents.yggdrasil.messages.MessageHeaders;
import org.hyperagents.yggdrasil.messages.MessageRequestMethods;
import org.hyperagents.yggdrasil.messages.impl.HttpNotificationDispatcherMessageboxImpl;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private Map<String, CartagoContext> agentContexts;

  @Override
  public void start() {
    JsonObjectUtils.getJsonObject(this.config(), "known-artifacts", LOGGER)
                   .ifPresent(t -> HypermediaArtifactRegistry.getInstance().addArtifactTemplates(t));

    HypermediaArtifactRegistry.getInstance().setHttpPrefix(new HttpInterfaceConfigImpl(this.context.config()).getBaseUri());

    this.agentContexts = new Hashtable<>();

    final var eventBus = this.vertx.eventBus();
    eventBus.consumer(MessageAddresses.CARTAGO.getName(), this::handleCartagoRequest);

    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoService.startNode();
      this.fetchCartagoPercepts(new HttpNotificationDispatcherMessageboxImpl(eventBus));
    } catch (final CartagoException e) {
      LOGGER.error(e.getMessage());
    }
  }

  private CompletableFuture<Void> fetchCartagoPercepts(final HttpNotificationDispatcherMessagebox messagebox) {
    return CompletableFuture.runAsync(new CartagoPerceptFetcher(this.agentContexts, messagebox))
                            .thenCompose(v -> this.fetchCartagoPercepts(messagebox))
                            .exceptionally(t -> {
                              LOGGER.error(t.getMessage());
                              this.fetchCartagoPercepts(messagebox);
                              return null;
                            });
  }

  private void handleCartagoRequest(final Message<String> message) {
    Optional.ofNullable(message.headers().get(MessageHeaders.AGENT_ID.getName())).ifPresentOrElse(
      agentUri -> {
        final var workspaceName = message.headers().get(MessageHeaders.WORKSPACE_NAME.getName());
        final var artifactName = message.headers().get(MessageHeaders.ARTIFACT_NAME.getName());

        try {
          // TODO: Is throwing exception the best default behavior?
          switch (
            MessageRequestMethods.getFromName(message.headers().get(MessageHeaders.REQUEST_METHOD.getName()))
                                 .orElseThrow()
          ) {
            case CREATE_WORKSPACE:
              message.reply(this.instantiateWorkspace(
                agentUri,
                message.headers().get(MessageHeaders.ENV_NAME.getName()),
                workspaceName
              ));
              break;
            case CREATE_ARTIFACT:
              final var artifactInit = Json.decodeValue(message.body(), JsonObject.class);

              this.instantiateArtifact(
                agentUri,
                workspaceName,
                JsonObjectUtils.getString(artifactInit, "artifactClass", LOGGER)
                               .flatMap(c -> HypermediaArtifactRegistry.getInstance().getArtifactTemplate(c))
                               .orElseThrow(),
                artifactName,
                JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER).map(i -> i.getList().toArray())
              );
              message.reply(HypermediaArtifactRegistry.getInstance().getArtifactDescription(artifactName));
              break;
            case DO_ACTION:
              this.doAction(
                agentUri,
                workspaceName,
                artifactName,
                message.headers().get(MessageHeaders.ACTION_NAME.getName()),
                Optional.ofNullable(message.body())
              );
              message.reply(HttpStatus.SC_OK);
              break;
          }
        } catch (final DecodeException | NoSuchElementException | CartagoException e) {
          message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
      },
      () -> message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.")
    );
  }

  private String instantiateWorkspace(final String agentUri, final String envName, final String workspaceName)
    throws CartagoException {
    LOGGER.info("Creating workspace " + workspaceName);
    this.agentContexts
        .computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)))
        .doAction(new Op("createWorkspace", workspaceName));

    // TODO: handle env IRIs
    final var workspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix(envName) + workspaceName;

    return new TDGraphWriter(
      new ThingDescription
        .Builder(workspaceName)
        .addThingURI(workspaceId)
        .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
        .addAction(
          new ActionAffordance
            .Builder(
              "makeArtifact",
              new Form.Builder(workspaceId + "/artifacts/").build()
            )
            .addSemanticType("http://w3id.org/eve#MakeArtifact")
            .addInputSchema(
              new ObjectSchema
                .Builder()
                .addProperty(
                  "artifactClass",
                  new StringSchema
                    .Builder()
                    .addSemanticType("http://w3id.org/eve#ArtifactClass")
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build()
                )
                .addProperty(
                  "artifactName",
                  new StringSchema
                    .Builder()
                    .addSemanticType("http://w3id.org/eve#ArtifactName")
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build()
                )
                .addProperty("initParams", new ArraySchema.Builder().build())
                .addRequiredProperties("artifactClass", "artifactName")
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
    .setNamespace("eve", "http://w3id.org/eve#")
    .write();
  }

  private void instantiateArtifact(
    final String agentUri,
    final String workspaceName,
    final String artifactClass,
    final String artifactName,
    final Optional<Object[]> params
  ) throws CartagoException {
    final var agentContext = this.agentContexts.computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)));
    final var workspaceId = agentContext.joinWorkspace(workspaceName);

    LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);

    if (params.isPresent()) {
      agentContext.makeArtifact(workspaceId, artifactName, artifactClass, params.get());
      agentContext.doAction(new Op("focusWhenAvailable", artifactName));
    } else {
      LOGGER.info("Creating artifact...");
      agentContext.makeArtifact(workspaceId, artifactName, artifactClass);
      agentContext.doAction(new Op("focusWhenAvailable", artifactName));
      agentContext.lookupArtifact(workspaceId, artifactName);
      LOGGER.info("Done!");
    }
  }

  private void doAction(
    final String agentUri,
    final String workspaceName,
    final String artifactName,
    final String action,
    final Optional<String> payload
  ) throws CartagoException {
    final var agentContext = this.agentContexts.computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)));
    final var operation = payload.map(p -> new Op(action, CartagoDataBundle.fromJson(p))).orElseGet(() -> new Op(action));

    LOGGER.info(
      "Performing action "
      + action
      + " on artifact "
      + artifactName
      + " with params: "
      + Arrays.asList(operation.getParamValues())
    );

    agentContext.doAction(agentContext.lookupArtifact(agentContext.joinWorkspace(workspaceName), artifactName), operation);
  }
}
