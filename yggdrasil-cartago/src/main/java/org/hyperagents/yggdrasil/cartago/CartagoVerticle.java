package org.hyperagents.yggdrasil.cartago;

import cartago.AgentIdCredential;
import cartago.CartagoContext;
import cartago.CartagoException;
import cartago.CartagoService;
import cartago.Op;
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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

@SuppressWarnings("PMD.ReplaceHashtableWithMap")
public class CartagoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private Map<String, CartagoContext> agentContexts;

  @Override
  public void start() {
    JsonObjectUtils
        .getJsonObject(this.config(), "known-artifacts", LOGGER)
        .ifPresent(t -> HypermediaArtifactRegistry.getInstance().addArtifactTemplates(t));

    HypermediaArtifactRegistry.getInstance()
                              .setHttpPrefix(new HttpInterfaceConfigImpl(this.context.config())
                              .getBaseUri());

    this.agentContexts = new Hashtable<>();

    final var eventBus = this.vertx.eventBus();
    final var ownMessagebox = new CartagoMessagebox(eventBus);
    ownMessagebox.init();
    ownMessagebox.receiveMessages(this::handleCartagoRequest);

    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoService.startNode();
      this.fetchCartagoPercepts(new HttpNotificationDispatcherMessagebox(eventBus));
    } catch (final CartagoException e) {
      LOGGER.error(e.getMessage());
    }
  }

  private CompletableFuture<Void> fetchCartagoPercepts(
      final Messagebox<HttpNotificationDispatcherMessage> messagebox
  ) {
    return CompletableFuture.runAsync(new CartagoPerceptFetcher(this.agentContexts, messagebox))
                            .thenCompose(v -> this.fetchCartagoPercepts(messagebox))
                            .exceptionally(t -> {
                              LOGGER.error(t.getMessage());
                              this.fetchCartagoPercepts(messagebox);
                              return null;
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
        case CartagoMessage.CreateWorkspace(
          String agentId,
          String envName,
          String workspaceName,
          String ignored
          ) ->
          message.reply(this.instantiateWorkspace(
            agentId,
            envName,
            workspaceName
          ));
        case CartagoMessage.CreateArtifact(
            String agentId,
            String workspaceName,
            String artifactName,
            String representation
          ) -> {
          final var artifactInit = Json.decodeValue(representation, JsonObject.class);

          this.instantiateArtifact(
              agentId,
              workspaceName,
              JsonObjectUtils.getString(artifactInit, "artifactClass", LOGGER)
                             .flatMap(c -> HypermediaArtifactRegistry.getInstance()
                                                                     .getArtifactTemplate(c))
                             .orElseThrow(),
              artifactName,
              JsonObjectUtils.getJsonArray(artifactInit, "initParams", LOGGER)
                             .map(i -> i.getList().toArray())
          );
          message.reply(HypermediaArtifactRegistry.getInstance()
                 .getArtifactDescription(artifactName));
        }
        case CartagoMessage.DoAction(
          String agentId,
          String workspaceName,
          String artifactName,
          String actionName,
          Optional<String> content
          ) -> {
          this.doAction(
              agentId,
              workspaceName,
              artifactName,
              actionName,
              content
          );
          message.reply(HttpStatus.SC_OK);
        }
      }
    } catch (final DecodeException | NoSuchElementException | CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private String instantiateWorkspace(
      final String agentUri,
      final String envName,
      final String workspaceName
  ) throws CartagoException {
    LOGGER.info("Creating workspace " + workspaceName);
    this.agentContexts
        .computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)))
        .doAction(new Op("createWorkspace", workspaceName));

    // TODO: handle env IRIs
    final var workspaceId =
        HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix(envName) + workspaceName;

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
    final var agentContext =
        this.agentContexts
            .computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)));
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
    final var agentContext =
        this.agentContexts
            .computeIfAbsent(agentUri, k -> new CartagoContext(new AgentIdCredential(k)));
    final var operation =
        payload.map(p -> new Op(action, CartagoDataBundle.fromJson(p)))
               .orElseGet(() -> new Op(action));

    LOGGER.info(
        "Performing action "
        + action
        + " on artifact "
        + artifactName
        + " with params: "
        + Arrays.asList(operation.getParamValues())
    );

    agentContext.doAction(
        agentContext.lookupArtifact(agentContext.joinWorkspace(workspaceName), artifactName),
        operation
    );
  }
}
