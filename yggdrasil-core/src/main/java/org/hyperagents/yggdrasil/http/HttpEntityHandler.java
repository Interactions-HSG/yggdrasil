package org.hyperagents.yggdrasil.http;

import static org.eclipse.rdf4j.model.util.Values.iri;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
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
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
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

  /**
   * Constructor for the EntityHandler.
   *
   * @param vertx              vertx
   * @param httpConfig         httpConfig
   * @param environmentConfig  environmentConfig
   * @param notificationConfig notificationConfig
   */
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
        RepresentationFactoryFactory.getRepresentationFactory(environmentConfig.getOntology(),
            notificationConfig,
            httpConfig);
  }

  /**
   * Redirect function that returns the same requestUri without the trailing slash.
   *
   * @param routingContext the routingContext
   */
  public void handleRedirectWithoutSlash(final RoutingContext routingContext) {
    final var requestUri = routingContext.request().absoluteURI();

    routingContext
        .response()
        .setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY)
        .headers()
        .add(HttpHeaders.LOCATION, requestUri.substring(0, requestUri.length() - 1));
    routingContext.response().end();
  }

  /**
   * Returns the representation of the entity at the given Uri.
   *
   * @param routingContext the routingContext
   */
  public void handleGetEntity(final RoutingContext routingContext) {
    final var entityIri = routingContext.request().absoluteURI();
    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(entityIri))
        .onComplete(
            this.handleStoreReply(routingContext, HttpStatus.SC_OK,
                this.getHeaders(entityIri))
        );
  }

  // TODO: what if localhost and different baseUri will headers work correctly for websub?

  /**
   * Returns the representation of the entity at the given Uri.
   *
   * @param context the routingContext
   */
  public void handleGetWorkspaces(final RoutingContext context) {
    var parentUri = context.request().getParam("parent");
    parentUri = parentUri == null ? this.httpConfig.getBaseUriTrailingSlash()
        : this.httpConfig.getWorkspaceUriTrailingSlash(parentUri);
    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetWorkspaces(parentUri)).onComplete(
        this.handleStoreReply(context, HttpStatus.SC_OK,
            this.getHeaders(context.request().absoluteURI()))
    );
  }

  /**
   * Returns the representation of the entity at the given Uri.
   *
   * @param context the routingContext
   */
  public void handleGetArtifacts(final RoutingContext context) {
    final var workspaceName = context.pathParam(WORKSPACE_ID_PARAM);
    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetArtifacts(workspaceName))
        .onComplete(this.handleStoreReply(context, HttpStatus.SC_OK,
            this.getHeaders(this.httpConfig.getArtifactsUri(workspaceName))));
  }

  /**
   * Handles the creation of a workspace.
   *
   * @param context routingContext
   */
  public void handleCreateWorkspace(final RoutingContext context) {
    if (context.request().getHeader(AGENT_WEBID_HEADER) == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }
    final var requestUri = this.httpConfig.getWorkspacesUriTrailingSlash();
    final var workspaceName = context.request().getHeader(SLUG_HEADER);
    final var parentWorkspaceName = context.pathParam(WORKSPACE_ID_PARAM);

    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri, workspaceName))
        .compose(response -> createWorkspaceRepresentation(response.body(), context))
        .compose(response -> this.rdfStoreMessagebox.sendMessage(
            new RdfStoreMessage.CreateWorkspace(
                requestUri,
                response.workspaceName(),
                parentWorkspaceName == null ? Optional.empty() : Optional.of(
                    this.httpConfig.getWorkspaceUri(parentWorkspaceName)
                ),
                response.modelString()
            )
        ).onComplete(
            this.handleStoreReply(context, HttpStatus.SC_CREATED,
                this.getHeaders(requestUri + response.workspaceName()))
        )).onFailure(
            f -> context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end()
        );
  }

  /**
   * Handles the creation of artifacts.
   *
   * @param context routingContext
   */
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

  /**
   * Handles the creation of virtual artifacts.
   *
   * @param context routingContext
   * @param agentId agent WebID
   */
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
                    ).onComplete(this.handleStoreReply(context, HttpStatus.SC_CREATED,
                        this.getHeaders(requestUri + nameResponse.body())))
            )
            .onFailure(r -> {
              if (r instanceof ReplyException e) {
                context.response().setStatusCode(e.failureCode()).end();
              } else {
                context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
              }
            })
    );
  }

  /**
   * Creating an Artifact given content in text/turtle.
   *
   * @param context              routingContext
   */
  public void handleCreateArtifactTurtle(final RoutingContext context) {
    final var requestUri = context.request().absoluteURI();
    final var entityRepresentation = context.body().asString();

    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri,
            context.request().getHeader(SLUG_HEADER)))
        .compose(
            actualEntityName -> this.rdfStoreMessagebox.sendMessage(
                new RdfStoreMessage.CreateArtifact(
                    requestUri,
                    actualEntityName.body(),
                    this.representationFactory.createArtifactRepresentation(
                        context.pathParam(WORKSPACE_ID_PARAM),
                        actualEntityName.body(),
                        "https://purl.org/hmas/Artifact",
                        false
                    )
                )).onComplete(
                  response -> this.rdfStoreMessagebox.sendMessage(
                    new RdfStoreMessage.UpdateEntity(
                        requestUri + actualEntityName.body(),
                        entityRepresentation
                    )
                ).onComplete(this.handleStoreReply(context, HttpStatus.SC_CREATED,
                    this.getHeaders(requestUri + actualEntityName.body())))))
        .onFailure(f -> context.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end());
  }

  /**
   * Handles focusing on a given entity.
   *
   * @param context routingContext
   */
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
            this.httpConfig.getArtifactUriFocusing(workspaceName, artifactName),
            representation.getString("callbackIri")
        ))
        .compose(v -> this.cartagoMessagebox.sendMessage(new CartagoMessage.Focus(
            agentId,
            workspaceName,
            artifactName
        )))
        .onComplete(this.handleStoreReply(context, HttpStatus.SC_OK,
            this.getHeaders(context.request().absoluteURI())));
  }

  // TODO: add payload validation

  /**
   * Updating an entity through PUT requests -> completely replaces the representation.
   *
   * @param routingContext routingContext
   */
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
        .onComplete(this.handleStoreReply(routingContext));
  }

  /**
   * Handles the deletion of an Entity.
   *
   * @param routingContext routingContext
   */
  public void handleDeleteEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    // remove trailing slash
    final var temp = routingContext.request().absoluteURI().endsWith("/")
        ?
        routingContext.request().absoluteURI().substring(0,
            routingContext.request().absoluteURI().length() - 1) :
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
        .onComplete(this.handleStoreReply(routingContext));
  }

  /**
   * Handles websub functionality.
   *
   * @param routingContext routingContext
   */
  public void handleEntitySubscription(final RoutingContext routingContext) {
    final var subscribeRequest = routingContext.body().asJsonObject();

    final var entityIri = subscribeRequest.getString("hub.topic");
    final var callbackIri = subscribeRequest.getString("hub.callback");


    switch (subscribeRequest.getString("hub.mode").toLowerCase(Locale.ENGLISH)) {
      case "subscribe":
        if (entityIri.matches("^https?://.*?:[0-9]+/workspaces(/)?(\\?(parent=[^&]+))?$")) {
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

  /**
   * handles joining a workspace.
   *
   * @param routingContext routingContext
   */
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
        .onComplete(this.handleStoreReply(routingContext));
  }

  /**
   * Handles leaving a workspace.
   *
   * @param routingContext routingContext
   */
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
        .compose(r -> this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.DeleteEntity(
                this.httpConfig.getAgentBodyUriTrailingSlash(
                    workspaceName,
                    hint
                )
            )))
        .onComplete(this.handleStoreReply(routingContext));
  }

  /**
   * handles the execution of an action.
   *
   * @param context routingContext
   */
  public void handleAction(final RoutingContext context) {
    final var request = context.request();
    final var agentId = request.getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
      return;
    }

    final var artifactName = context.pathParam("artid");
    final var workspaceName = context.pathParam(WORKSPACE_ID_PARAM);
    final var artifactIri = this.httpConfig
        .getArtifactUriTrailingSlash(workspaceName, artifactName);
    final var actionName = request.method().name() + request.absoluteURI();

    this.rdfStoreMessagebox
        .sendMessage(
            new RdfStoreMessage.GetEntity(artifactIri.substring(0, artifactIri.length() - 1)))
        .onSuccess(storeResponse -> {

          final var apiKey = Optional.ofNullable(context.request()
              .getHeader("X-API-Key"));

          this.cartagoMessagebox
              .sendMessage(new CartagoMessage.DoAction(
                  agentId,
                  workspaceName,
                  artifactName,
                  actionName,
                  apiKey,
                  storeResponse.body(),
                  context.body().asString()
              )).onSuccess(cartagoResponse -> {
                final var httpResponse = context.response().setStatusCode(HttpStatus.SC_OK);
                if (cartagoResponse.body() == null) {
                  httpResponse.end();
                } else {
                  // TODO: Once we remove constriction on return type being json array this will
                  // move into CartagoVerticle
                  final var responseString = "[" + cartagoResponse.body() + "]";
                  httpResponse.putHeader(HttpHeaders.CONTENT_TYPE,
                          HttpHeaderValues.APPLICATION_JSON)
                      .end(responseString);
                }
              })
              .onFailure(
                  t -> {
                    if (t instanceof ReplyException e) {
                      context.response().setStatusCode(e.failureCode()).end();
                    } else {
                      context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
                    }
                  });
        }).onFailure(
            t -> context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end());
  }

  /**
   * handles a query to the rdfstore.
   *
   * @param routingContext routingContext
   */
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


  private Future<WorkspaceResult> buildAndConvertModel(final String modelString,
                                                       final Model additionalMetadataModel,
                                                       final IRI entityIri,
                                                       final String workspaceName) {
    if (additionalMetadataModel == null) {
      return Future.succeededFuture(new WorkspaceResult(workspaceName, modelString));
    }
    try {
      final Model baseModel = RdfModelUtils.stringToModel(modelString, entityIri, RDFFormat.TURTLE);
      additionalMetadataModel.addAll(baseModel);
      baseModel.getNamespaces().forEach(additionalMetadataModel::setNamespace);
      final String result = RdfModelUtils.modelToString(additionalMetadataModel, RDFFormat.TURTLE,
          this.httpConfig.getBaseUriTrailingSlash());
      return Future.succeededFuture(new WorkspaceResult(workspaceName, result));
    } catch (IOException e) {
      return Future.failedFuture(e);
    }
  }

  private Future<WorkspaceResult> createWorkspaceRepresentation(final String workspaceName,
                                                                final RoutingContext ctx) {
    final var entityIri = iri(this.httpConfig.getWorkspaceUri(workspaceName));

    final Model additionalMetadataModel;
    try {
      additionalMetadataModel = ctx.body().isEmpty()
          ? null
          : RdfModelUtils.stringToModel(ctx.body().asString(), entityIri, RDFFormat.TURTLE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final Future<WorkspaceResult> baseModelFuture = environment
        ? ctx.pathParam(WORKSPACE_ID_PARAM) == null
        ? this.cartagoMessagebox.sendMessage(new CartagoMessage.CreateWorkspace(workspaceName))
        .compose(
            r -> buildAndConvertModel(r.body(), additionalMetadataModel, entityIri, workspaceName))
        : this.cartagoMessagebox.sendMessage(new CartagoMessage.CreateSubWorkspace(
            ctx.pathParam(WORKSPACE_ID_PARAM), workspaceName))
        .compose(
            r -> buildAndConvertModel(r.body(), additionalMetadataModel, entityIri, workspaceName)
        )
        : buildAndConvertModel(
        this.representationFactory.createWorkspaceRepresentation(workspaceName, new HashSet<>(),
            false),
        additionalMetadataModel,
        entityIri,
        workspaceName
    );
    return baseModelFuture.recover(Future::failedFuture);
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


  private Handler<AsyncResult<Message<String>>> handleStoreReply(
      final RoutingContext routingContext
  ) {
    return this.handleStoreReply(routingContext, HttpStatus.SC_OK, new HashMap<>());
  }


  private Handler<AsyncResult<Message<String>>> handleStoreReply(
      final RoutingContext routingContext,
      final int successCode,
      final Map<String, List<String>> headers
  ) {
    return reply -> {
      if (reply.succeeded()) {
        final var httpResponse = routingContext.response();
        httpResponse.setStatusCode(successCode);
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

  private record WorkspaceResult(String workspaceName, String modelString) {
  }
}
