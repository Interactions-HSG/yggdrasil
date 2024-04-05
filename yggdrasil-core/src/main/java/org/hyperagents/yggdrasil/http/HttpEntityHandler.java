package org.hyperagents.yggdrasil.http;

import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
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

import static org.hyperagents.yggdrasil.utils.JsonObjectUtils.parseInput;


/**
 * This class implements handlers for all HTTP requests. Requests related to CArtAgO operations
 * (e.g., creating a workspace, executing an action) are redirected to the
 * {@link CartagoMessagebox}.
 */
public class HttpEntityHandler implements HttpEntityHandlerInterface {
  private static final Logger LOGGER = LogManager.getLogger(HttpEntityHandler.class);
  private static final String WORKSPACE_ID_PARAM = "wkspid";
  private static final String AGENT_WEBID_HEADER = "X-Agent-WebID";

  private final Messagebox<CartagoMessage> cartagoMessagebox;
  private final Messagebox<RdfStoreMessage> rdfStoreMessagebox;
  private final Messagebox<HttpNotificationDispatcherMessage> notificationMessagebox;
  private final HttpInterfaceConfig httpConfig;
  private final WebSubConfig notificationConfig;

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
    final var entityIri = this.httpConfig.getBaseUri() + routingContext.request().path().substring(1);
    this.rdfStoreMessagebox
      .sendMessage(new RdfStoreMessage.GetEntity(entityIri))
      .onComplete(
        this.handleStoreReply(routingContext, HttpStatus.SC_OK, this.getHeaders(entityIri))
      );
  }

  public void handleCreateWorkspaceJson(final RoutingContext context) {
    final var workspaceName = context.request().getHeader("Slug");
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
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
              this.httpConfig.getBaseUri() + context.request().path(),
              nameResponse.body(),
              Optional.empty(),
              response.body()
            ))
            .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(r.body()))
            .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end())
        )
        .onFailure(context::fail)
    );
  }

  public void handleCreateWorkspaceTurtle(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
    }
    this.handleCreateWorkspaceTurtle(routingContext, routingContext.body().asString());
  }

  public void handleCreateArtifactJson(final RoutingContext context) {
    final var representation = context.body().asString();
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
    }

    final var artifactName =
      ((JsonObject) Json.decodeValue(representation)).getString("artifactName");

    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(
      this.httpConfig.getBaseUri() + context.request().path(), artifactName)
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
              this.httpConfig.getBaseUri() + context.request().path(),
              nameResponse.body(),
              response.body()
            ))
            .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(r.body()))
            .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end())
        )
        .onFailure(context::fail))
    ;
  }

  public void handleCreateArtifactTurtle(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
    }
    this.handleCreateArtifactTurtle(routingContext, routingContext.body().asString());
  }

  public void handleCreateArtifactTurtle(final RoutingContext context, final String entityRepresentation) {
    // TODO: FORGETTING TO ADD ENTITY REPRESENTATION TO MODEL?
    final var requestUri = this.httpConfig.getBaseUri() + context.request().path().substring(1);
    final var hint = context.request().getHeader("Slug");
    final var name = hint.endsWith("/") ? hint.substring(0, hint.length() - 1) : hint;
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);
    final var entityIri = RdfModelUtils.createIri(requestUri + name);
    Model entityGraph = null;
    try {
      entityGraph = RdfModelUtils.stringToModel(
        entityRepresentation,
        entityIri,
        RDFFormat.TURTLE
      );
    } catch (final Exception e) {
      LOGGER.error(e);
      context.fail(HttpStatus.SC_BAD_REQUEST);
    }


    Model finalEntityGraph = entityGraph;
    this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri, name)).compose(
      actualEntityName -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          agentId,
          context.pathParam(WORKSPACE_ID_PARAM),
          actualEntityName.body(),
          String.format(
            """
            {
              "artifactName": "%s",
              "artifactClass": "http://example.org/Artifact"
            }
            """,
            actualEntityName.body()
          )
        ))
        .compose(response -> {
          var artifactRepresentation = response.body();
          try {
            var baseModel = RdfModelUtils.stringToModel(artifactRepresentation, entityIri, RDFFormat.TURTLE);
            finalEntityGraph.addAll(RdfModelUtils.stringToModel(artifactRepresentation, entityIri, RDFFormat.TURTLE));
            baseModel.getNamespaces().forEach(finalEntityGraph::setNamespace);
            artifactRepresentation = RdfModelUtils.modelToString(finalEntityGraph, RDFFormat.TURTLE, this.httpConfig.getBaseUri());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          return this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.CreateArtifact(
              this.httpConfig.getBaseUri() + context.request().path(),
              actualEntityName.body(),
              artifactRepresentation//artifactRepresentation
            ))
            .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(r.body()))
            .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end());
        }).onFailure(context::fail));
  }

  public void handleFocus(final RoutingContext context) {
    final var representation = ((JsonObject) Json.decodeValue(context.body().asString()));
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
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
      .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_OK).end(r.body()))
      .onFailure(t -> context.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  // TODO: add payload validation
  public void handleUpdateEntity(final RoutingContext routingContext) {
    if (routingContext.request().getHeader(AGENT_WEBID_HEADER) == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
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
      return;
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

    if (agentId == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
    }

    final var workspaceName = routingContext.pathParam(WORKSPACE_ID_PARAM);
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.JoinWorkspace(
        agentId,
        workspaceName
      ))
      .compose(response ->
        this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.CreateBody(
            workspaceName,
            agentId,
            this.getAgentNameFromId(agentId),
            response.body()
          ))
      )
      .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end(r.body()))
      .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  public void handleLeaveWorkspace(final RoutingContext routingContext) {
    final var agentId = routingContext.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      routingContext.fail(HttpStatus.SC_UNAUTHORIZED);
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
          this.httpConfig.getAgentBodyUri(
            workspaceName,
            this.getAgentNameFromId(agentId)
          )
        )))
      .onSuccess(r -> routingContext.response().setStatusCode(HttpStatus.SC_OK).end(r.body()))
      .onFailure(t -> routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR));
  }

  public void handleCreateSubWorkspace(final RoutingContext context) {
    final var agentId = context.request().getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
    }

    final var subWorkspaceName = context.request().getHeader("Slug");
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateSubWorkspace(
        context.pathParam(WORKSPACE_ID_PARAM),
        subWorkspaceName
      ))
      .compose(response ->
        this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.CreateWorkspace(
            this.httpConfig.getWorkspacesUri() + "/",
            subWorkspaceName,
            Optional.of(this.httpConfig.getWorkspaceUri(context.pathParam(WORKSPACE_ID_PARAM))),
            response.body()
          ))
          .onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(r.body()))
          .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end())
      )
      .onFailure(context::fail);
  }

