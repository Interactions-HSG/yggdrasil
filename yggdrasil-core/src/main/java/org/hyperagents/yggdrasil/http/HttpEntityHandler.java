package org.hyperagents.yggdrasil.http;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.Messagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;


/**
 * This class implements handlers for all HTTP requests. Requests related to CArtAgO operations
 * (e.g., creating a workspace, executing an action) are redirected to the
 * {@link CartagoMessagebox}.
 */
public class HttpEntityHandler implements HttpEntityHandlerInterface {
  private static final Logger LOGGER = LogManager.getLogger(HttpEntityHandler.class);
  private static final String WORKSPACE_ID_PARAM = "wkspid";
  private static final String AGENT_WEBID_HEADER = "X-Agent-WebID";
  private static final String AGENT_LOCALNAME_HEADER = "X-Agent-LocalName";
  private static final String SLUG_HEADER = "Slug";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";

  private final Messagebox<CartagoMessage> cartagoMessagebox;
  private final Messagebox<RdfStoreMessage> rdfStoreMessagebox;
  private final Messagebox<HttpNotificationDispatcherMessage> notificationMessagebox;
  private final HttpInterfaceConfig httpConfig;
  private final WebSubConfig notificationConfig;
  private final RepresentationFactory representationFactory;

  private final boolean environment;

