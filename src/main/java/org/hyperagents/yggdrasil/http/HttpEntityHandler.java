package org.hyperagents.yggdrasil.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

public class HttpEntityHandler {
  public static final String REQUEST_METHOD = "org.hyperagents.yggdrasil.eventbus.headers.requestMethod";
  public static final String REQUEST_URI = "org.hyperagents.yggdrasil.eventbus.headers.requestUri";
  public static final String ENTITY_URI_HINT = "org.hyperagents.yggdrasil.eventbus.headers.slug";
  public static final String CONTENT_TYPE = "org.hyperagents.yggdrasil.eventbus.headers.contentType";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEntityHandler.class.getName());
  
  private Vertx vertx;
  private CartagoEntityHandler cartagoHandler;
  
  private String webSubHubIRI = null;
  private String rdfSubHubIRI = null;
  
  public HttpEntityHandler(Vertx vertx) {
    this.vertx = vertx;
    this.cartagoHandler = new CartagoEntityHandler(vertx);
    
    JsonObject httpConfig = Vertx.currentContext().config().getJsonObject("http-config");
    
    if (httpConfig != null) {
      if (httpConfig.getString("websub-hub") != null) {
        webSubHubIRI = httpConfig.getString("websub-hub");
      }
      if (httpConfig.getString("rdfsub-hub") != null) {
        rdfSubHubIRI = httpConfig.getString("rdfsub-hub");
      }
    }
  }
  
  public void handleGetEntity(RoutingContext routingContext) {
    handleGetEntity(routingContext, false);
  }
  
  public void handleGetEntity(RoutingContext routingContext, boolean isCartagoArtifact) {
    String entityIri = routingContext.request().absoluteURI();
    
    LOGGER.info("GET request: " + entityIri);
    
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.GET_ENTITY)
        .addHeader(REQUEST_URI, entityIri);
    
    Map<String,List<String>> headers = new HashMap<String,List<String>>();
    
    // Use RDFSub by default for all entities, we use WebSub only for CArtAgO artifacts
    if (isCartagoArtifact) {
      if (webSubHubIRI != null) {
        headers.put("Link", Arrays.asList("<" + webSubHubIRI + ">; rel=\"hub\"", 
            "<" + entityIri + ">; rel=\"self\""));
      }
    } else if (rdfSubHubIRI != null) {
      headers.put("Link", Arrays.asList("<" + rdfSubHubIRI + "subscription>; rel=\"rdfhub\"",
          "<http://hyperagents.org/>; rel=\"topic\"")); // TODO: quick-and-dirty for MASTech demo
    }
    
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options, handleStoreReply(routingContext, 
        HttpStatus.SC_OK, headers));
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
        .addAction(new ActionAffordance.Builder(new Form.Builder(envURI + "/workspaces/").build())
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
    
    cartagoPromise.future().compose(result ->  {
      return Future.future(promise -> storeEntity(context, artifactName, result, promise));
    });
  }
  
  private void storeEntity(RoutingContext context, String entityName, String representation, 
      Promise<Object> promise) {
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.CREATE_ENTITY)
        .addHeader(REQUEST_URI, context.request().absoluteURI())
        .addHeader(ENTITY_URI_HINT, entityName);
