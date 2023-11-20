package org.hyperagents.yggdrasil.http;

import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import com.google.gson.JsonParser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

/**
 * This class implements handlers for all HTTP requests. Requests related to CArtAgO operations
 * (e.g., creating a workspace, executing an action) are redirected to the
 * {@link CartagoMessagebox}.
 */
public class HttpEntityHandler {
  private static final Logger LOGGER = LogManager.getLogger(HttpEntityHandler.class);
  private static final String WORKSPACE_ID_PARAM = "wkspid";
  private static final String AGENT_WEBID_HEADER = "X-Agent-WebID";

  private final Messagebox<CartagoMessage> cartagoMessagebox;
  private final Messagebox<RdfStoreMessage> rdfStoreMessagebox;
  private final HttpInterfaceConfig httpConfig;

  public HttpEntityHandler(final Vertx vertx, final Context context) {
    this.cartagoMessagebox = new CartagoMessagebox(vertx.eventBus());
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.httpConfig = new HttpInterfaceConfigImpl(context.config());
  }

  public void handleRedirectWithoutSlash(final RoutingContext routingContext) {
    final var requestUri = routingContext.request().absoluteURI();

    routingContext
        .response()
        .setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY)
        .headers()
        .add(HttpHeaders.LOCATION, requestUri.substring(0, requestUri.length() - 1));
    routingContext.response().end();
  }

  public void handleGetEntity(final RoutingContext routingContext) {
    final var entityIri = this.httpConfig.getBaseUri() + routingContext.request().path();
    LOGGER.info("GET request: " + entityIri);
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(entityIri))
        .onComplete(
          this.handleStoreReply(routingContext, HttpStatus.SC_OK, this.getHeaders(entityIri))
        );
  }

  public void handleCreateWorkspace(final RoutingContext context) {
    final var workspaceName = context.request().getHeader("Slug");
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(workspaceName))
        .compose(response -> this.storeEntity(
          context,
          this.httpConfig.getBaseUri() + context.request().path(),
          workspaceName,
          response.body()
        ))
        .onFailure(context::fail);
  }

  public void handleCreateArtifact(final RoutingContext context) {
    LOGGER.info("Received create artifact request");
    final var representation = context.body().asString();
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    final var artifactName =
        ((JsonObject) Json.decodeValue(representation)).getString("artifactName");

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
            agentId,
            context.pathParam(WORKSPACE_ID_PARAM),
            artifactName,
            representation
        ))
        .compose(response -> this.storeEntity(
          context,
          this.httpConfig.getBaseUri() + context.request().path(),
          artifactName,
          response.body()
        ))
        .onFailure(context::fail);
  }

  // TODO: add payload validation
  public void handleCreateEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }
    this.createEntity(routingContext, routingContext.body().asString());
  }

  public void handleFocus(final RoutingContext context) {
    LOGGER.info("handle focus");
    final var representation = ((JsonObject) Json.decodeValue(context.body().asString()));
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          agentId,
          context.pathParam(WORKSPACE_ID_PARAM),
          representation.getString("artifactName"),
          representation.getString("callbackIri")
        ))
        .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_OK).end())
        .onFailure(t -> context.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  public void handleAction(final RoutingContext context) {
    final var request = context.request();
    final var agentId = request.getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    final var artifactName = context.pathParam("artid");
    final var workspaceName = context.pathParam(WORKSPACE_ID_PARAM);
    final var registry = HypermediaArtifactRegistry.getInstance();
    final var artifactIri = this.httpConfig.getArtifactUri(workspaceName, artifactName);
    final var actionName =
        registry.getActionName(request.method().name(), request.absoluteURI());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(artifactIri))
        .onSuccess(storeResponse -> {
          Optional.ofNullable(context.request().getHeader("X-API-Key"))
                  .filter(a -> !a.isEmpty())
                  .ifPresent(a -> registry.setApiKeyForArtifact(artifactIri, a));

          this.cartagoMessagebox
              .sendMessage(new CartagoMessage.DoAction(
                agentId,
                workspaceName,
                artifactName,
                actionName,
                TDGraphReader
                  .readFromString(TDFormat.RDF_TURTLE, storeResponse.body())
                  .getActions()
                  .stream()
                  .filter(
                    action -> action.getTitle().isPresent()
                              && action.getTitle().get().equals(actionName)
                  )
                  .findFirst()
                  .flatMap(ActionAffordance::getInputSchema)
                  .filter(inputSchema -> inputSchema.getDatatype().equals(DataSchema.ARRAY))
                  .map(inputSchema -> CartagoDataBundle.toJson(
                    ((ArraySchema) inputSchema)
                      .parseJson(JsonParser.parseString(context.body().asString()))
                  ))
              ))
              .onSuccess(cartagoResponse -> {
                LOGGER.info("CArtAgO operation succeeded: " + artifactName + ", " + actionName);
                final var httpResponse = context.response().setStatusCode(HttpStatus.SC_OK);
                if (registry.hasFeedbackParam(artifactName, actionName)) {
                  httpResponse.end(cartagoResponse.body());
                } else {
                  httpResponse.end();
                }
              })
              .onFailure(t -> context.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        })
        .onFailure(t -> context.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  // TODO: add payload validation
  public void handleUpdateEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.UpdateEntity(
            routingContext.request().absoluteURI(),
            routingContext.body().asString()
        ))
        .onComplete(this.handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleDeleteEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.DeleteEntity(routingContext.request().absoluteURI()))
        .onComplete(this.handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleEntitySubscription(final RoutingContext routingContext) {
    final var subscribeRequest = routingContext.body().asJsonObject();

    final var entityIri = subscribeRequest.getString("hub.topic");
    final var callbackIri = subscribeRequest.getString("hub.callback");

    switch (subscribeRequest.getString("hub.mode").toLowerCase(Locale.ENGLISH)) {
      case "subscribe":
        this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.GetEntity(entityIri))
            .onSuccess(response -> {
              NotificationSubscriberRegistry.getInstance().addCallbackIri(entityIri, callbackIri);
              routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
            })
            .onFailure(t -> routingContext.fail(
              t instanceof ReplyException e && e.failureCode() == HttpStatus.SC_NOT_FOUND
              ? HttpStatus.SC_NOT_FOUND
              : HttpStatus.SC_INTERNAL_SERVER_ERROR
            ));
        break;
      case "unsubscribe":
        NotificationSubscriberRegistry.getInstance().removeCallbackIri(entityIri, callbackIri);
        routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
        break;
      default:
        routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        break;
    }
  }

  public void handleJoinWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
          agentId,
          routingContext.pathParam(WORKSPACE_ID_PARAM)
        ))
        .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end(r.body()))
        .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  public void handleLeaveWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(
          agentId,
          routingContext.pathParam(WORKSPACE_ID_PARAM)
        ))
        .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end(r.body()))
        .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  public void handleCreateSubWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
    }

    final var subWorkspaceName = routingContext.request().getHeader("Slug");
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          routingContext.pathParam(WORKSPACE_ID_PARAM),
          subWorkspaceName
        ))
        .compose(r -> this.storeEntity(
            routingContext,
            this.httpConfig.getWorkspacesUri() + "/",
            subWorkspaceName,
            r.body()
        ))
        .onFailure(routingContext::fail);
  }

  private Map<String, List<String>> getHeaders(final String entityIri) {
    final var headers = new HashMap<>(this.getWebSubHeaders(entityIri));
    headers.putAll(this.getCorsHeaders());
    return headers;
  }

  private Map<String, List<String>> getWebSubHeaders(final String entityIri) {
    return this.httpConfig
               .getWebSubHubUri()
               .map(hubIRI -> Map.of(
                 "Link",
                 Arrays.asList("<" + hubIRI + ">; rel=\"hub\"", "<" + entityIri + ">; rel=\"self\"")
               ))
               .orElse(Collections.emptyMap());
  }

  private Map<String, List<String>> getCorsHeaders() {
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

  private Future<Message<String>> storeEntity(
      final RoutingContext context,
      final String requestUri,
      final String entityName,
      final String representation
  ) {
    return this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.CreateEntity(
          requestUri,
          entityName,
          representation
        ))
        .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(representation))
        .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end());
  }

  // TODO: support different content types
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

        headers.forEach((headerName, headerValue) -> {
          if (headerName.equalsIgnoreCase("Link")) {
            httpResponse.putHeader(headerName, headerValue);
          } else {
            httpResponse.putHeader(headerName, String.join(",", headerValue));
          }
        });

        Optional.ofNullable(reply.result().body())
                .filter(r -> !r.isEmpty())
                .ifPresentOrElse(httpResponse::end, httpResponse::end);
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
