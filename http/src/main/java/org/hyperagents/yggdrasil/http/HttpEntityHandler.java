package org.hyperagents.yggdrasil.http;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import com.google.gson.JsonParser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.messages.CartagoMessage;
import org.hyperagents.yggdrasil.messages.Messagebox;
import org.hyperagents.yggdrasil.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.messages.impl.CartagoMessagebox;
import org.hyperagents.yggdrasil.messages.impl.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

import java.util.*;

/**
 * This class implements handlers for all HTTP requests. Requests related to CArtAgO operations (e.g.,
 * creating a workspace, executing an action) are redirected to the {@link CartagoMessagebox}.
 */
public class HttpEntityHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());

  private final Messagebox<CartagoMessage> cartagoMessagebox;
  private final Messagebox<RdfStoreMessage> rdfStoreMessagebox;
  private final HttpInterfaceConfig httpConfig;

  public HttpEntityHandler(final Vertx vertx, final Context context) {
    this.cartagoMessagebox = new CartagoMessagebox(vertx.eventBus());
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.httpConfig = new HttpInterfaceConfigImpl(context.config());
  }

  public void handleRedirectWithoutSlash(final RoutingContext routingContext) {
    final var requestURI = routingContext.request().absoluteURI();

    routingContext
      .response()
      .setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY)
      .headers()
      .add(HttpHeaders.LOCATION, requestURI.substring(0, requestURI.length() - 1));
    routingContext.response().end();
  }

  public void handleGetEntity(final RoutingContext routingContext) {
    final var entityIRI = this.httpConfig.getBaseUri() + routingContext.request().path();
    LOGGER.info("GET request: " + entityIRI);
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(entityIRI, Optional.empty()))
        .onComplete(this.handleStoreReply(routingContext, HttpStatus.SC_OK, this.getHeaders(entityIRI)));
  }

  public void handleCreateEnvironment(final RoutingContext context) {
    final var envName = context.request().getHeader("Slug");

    if (context.request().getHeader("X-Agent-WebID") == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    final var envURI = HypermediaArtifactRegistry.getInstance().getHttpEnvironmentsPrefix() + envName;

    this.createEntity(
      context,
      TDGraphWriter.write(
        new ThingDescription.Builder(envName)
          .addThingURI(envURI)
          .addSemanticType("http://w3id.org/eve#EnvironmentArtifact")
          .addAction(
            new ActionAffordance.Builder("makeWorkspace", new Form.Builder(envURI + "/workspaces/").build())
              .addSemanticType("http://w3id.org/eve#MakeWorkspace")
              .build()
          )
          .build()
      )
    );
  }

  public void handleCreateWorkspace(final RoutingContext context) {
    final var envName = context.pathParam("envid");
    final var workspaceName = context.request().getHeader("Slug");
    final var agentId = context.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(agentId, envName, workspaceName, context.getBodyAsString()))
        .onComplete(response -> {
          if (response.succeeded()) {
            HypermediaArtifactRegistry.getInstance().addWorkspace(envName, workspaceName);
            storeEntity(context, workspaceName, response.result().body());
          } else {
            LOGGER.error("CArtAgO operation has failed.");
          }
        });
  }

  public void handleCreateArtifact(final RoutingContext context) {
    LOGGER.info("Received create artifact request");
    final var representation = context.getBodyAsString();
    final var agentId = context.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    final var artifactName = ((JsonObject) Json.decodeValue(representation)).getString("artifactName");

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(agentId, context.pathParam("wkspid"), artifactName, representation))
        .onComplete(response -> {
          if (response.succeeded()) {
            storeEntity(context, artifactName, response.result().body());
          } else {
            LOGGER.error("CArtAgO operation has failed.");
          }
      });
  }

  // TODO: add payload validation
  public void handleCreateEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader("X-Agent-WebID") == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }
    this.createEntity(routingContext, routingContext.getBodyAsString());
  }

  public void handleAction(final RoutingContext context) {
    final var wkspName = context.pathParam("wkspid");
    final var artifactName = context.pathParam("artid");
    final var request = context.request();
    final var agentId = request.getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    final var artifactRegistry = HypermediaArtifactRegistry.getInstance();
    final var artifactIri = artifactRegistry.getHttpArtifactsPrefix(wkspName) + artifactName;
    final var actionName = artifactRegistry.getActionName(request.rawMethod(), request.absoluteURI());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(artifactIri, Optional.ofNullable(request.getHeader(HttpHeaders.CONTENT_TYPE))))
        .onComplete(reply -> {
          if (reply.succeeded()) {
            final var apiKey = context.request().getHeader("X-API-Key");

            if (apiKey != null && !apiKey.isEmpty()) {
              artifactRegistry.setAPIKeyForArtifact(artifactIri, apiKey);
            }

            this.cartagoMessagebox
                .sendMessage(new CartagoMessage.DoAction(
                  agentId,
                  wkspName,
                  artifactName,
                  actionName,
                  TDGraphReader
                    .readFromString(TDFormat.RDF_TURTLE, reply.result().body())
                    .getActions()
                    .stream()
                    .filter(action -> action.getTitle().isPresent() && action.getTitle().get().compareTo(actionName) == 0)
                    .findFirst()
                    .flatMap(ActionAffordance::getInputSchema)
                    .filter(inputSchema -> inputSchema.getDatatype().equals(DataSchema.ARRAY))
                    .map(inputSchema ->
                        CartagoDataBundle.toJson(
                          ((ArraySchema) inputSchema).parseJson(JsonParser.parseString(context.getBodyAsString()))
                        )
                    )
                ))
                .onComplete(cartagoReply -> {
                  if (cartagoReply.succeeded()) {
                    LOGGER.info("CArtAgO operation succeeded: " + artifactName + ", " + actionName);
                    context.response().setStatusCode(HttpStatus.SC_OK).end();
                  } else {
                    LOGGER.info(
                      "CArtAgO operation failed: "
                        + artifactName
                        + ", "
                        + actionName
                        + "; reason: "
                        + cartagoReply.cause().getMessage()
                    );
                    context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
                  }
                });
          }
        });
  }

  // TODO: add payload validation
  public void handleUpdateEntity(final RoutingContext routingContext) {
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.UpdateEntity(routingContext.request().absoluteURI(), routingContext.getBodyAsString()))
        .onComplete(this.handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleDeleteEntity(final RoutingContext routingContext) {
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.DeleteEntity(routingContext.request().absoluteURI()))
        .onComplete(this.handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleEntitySubscription(final RoutingContext routingContext) {
    final var subscribeRequest = routingContext.getBodyAsJson();

    final var entityIri = subscribeRequest.getString("hub.topic");
    final var callbackIri = subscribeRequest.getString("hub.callback");

    switch (subscribeRequest.getString("hub.mode").toLowerCase(Locale.ENGLISH)) {
      case "subscribe":
        this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.GetEntity(entityIri, Optional.empty()))
            .onComplete(reply -> {
              if (reply.succeeded()) {
                NotificationSubscriberRegistry.getInstance().addCallbackIRI(entityIri, callbackIri);
                routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
              } else {
                ReplyException exception = ((ReplyException) reply.cause());

                if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
                  routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
                } else {
                  routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
                }
              }
            });
        break;
      case "unsubscribe":
        NotificationSubscriberRegistry.getInstance().removeCallbackIRI(entityIri, callbackIri);
        routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
        break;
      default:
        routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        break;
    }
  }

  private Map<String, List<String>> getHeaders(final String entityIRI) {
    final var headers = new HashMap<>(this.getWebSubHeaders(entityIRI));
    headers.putAll(this.getCORSHeaders());
    return headers;
  }

  private Map<String, List<String>> getWebSubHeaders(final String entityIRI) {
    return
      this.httpConfig
          .getWebSubHubUri()
          .map(hubIRI -> Map.of(
            "Link",
            Arrays.asList("<" + hubIRI + ">; rel=\"hub\"", "<" + entityIRI + ">; rel=\"self\"")
          ))
          .orElse(Collections.emptyMap());
  }

  private Map<String, List<String>> getCORSHeaders() {
    return Map.of(
      com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
      Collections.singletonList("*"),
      com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
      Collections.singletonList("true"),
      com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
      List.of(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.HEAD.name(),
        HttpMethod.OPTIONS.name()
      )
    );
  }

  private void storeEntity(
    final RoutingContext context,
    final String entityName,
    final String entityRepresentation
  ) {
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.CreateEntity(
          this.httpConfig.getBaseUri() + context.request().path(),
          entityName,
          entityRepresentation
        ))
        .onComplete(result -> {
          if (result.succeeded()) {
            context.response().setStatusCode(HttpStatus.SC_CREATED).end(entityRepresentation);
            LOGGER.info("Entity created: " + entityRepresentation);
          } else {
            context.response().setStatusCode(HttpStatus.SC_CREATED).end();
            LOGGER.error("RdfStore operation has failed.");
          }
        });
  }

  private void createEntity(final RoutingContext context, final String entityRepresentation) {
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.CreateEntity(
          this.httpConfig.getBaseUri() + context.request().path(),
          context.request().getHeader("Slug"),
          entityRepresentation
        ))
        .onComplete(this.handleStoreReply(context, HttpStatus.SC_CREATED));
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(
    final RoutingContext routingContext,
    final int succeededStatusCode
  ) {
    return this.handleStoreReply(routingContext, succeededStatusCode, new HashMap<>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(
    final RoutingContext routingContext,
    final int succeededStatusCode,
    final Map<String, List<String>> headers
  ) {
    return reply -> {
      if (reply.succeeded()) {
        LOGGER.info("Creating Response");

        final var httpResponse = routingContext.response();
        httpResponse.setStatusCode(succeededStatusCode);

        httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");

        for (String headerName : headers.keySet()) {
          if (headerName.equalsIgnoreCase("Link")) {
            httpResponse.putHeader(headerName, headers.get(headerName));
          } else {
            httpResponse.putHeader(headerName, String.join(",", headers.get(headerName)));
          }
        }

        final var storeReply = reply.result().body();
        if (storeReply != null && !storeReply.isEmpty()) {
          httpResponse.end(storeReply);
        } else {
          httpResponse.end();
        }
      } else {
        final var exception = ((ReplyException) reply.cause());

        LOGGER.info(exception.getMessage());

        if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
          routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
        } else {
          routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
        }
      }
    };
  }
}