//        .addHeader(CONTENT_TYPE, context.request().getHeader("Content-Type"));
    
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
  
  // TODO: add payload validation
  public void handleCreateEntity(RoutingContext routingContext) {
    String entityRepresentation = routingContext.getBodyAsString();
//    String slug = routingContext.request().getHeader("Slug");
//    String agentUri = routingContext.request().getHeader("X-Agent-WebID");
    
//    Optional<String> artifactClass = Optional.empty();
//    HypermediaArtifactRegistry artifactRegistry = HypermediaArtifactRegistry.getInstance();
//    Set<IRI> types;
    
//    try {
//      types = new RdfPayload(RDFFormat.TURTLE, entityRepresentation, "").getSemanticTypes();
//    } catch (RDFParseException | RDFHandlerException | IOException e) {
//      e.printStackTrace();
//      routingContext.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
//      return;
//    }
//    
//    for (IRI type : types) {
//      artifactClass = artifactRegistry.getArtifactTemplate(type.stringValue());
//      if (artifactClass.isPresent()) {
//        break;
//      }
//    }
    
//    if (artifactClass.isPresent() && agentUri != null && !agentUri.isEmpty()) {
//      // The client wants to instantiate a known virtual artifact. 
//      // Send request to CArtAgO verticle to instantiate the artifact.
//      DeliveryOptions options = new DeliveryOptions()
//          .addHeader(CartagoVerticle.AGENT_ID, agentUri)
//          .addHeader(REQUEST_METHOD, CartagoVerticle.CREATE_ARTIFACT)
//          .addHeader(CartagoVerticle.ARTIFACT_CLASS, artifactClass.get())
//          .addHeader(ENTITY_URI_HINT, slug);
//      
//      vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, entityRepresentation, options,
//          response -> {
//              if (response.succeeded()) {
//                String artifactDescription = (String) response.result().body();
//                
//                LOGGER.info("CArtAgO artifact created: " + artifactDescription);
//                
//                // If the CArtAgO artifact was created successfully, generate the Thing Description 
//                // and store it
//                createEntity(routingContext, artifactDescription);
//              }
//            });
//    } else {
      createEntity(routingContext, entityRepresentation);
//    }
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
                  action.getTitle().get().compareTo(actionName) == 0).findFirst();
              
              String serializedPayload = null;
              
              if (affordance.isPresent()) {
                Optional<DataSchema> inputSchema = affordance.get().getInputSchema();
                
                if (inputSchema.isPresent() && inputSchema.get().getDatatype() == DataSchema.ARRAY) {
                  JsonElement payload = JsonParser.parseString(entityRepresentation);
                  List<Object> params = ((ArraySchema) inputSchema.get()).parseJson(payload);
                  
                  serializedPayload = CartagoDataBundle.toJson(params);
                }
              }
              
//              context.response().setStatusCode(HttpStatus.SC_ACCEPTED).end();
//              
//              vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, serializedPayload, cartagoOptions, 
//                  cartagoReply -> {
//                    if (cartagoReply.succeeded()) {
////                      context.response().setStatusCode(HttpStatus.SC_OK).end();
//                      LOGGER.info("CArtAgO operation succeeded: " + artifactName + ", " + actionName);
//                    } else {
//                      LOGGER.info("CArtAgO operation failed: " + artifactName + ", " + actionName);
//                      context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
//                          .end();
//                    }
//                  });
              
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
  
  public void handlePatchEntity(RoutingContext routingContext) {
    // TODO
  }

  // TODO: add payload validation
  public void handleUpdateEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();
    String entityRepresentation = routingContext.getBodyAsString();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.UPDATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);
    
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, entityRepresentation, options, 
        handleStoreReplyNext(routingContext));
  }

  public void handleDeleteEntity(RoutingContext routingContext) {
    String entityIri = routingContext.request().absoluteURI();

    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.DELETE_ENTITY)
        .addHeader(REQUEST_URI, entityIri);
    
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options, handleStoreReplyNext(routingContext));
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
  
  private void createEntity(RoutingContext context, String representation) {
    String entityIri = context.request().absoluteURI();
    String slug = context.request().getHeader("Slug");
//    String contentType = context.request().getHeader("Content-Type");
    
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(REQUEST_METHOD, RdfStore.CREATE_ENTITY)
        .addHeader(REQUEST_URI, entityIri)
        .addHeader(ENTITY_URI_HINT, slug);
//        .addHeader(CONTENT_TYPE, contentType);
    
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, handleStoreReply(context, 
        HttpStatus.SC_CREATED));
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext (RoutingContext routingContext) {
    return handleStoreReplyNext(routingContext, HttpStatus.SC_OK);
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext(RoutingContext routingContext, 
      int succeededStatusCode) {
    return handleStoreReplyNext(routingContext, succeededStatusCode, 
        new HashMap<String,List<String>>());
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReplyNext(RoutingContext routingContext, 
      int succeededStatusCode, Map<String,List<String>> headers) {
    
    if (succeededStatusCode == HttpStatus.SC_OK) {
      return handleStoreReply(routingContext, succeededStatusCode, headers);
    } else {
      routingContext.next();
      return null;
    }
  }

  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext, 
      int succeededStatusCode) {
    return handleStoreReply(routingContext, succeededStatusCode, new HashMap<String,List<String>>());
  }
  
  private Handler<AsyncResult<Message<String>>> handleStoreReply(RoutingContext routingContext,
      int succeededStatusCode, Map<String,List<String>> headers) {

    return reply -> {
      if (reply.succeeded()) {
        HttpServerResponse httpResponse = routingContext.response();
        
        httpResponse
          .setStatusCode(succeededStatusCode)
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/turtle");

        headers.forEach((k, v) -> {
          httpResponse.putHeader(k, v);
        });
        
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
