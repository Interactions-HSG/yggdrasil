package org.hyperagents.yggdrasil.http;

import java.util.*;
import java.util.stream.Collectors;

import io.vertx.core.http.HttpMethod;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.CartagoEntityHandler;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

/**
 * This class implements handlers for all HTTP requests. Requests related to CArtAgO operations (e.g.,
 * creating a workspace, executing an action) are redirected to the {@link
 * org.hyperagents.yggdrasil.cartago.CartagoEntityHandler}.
 */
public class HttpEntityHandler {
  public static final String REQUEST_METHOD = "org.hyperagents.yggdrasil.eventbus.headers.requestMethod";
  public static final String REQUEST_URI = "org.hyperagents.yggdrasil.eventbus.headers.requestUri";
  public static final String ENTITY_URI_HINT = "org.hyperagents.yggdrasil.eventbus.headers.slug";
  public static final String CONTENT_TYPE = "org.hyperagents.yggdrasil.eventbus.headers.contentType";

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());

  private final Vertx vertx;
  private final CartagoEntityHandler cartagoHandler;

  public HttpEntityHandler(Vertx vertx) {
    this.vertx = vertx;
    this.cartagoHandler = new CartagoEntityHandler(vertx);
  }

  public void handleRedirectWithoutSlash(RoutingContext routingContext) {
    String requestURI = routingContext.request().absoluteURI();

    routingContext.response().setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY)
      .headers().add(HttpHeaders.LOCATION, requestURI.substring(0, requestURI.length()-1));

    routingContext.response().end();
  }

  public void handleGetEntity(RoutingContext routingContext) {
    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(Vertx.currentContext().config());
    String entityIRI = httpConfig.getBaseUri() + routingContext.request().path();
    LOGGER.info("GET request: " + entityIRI);

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
        .addHeader(REQUEST_URI, entityIRI);

    Map<String,List<String>> headers = getHeaders(entityIRI);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options,
      handleStoreReply(routingContext, HttpStatus.SC_OK, headers));
  }

  public void handleCreateEnvironment(RoutingContext context) {
    String envName = context.request().getHeader("Slug");
    String agentId = context.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    String envURI = HypermediaArtifactRegistry.getInstance().getHttpEnvironmentsPrefix() + envName;

    ThingDescription td = new ThingDescription.Builder(envName)
        .addThingURI(envURI)
        .addSemanticType("http://w3id.org/eve#EnvironmentArtifact")
        .addAction(new ActionAffordance.Builder("makeWorkspace",
              new Form.Builder(envURI + "/workspaces/").build())
            .addSemanticType("http://w3id.org/eve#MakeWorkspace")
            .build())
        .build();

    createEntity(context, TDGraphWriter.write(td));
  }

  public void handleCreateWorkspace(RoutingContext context) {
    String representation = context.getBodyAsString();
    String envName = context.pathParam("envid");
    String workspaceName = context.request().getHeader("Slug");
    String agentId = context.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    Promise<String> cartagoPromise = Promise.promise();
    cartagoHandler.createWorkspace(agentId, envName, workspaceName, representation, cartagoPromise);

    cartagoPromise.future().compose(result ->  {
      HypermediaArtifactRegistry.getInstance().addWorkspace(envName, workspaceName);
      return Future.future(promise -> storeEntity(context, workspaceName, result, promise));
    });
  }

  public void handleCreateArtifact(RoutingContext context) {
    LOGGER.info("Received create artifact request");
    String representation = context.getBodyAsString();
    String workspaceName = context.pathParam("wkspid");
    String agentId = context.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    JsonObject artifactInit = (JsonObject) Json.decodeValue(representation);
    String artifactName = artifactInit.getString("artifactName");

    Promise<String> cartagoPromise = Promise.promise();
    cartagoHandler.createArtifact(agentId, workspaceName, artifactName, representation,
        cartagoPromise);

    cartagoPromise.future().compose(result ->
      Future.future(promise -> storeEntity(context, artifactName, result, promise)));
  }

  // TODO: add payload validation
  public void handleCreateEntity(RoutingContext routingContext) {
    String agentId = routingContext.request().getHeader("X-Agent-WebID");

    if (agentId == null) {
      routingContext.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    String entityRepresentation = routingContext.getBodyAsString();
    createEntity(routingContext, entityRepresentation);
  }

  public void handleAction(RoutingContext context) {
    String entityRepresentation = context.getBodyAsString();
    String wkspName = context.pathParam("wkspid");
    String artifactName = context.pathParam("artid");

    HttpServerRequest request = context.request();
    String agentId = request.getHeader("X-Agent-WebID");

    if (agentId == null) {
      context.response().setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
    }

    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();

    String artifactIri = artifactRegistry.getHttpArtifactsPrefix(wkspName) + artifactName;
    String actionName = artifactRegistry.getActionName(request.rawMethod(), request.absoluteURI());

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
        .addHeader(REQUEST_URI, artifactIri);

    String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);

    if (contentType != null) {
      options.addHeader(CONTENT_TYPE, contentType);
    }

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options, reply -> {
          if (reply.succeeded()) {
              String artifactDescription = (String) reply.result().body();
              ThingDescription td = TDGraphReader.readFromString(TDFormat.RDF_TURTLE,
                  artifactDescription);

              DeliveryOptions cartagoOptions = new DeliveryOptions()
                  .addHeader(CartagoVerticle.AGENT_ID, agentId)
                  .addHeader(REQUEST_METHOD, CartagoVerticle.DO_ACTION)
                  .addHeader(CartagoVerticle.WORKSPACE_NAME, wkspName)
                  .addHeader(CartagoVerticle.ARTIFACT_NAME, artifactName)
                  .addHeader(CartagoVerticle.ACTION_NAME, actionName);

              String apiKey = context.request().getHeader("X-API-Key");
              if (apiKey != null && !apiKey.isEmpty()) {
                artifactRegistry.setAPIKeyForArtifact(artifactIri, apiKey);
              }

              Optional<ActionAffordance> affordance = td.getActions().stream().filter(action ->
                  action.getTitle().isPresent() && action.getTitle().get().compareTo(actionName) == 0)
                  .findFirst();

              String serializedPayload = null;

              if (affordance.isPresent()) {
                Optional<DataSchema> inputSchema = affordance.get().getInputSchema();

                if (inputSchema.isPresent() && inputSchema.get().getDatatype().equals(DataSchema.ARRAY)) {
                  JsonElement payload = JsonParser.parseString(entityRepresentation);
                  List<Object> params = ((ArraySchema) inputSchema.get()).parseJson(payload);

                  serializedPayload = CartagoDataBundle.toJson(params);
                }
              }

              vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, serializedPayload, cartagoOptions,
                  cartagoReply -> {
                    if (cartagoReply.succeeded()) {
                      LOGGER.info("CArtAgO operation succeeded: " + artifactName + ", " + actionName);
                      context.response().setStatusCode(HttpStatus.SC_OK).end();
                    } else {
                      LOGGER.info("CArtAgO operation failed: " + artifactName + ", " + actionName
                          + "; reason: " + cartagoReply.cause().getMessage());
                      context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                          .end();
                    }
                  });
            }
        });
  }

  // TODO: add payload validation
  public void handleUpdateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.UPDATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, entityRepresentation, options,
        handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleDeleteEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.DELETE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options,
        handleStoreReply(routingContext, HttpStatus.SC_OK));
  }

  public void handleEntitySubscription(RoutingContext routingContext) {
    JsonObject subscribeRequest = routingContext.getBodyAsJson();

    String mode = subscribeRequest.getString("hub.mode");
    String entityIri = subscribeRequest.getString("hub.topic");
    String callbackIri = subscribeRequest.getString("hub.callback");

    if (mode.equalsIgnoreCase("subscribe")) {
      DeliveryOptions options = new DeliveryOptions()
          .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
          .addHeader(REQUEST_URI, entityIri);

      vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options, reply -> {
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
    }
    else if (mode.equalsIgnoreCase("unsubscribe")) {
      NotificationSubscriberRegistry.getInstance().removeCallbackIRI(entityIri, callbackIri);
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    }
    else {
      routingContext.response().setStatusCode(HttpStatus.SC_BAD_REQUEST).end();
    }
  }

  private Map<String, List<String>> getHeaders(String entityIRI) {

    Map<String,List<String>> headers = getWebSubHeaders(entityIRI);
    headers.putAll(getCORSHeaders());

    return headers;
  }

  private Map<String, List<String>> getWebSubHeaders(String entityIRI) {
    Map<String,List<String>> headers = new HashMap<>();

    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(Vertx.currentContext().config());
    Optional<String> webSubHubIRI = httpConfig.getWebSubHubUri();

    webSubHubIRI.ifPresent(hubIRI -> headers.put("Link", Arrays.asList("<" + hubIRI + ">; rel=\"hub\"",
      "<" + entityIRI + ">; rel=\"self\"")));

    return headers;
  }

  private Map<String, ? extends List<String>> getCORSHeaders() {
    Map<String, List<String>> corsHeaders = new HashMap<>();

    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, Arrays.asList("*"));
    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, Arrays.asList("true"));
    corsHeaders.put(com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name()));

    return corsHeaders;
  }

  private void storeEntity(RoutingContext context, String entityName, String representation,
                           Promise<Object> promise) {

    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(Vertx.currentContext().config());
    String entityIRI = httpConfig.getBaseUri() + context.request().path();

    DeliveryOptions options = new DeliveryOptions()
      .addHeader(REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(REQUEST_URI, entityIRI)
      .addHeader(ENTITY_URI_HINT, entityName);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, result -> {
      if (result.succeeded()) {
        context.response().setStatusCode(HttpStatus.SC_CREATED).end(representation);
        promise.complete();
      } else {
        context.response().setStatusCode(HttpStatus.SC_CREATED).end();
        promise.fail("Could not store the entity representation.");
      }
    });
  }

  private void createEntity(RoutingContext context, String representation) {
    String slug = context.request().getHeader("Slug");

    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(Vertx.currentContext().config());
    String entityIri = httpConfig.getBaseUri() + context.request().path();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.CREATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri)
        .addHeader(ENTITY_URI_HINT, slug);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, handleStoreReply(context,
        HttpStatus.SC_CREATED));
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext,
      int succeededStatusCode) {
    return handleStoreReply(routingContext, succeededStatusCode, new HashMap<>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext,
      int succeededStatusCode, Map<String,List<String>> headers) {

    return reply -> {
      if (reply.succeeded()) {
        LOGGER.info("Creating Response");

        HttpServerResponse httpResponse = routingContext.response();
        httpResponse.setStatusCode(succeededStatusCode);

        httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");

        // Note: forEach produces duplicate keys. If anyone wants to, please replace this with a Java 8 version ;-)
        for(String headerName : headers.keySet()) {
          httpResponse.putHeader(headerName, headers.get(headerName).stream().collect(Collectors.joining(",")));
        }

        String storeReply = reply.result().body();
        if (storeReply != null && !storeReply.isEmpty()) {
          httpResponse.end(storeReply);
        } else {
          httpResponse.end();
        }
      } else {
        ReplyException exception = ((ReplyException) reply.cause());

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