// TODO: MAKE IT WORK FOR BOTH TD AND HMAS SOMEHOW???
// HOW TO BEST HANDLE CARTAGO ACTIONS? -> INPUT ALWAYS JSON ARRAY AS PER API
  public void handleAction(final RoutingContext context) {
    final var request = context.request();
    final var agentId = request.getHeader(AGENT_WEBID_HEADER);

    if (agentId == null) {
      context.fail(HttpStatus.SC_UNAUTHORIZED);
      return;
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

        // TODO: Actually handle actions with parameters
        // gets the signifier for the action
        final var signifierIRI = artifactIri + "#" + actionName + "-Signifier";


        var signifier = ResourceProfileGraphReader.readFromString(storeResponse.body()).getExposedSignifiers().stream()
          .filter(sig -> sig.getIRIAsString().isPresent())
          .filter(sig -> sig.getIRIAsString().get().equals(signifierIRI))
          .findFirst();

        Optional<String> description = Optional.empty();
        if (signifier.isPresent() && signifier.get().getActionSpecification().getInputSpecification().isPresent()) {
          JsonElement jsonElement = JsonParser.parseString(context.body().asString());
          var input = signifier.get().getActionSpecification().getInputSpecification().get();
          QualifiedValueSpecification qualifiedValueSpecification = (QualifiedValueSpecification) input;
          description = CartagoDataBundle.toJson(
            parseInput(jsonElement,qualifiedValueSpecification,new ArrayList<>())
          ).describeConstable();
        }

        this.cartagoMessagebox
          .sendMessage(new CartagoMessage.DoAction(
            agentId,
            workspaceName,
            artifactName,
            actionName,
            description
          ))
          .onSuccess(cartagoResponse -> {
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


  public void handleQuery(final RoutingContext routingContext) {
    final var request = routingContext.request();
    if (request.method().equals(HttpMethod.GET)) {
      final var queryParams = request.params();
      final var queries = queryParams.getAll("query");
      if (!routingContext.body().isEmpty() || queries.size() != 1) {
        routingContext.fail(HttpStatus.SC_BAD_REQUEST);
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
        routingContext.fail(HttpStatus.SC_BAD_REQUEST);
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
          routingContext.fail(e.failureCode());
        } else {
          routingContext.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
      });
  }

  // TODO getAgentNameFromId is also in CartagoVerticle needs to be linked!
  private String getAgentNameFromId(final String agentId) {
    var returnVal = Pattern.compile("^https?://.*?:[0-9]+/agents/(.*?)$")
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
    final var hint = context.request().getHeader("Slug");
    final var name = hint.endsWith("/") ? hint.substring(0, hint.length() - 1) : hint;
    final var entityIri = RdfModelUtils.createIri(requestUri + name);

    try {
      final var entityGraph = RdfModelUtils.stringToModel(
        entityRepresentation,
        entityIri,
        RDFFormat.TURTLE
      );

      // TODO: if slug is without trailing backslash and representation is with then doesnt work

      entityGraph.remove(null, RDF.TYPE, CORE.RESOURCE_PROFILE);
      entityGraph.remove(null, CORE.IS_PROFILE_OF, null);
      entityGraph.remove(null, RDF.TYPE, CORE.WORKSPACE);
      this.rdfStoreMessagebox.sendMessage(new RdfStoreMessage.GetEntityIri(requestUri, name)).compose(
        actualEntityName ->
          this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateWorkspace(actualEntityName.body()))
            .compose(response -> {
              var workspaceRepresentation = response.body();
              try {
                var baseModel = RdfModelUtils.stringToModel(workspaceRepresentation, entityIri, RDFFormat.TURTLE);
                entityGraph.addAll(RdfModelUtils.stringToModel(workspaceRepresentation, entityIri, RDFFormat.TURTLE));
                baseModel.getNamespaces().forEach(entityGraph::setNamespace);
                workspaceRepresentation = RdfModelUtils.modelToString(entityGraph, RDFFormat.TURTLE,this.httpConfig.getBaseUri());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return this.rdfStoreMessagebox
                .sendMessage(new RdfStoreMessage.CreateWorkspace(
                  requestUri,
                  actualEntityName.body(),
                  entityGraph.stream()
                    .filter(t ->
                      t.getSubject().equals(entityIri)
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
            }).onSuccess(r -> context.response().setStatusCode(HttpStatus.SC_CREATED).end(r.body()))
            .onFailure(t -> context.response().setStatusCode(HttpStatus.SC_CREATED).end())
            .onFailure(context::fail)
      );
    } catch (final Exception e) {
      LOGGER.error(e);
      context.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
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