  public HttpEntityHandler(
    final Vertx vertx,
    final HttpInterfaceConfig httpConfig,
    final EnvironmentConfig environmentConfig,
    final WebSubConfig notificationConfig
  ) {
    this.httpConfig = httpConfig;
    this.notificationConfig = notificationConfig;
    this.cartagoMessagebox = new CartagoMessagebox(
      vertx.eventBus(),
      environmentConfig
    );
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.notificationMessagebox =
      new HttpNotificationDispatcherMessagebox(vertx.eventBus(), this.notificationConfig);

    // Should be able to use this boolean value to decide if we use cartago messages or not
    // that way the router does not need to check for routes itself
    this.environment = environmentConfig.isEnabled();
    this.representationFactory =
      RepresentationFactoryFactory.getRepresentationFactory(environmentConfig.getOntology(),notificationConfig,
        httpConfig);
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
    final var entityIri = routingContext.request().absoluteURI();
    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.GetEntity(entityIri))
      .onComplete(
        this.handleStoreSucceededReply(routingContext, HttpStatus.SC_OK, this.getHeaders(entityIri))
      );
  }

  public void handleCreateWorkspaceJson(final RoutingContext context) {
    final var workspaceName = context.request().getHeader(SLUG_HEADER);
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);
    final var requestUri = context.request().absoluteURI();

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    this.rdfStoreMessagebox.sendMessage(
      new RdfStoreMessage.GetEntityIri(this.httpConfig.getWorkspacesUri(), workspaceName)
    ).compose(nameResponse ->
      this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(nameResponse.body()))
        .compose(response ->
          this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.CreateWorkspace(
                requestUri,
                nameResponse.body(),
                Optional.empty(),
                response.body()
              )
            ).onComplete(this.handleStoreSucceededReply(context, HttpStatus.SC_CREATED, this.getHeaders(requestUri + nameResponse.body() + "/")))
        )
        .onFailure(context::fail)
    );
  }

  public void handleCreateWorkspaceTurtle(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    this.handleCreateWorkspaceTurtle(routingContext, routingContext.body().asString());
  }

  public void handleCreateArtifact(final RoutingContext context) {
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);
    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    final var contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE);

    if (contentType == null) {
      context.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
      return;
    }

    switch (contentType) {
      case "application/json" -> handleCreateArtifactJson(context, agentId);
      case TURTLE_CONTENT_TYPE -> handleCreateArtifactTurtle(context);
      default -> context.response().setStatusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE).end();
    }
  }

  public void handleCreateArtifactJson(final RoutingContext context, final String agentId) {
    final var representation = context.body().asString();
    final var requestUri = context.request().absoluteURI();
    final var artifactName =
      ((JsonObject) Json.decodeValue(representation)).getString("artifactName");

    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(
      requestUri, artifactName)
    ).compose(nameResponse ->
      this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          agentId,
          context.pathParam(WORKSPACE_ID_PARAM),
          nameResponse.body(),
          representation
        ))
        .compose(response ->
          this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.CreateArtifact(
                requestUri,
                nameResponse.body(),
                response.body()
              )
            ).onComplete(this.handleStoreSucceededReply(context, HttpStatus.SC_CREATED, this.getHeaders(requestUri + nameResponse.body() + "/")))
        )
        .onFailure(r -> context.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end()))
    ;
  }

  public void handleCreateArtifactTurtle(final RoutingContext routingContext) {
    this.handleCreateArtifactTurtle(routingContext, routingContext.body().asString());
  }

  public void handleCreateArtifactTurtle(final RoutingContext context, final String entityRepresentation) {
    final var requestUri = context.request().absoluteURI();

    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri,
        context.request().getHeader(SLUG_HEADER)))
      .compose(
        actualEntityName -> this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
          requestUri,
          actualEntityName.body(),
          this.representationFactory.createArtifactRepresentation(
            context.pathParam(WORKSPACE_ID_PARAM),
            actualEntityName.body(),
            "https://purl.org/hmas/Artifact",
            false
          )
        )).onComplete(
          response -> this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
            actualEntityName.address(),
            entityRepresentation
          ))
        ).onComplete(this.handleStoreSucceededReply(context, HttpStatus.SC_CREATED,
          this.getHeaders(requestUri + actualEntityName.body()))))
      .onFailure(f -> context.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end());
  }

  public void handleFocus(final RoutingContext context) {
    final var representation = ((JsonObject) Json.decodeValue(context.body().asString()));
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    final var workspaceName = context.pathParam(WORKSPACE_ID_PARAM);
    final var artifactName = representation.getString("artifactName");
    this.notificationMessagebox
      .sendMessage(new HttpNotificationDispatcherMessage.AddCallback(
        this.httpConfig.getArtifactUri(workspaceName, artifactName),
        representation.getString("callbackIri")
      ))
      .compose(v -> this.cartagoMessagebox.sendMessage(new CartagoMessage.Focus(
        agentId,
        workspaceName,
        artifactName
      )))
      .onComplete(this.handleStoreSucceededReply(context, HttpStatus.SC_OK, this.getHeaders(context.request().absoluteURI())));
  }

  // TODO: add payload validation
  public void handleUpdateEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.ReplaceEntity(
        routingContext.request().absoluteURI(),
        routingContext.body().asString()
      ))
      .onComplete(this.handleStoreSucceededReply(routingContext));
  }

  public void handleDeleteEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    // remove trailing slash
    final var temp = routingContext.request().absoluteURI().endsWith("/") ?
      routingContext.request().absoluteURI().substring(0, routingContext.request().absoluteURI().length() - 1) :
      routingContext.request().absoluteURI();

    final var parts = temp.split("/");
    final var artifactName = parts[parts.length - 1];

    if (environment) {
      final var workspaceName = routingContext.pathParam(WORKSPACE_ID_PARAM);
        this.cartagoMessagebox.sendMessage(
          new CartagoMessage.DeleteEntity(workspaceName, artifactName)
        );
    }
    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.DeleteEntity(routingContext.request().absoluteURI()))
      .onComplete(this.handleStoreSucceededReply(routingContext));
  }

  public void handleEntitySubscription(final RoutingContext routingContext) {
    final var subscribeRequest = routingContext.body().asJsonObject();

    final var entityIri = subscribeRequest.getString("hub.topic");
    final var callbackIri = subscribeRequest.getString("hub.callback");

    switch (subscribeRequest.getString("hub.mode").toLowerCase(Locale.ENGLISH)) {
      case "subscribe":
        if (entityIri.matches("^https?://.*?:[0-9]+/workspaces/$")) {
          this.notificationMessagebox
            .sendMessage(
              new HttpNotificationDispatcherMessage.AddCallback(entityIri, callbackIri)
            )
            .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end())
            .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        } else {
          final var actualEntityIri =
            Pattern.compile("^(https?://.*?:[0-9]+/workspaces/.*?)/(?:artifacts|agents)/$")
              .matcher(entityIri)
              .results()
              .map(r -> r.group(1))
              .findFirst()
              .orElse(entityIri);
          this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.GetEntity(actualEntityIri))
            .compose(r -> this.notificationMessagebox.sendMessage(
              // TODO: why do we need this if we have the same callback again?
              new HttpNotificationDispatcherMessage.AddCallback(entityIri, callbackIri)
            ))
            .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end())
            .onFailure(t -> routingContext.fail(
              t instanceof ReplyException e && e.failureCode() == HttpStatus.SC_NOT_FOUND
                ? HttpStatus.SC_NOT_FOUND
                : HttpStatus.SC_INTERNAL_SERVER_ERROR
            ));
        }
        break;
      case "unsubscribe":
        this.notificationMessagebox
          .sendMessage(
            new HttpNotificationDispatcherMessage.RemoveCallback(entityIri, callbackIri)
          )
          .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end())
          .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        break;
      default:
        routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        break;
    }
  }

  public void handleJoinWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);
    final var agentBodyName = routingContext.request().getHeader(AGENT_LOCALNAME_HEADER);

    if (agentId == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    if (agentBodyName == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
      return;
    }

    final var workspaceName = routingContext.pathParam(WORKSPACE_ID_PARAM);

    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.JoinWorkspace(
        agentId,
        agentBodyName,
        workspaceName
      ))
      .compose(response -> this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.CreateBody(
            workspaceName,
            agentId,
            agentBodyName,
            response.body()
          ))
      )
      .onComplete(this.handleStoreSucceededReply(routingContext));
  }

  public void handleLeaveWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);
    final var hint = routingContext.request().getHeader(AGENT_LOCALNAME_HEADER);

    if (agentId == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    final var workspaceName = routingContext.pathParam(WORKSPACE_ID_PARAM);
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.LeaveWorkspace(
        agentId,
        workspaceName
      ))
      .compose(r -> {
        if (hint == null) {
          return this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.DeleteEntity(
              this.httpConfig.getAgentBodyUri(
                workspaceName,
                this.getAgentNameFromId(agentId)
              )
            ));
        }
        return this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.DeleteEntity(
            this.httpConfig.getAgentBodyUri(
              workspaceName,
              hint
            )
          ));
      })
      .onComplete(this.handleStoreSucceededReply(routingContext));
  }

  public void handleCreateSubWorkspace(final RoutingContext context) {
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    final var subWorkspaceName = context.request().getHeader(SLUG_HEADER);
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateSubWorkspace(
        context.pathParam(WORKSPACE_ID_PARAM),
        subWorkspaceName
      ))
      .compose(response ->
        this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.CreateWorkspace(
            this.httpConfig.getWorkspacesUri(),
            subWorkspaceName,
            Optional.of(this.httpConfig.getWorkspaceUri(context.pathParam(WORKSPACE_ID_PARAM))),
            response.body()
          ))
          .onComplete(this.handleStoreSucceededReply(context, HttpStatus.SC_CREATED, this.getHeaders(this.httpConfig.getWorkspaceUri(subWorkspaceName))))
      )
      .onFailure(f -> context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end());
  }

  public void handleAction(final RoutingContext context) {
    final var request = context.request();
    final var agentId = request.getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    final var artifactName = context.pathParam("artid");
    final var workspaceName = context.pathParam(WORKSPACE_ID_PARAM);
    final var artifactIri = this.httpConfig.getArtifactUri(workspaceName, artifactName);
    final var actionName = request.method().name() + request.absoluteURI();

    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.GetEntity(artifactIri.substring(0, artifactIri.length() - 1)))
      .onSuccess(storeResponse -> {

        final var apiKey = Optional.ofNullable(context.request().getHeader("X-API-Key"));

        this.cartagoMessagebox
          .sendMessage(new CartagoMessage.DoAction(
            agentId,
            workspaceName,
            artifactName,
            actionName,
            apiKey,
            storeResponse.body(),
            context.body().asString()
          ))
          .onSuccess(cartagoResponse -> {
            final var httpResponse = context.response().setStatusCode(HttpStatus.SC_OK);
            if (cartagoResponse.body() == null) {
              httpResponse.end();
            } else {
              // TODO: Once we remove constriction on return type being json array this will move into CartagoVerticle
              final var responseString = "[" + cartagoResponse.body() + "]";
              httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).end(responseString);
            }
          })
          .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end());
      })
      .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end());
  }


  public void handleQuery(final RoutingContext routingContext) {
    final var request = routingContext.request();
    if (request.method().equals(HttpMethod.GET)) {
      final var queryParams = request.params();
      final var queries = queryParams.getAll("query");
      if (!routingContext.body().isEmpty() || queries.size() != 1) {
        routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        return;
      }
      this.handleQueryMessage(
        routingContext,
        URLDecoder.decode(queries.getFirst(), StandardCharsets.UTF_8),
        queryParams.getAll("default-graph-uri"),
        queryParams.getAll("named-graph-uri"),
        request.getHeader(HttpHeaders.ACCEPT)
      );
    } else if (request.getHeader(HttpHeaders.CONTENT_TYPE)
      .equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
      final var formParams = request.formAttributes();
      final var queries = formParams.getAll("query");
      if (queries.size() != 1) {
        routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
        return;
      }
      this.handleQueryMessage(
        routingContext,
        URLDecoder.decode(queries.getFirst(), StandardCharsets.UTF_8),
        formParams.getAll("default-graph-uri"),
        formParams.getAll("named-graph-uri"),
        request.getHeader(HttpHeaders.ACCEPT)
      );
    } else {
      final var queryParams = request.params();
      this.handleQueryMessage(
        routingContext,
        routingContext.body().asString(),
        queryParams.getAll("default-graph-uri"),
        queryParams.getAll("named-graph-uri"),
        request.getHeader(HttpHeaders.ACCEPT)
      );
    }
  }

  private void handleQueryMessage(
    final RoutingContext routingContext,
    final String query,
    final List<String> defaultGraphUris,
    final List<String> namedGraphUris,
    final String resultContentType
  ) {
    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
        query,
        defaultGraphUris.stream()
          .map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8))
          .toList(),
        namedGraphUris.stream()
          .map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8))
          .toList(),
        Optional.ofNullable(resultContentType).orElse("application/sparql-results+json")
      ))
      .onSuccess(r ->
        routingContext.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, resultContentType)
          .end(r.body())
      )
      .onFailure(t -> {
        if (t instanceof ReplyException e) {
          routingContext.response().setStatusCode(e.failureCode()).end();
        } else {
          routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
        }
      });
  }

  // TODO getAgentNameFromId is also in CartagoVerticle needs to be linked!
  private String getAgentNameFromId(final String agentId) {
    final var returnVal = Pattern.compile("^https?://.*?:[0-9]+/agents/(.*?)$")
      .matcher(agentId)
      .results()
      .findFirst();
    if (returnVal.isPresent()) {
      return returnVal.get().group(1);
    }
    return "anonymous-agent";
  }

  private Map<String, List<String>> getHeaders(final String entityIri) {
    final var headers = new HashMap<>(this.getWebSubHeaders(entityIri));
    headers.putAll(this.getCorsHeaders());
    return headers;
  }

  private Map<String, List<String>> getWebSubHeaders(final String entityIri) {
    return this.notificationConfig.isEnabled()
      ? Map.of(
      "Link",
      Arrays.asList(
        "<" + this.notificationConfig.getWebSubHubUri() + ">; rel=\"hub\"",
        "<" + entityIri + ">; rel=\"self\""
      )
    )
      : Collections.emptyMap();
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

  // TODO: support different content types
  private void handleCreateWorkspaceTurtle(final RoutingContext context, final String entityRepresentation) {
    final var requestUri = this.httpConfig.getBaseUri() + context.request().path().substring(1);
    final var hint = context.request().getHeader(SLUG_HEADER);
    final var name = hint.endsWith("/") ? hint : hint + "/";
    final var entityIri = RdfModelUtils.createIri(requestUri + name);

    final Model entityGraph;
    try {
      entityGraph = RdfModelUtils.stringToModel(
        entityRepresentation,
        entityIri,
        RDFFormat.TURTLE
      );
    } catch (Exception e) {
      context.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
      return;
    }

    // TODO: if slug is without trailing backslash and representation is with then doesnt work

    entityGraph.remove(null, RDF.TYPE, RdfModelUtils.createIri("https://purl.org/hmas/ResourceProfile"));
    entityGraph.remove(null, RdfModelUtils.createIri("https://purl.org/hmas/isProfileOf"), null);
    entityGraph.remove(null, RDF.TYPE, RdfModelUtils.createIri("https://purl.org/hmas/Workspace"));
    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri, name)).compose(
        actualEntityName -> {
          var workspaceRepresentation =
            this.representationFactory.createWorkspaceRepresentation(actualEntityName.body(), new HashSet<>(),false);
          try {
            final var baseModel = RdfModelUtils.stringToModel(workspaceRepresentation, entityIri, RDFFormat.TURTLE);
            entityGraph.addAll(RdfModelUtils.stringToModel(workspaceRepresentation, entityIri, RDFFormat.TURTLE));
            baseModel.getNamespaces().forEach(entityGraph::setNamespace);
            workspaceRepresentation = RdfModelUtils.modelToString(entityGraph, RDFFormat.TURTLE, this.httpConfig.getBaseUri());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.CreateWorkspace(
              requestUri,
              actualEntityName.body(),
              entityGraph.stream()
                .filter(t ->
                  t.getSubject().toString().contains(entityIri.stringValue())
                    && t.getPredicate().equals(RdfModelUtils.createIri(
                    "https://purl.org/hmas/isContainedIn"
                  ))
                )
                .map(Statement::getObject)
                .map(t -> t instanceof IRI i ? Optional.of(i) : Optional.<IRI>empty())
                .flatMap(Optional::stream)
                .map(IRI::toString)
                .findFirst(),
              workspaceRepresentation
            ));
        }).onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle").end(r.body()))
      .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle").end());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreSucceededReply(
    final RoutingContext routingContext
  ) {
    return this.handleStoreSucceededReply(routingContext, HttpStatus.SC_OK, new HashMap<>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreSucceededReply(
    final RoutingContext routingContext,
    final int succeededStatusCode,
    final Map<String, List<String>> headers
  ) {
    return reply -> {
      if (reply.succeeded()) {
        final var httpResponse = routingContext.response();
        httpResponse.setStatusCode(succeededStatusCode);
        httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE);

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

        LOGGER.error(exception);

        if (exception.failureCode() == HttpStatus.SC_NOT_FOUND) {
          routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND).end();
        } else {
          routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
        }
      }
    };
  }
}
